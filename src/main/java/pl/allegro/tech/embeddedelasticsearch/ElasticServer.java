package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Throwables.propagate;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.lang3.Validate.isTrue;

class ElasticServer {

    private static final Logger logger = LoggerFactory.getLogger(ElasticServer.class);

    private boolean started;
    private final Object startedLock = new Object();

    private static final int ELS_START_TIMEOUT_IN_MS = 15_000;

    private Process elastic;
    private int boundPort = -1;
    private Thread ownerThread;
    private int pid = -1;

    private final InstanceDescription instanceDescription;
    private final InstallationDirectory installationDirectory;

    ElasticServer(InstanceDescription instanceDescription, InstallationDirectory installationDirectory) {
        this.instanceDescription = instanceDescription;
        this.installationDirectory = installationDirectory;
    }

    void start() throws IOException, InterruptedException {
        deleteDataDirectory();
        startElasticProcess();
        installExitHook();
        waitForElasticToStart();
        verify();
    }

    void stop() {
        try {
            stopElasticServer();
            finalizeClose();
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    boolean isStarted() {
        return started;
    }

    private void deleteDataDirectory() {
        try {
            deleteDirectory(installationDirectory.getDataDirectory());
        } catch (IOException e) {
            throw new EmbeddedElasticsearchStartupException("Could not delete data directory of embedded elasticsearch server. Possibly an instance is running.", e);
        }
    }

    private void startElasticProcess() {
        ownerThread = new Thread(() -> {
            try {
                synchronized (this) {
                    ProcessBuilder builder = new ProcessBuilder();
                    builder.redirectErrorStream(true);
                    builder.command(elasticStartCommand());
                    elastic = builder.start();
                }
                BufferedReader outputStream = new BufferedReader(new InputStreamReader(elastic.getInputStream(), UTF_8));
                String line;
                while ((line = outputStream.readLine()) != null) {
                    logger.info(line);
                    parseElasticLogLine(line);
                }
            } catch (Exception e) {
                throw new EmbeddedElasticsearchStartupException(e);
            }
        }, "EmbeddedElsHandler");
        ownerThread.start();
    }

    private void installExitHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "ElsInstanceCleaner"));
    }
    
    private List<String> elasticStartCommand() {
        return asList(elasticExecutable(), "--transport.tcp.port=" + instanceDescription.getPort(), "--cluster.name=" + instanceDescription.getClusterName());
    }

    private String elasticExecutable() {
        return installationDirectory.getExecutableLocation().getAbsolutePath();
    }

    private void waitForElasticToStart() throws InterruptedException, IOException {
        logger.info("Waiting for ElasticSearch to start...");
        long waitUtil = System.currentTimeMillis() + ELS_START_TIMEOUT_IN_MS;

        synchronized (startedLock) {
            while (!started && System.currentTimeMillis() < waitUtil) {
                startedLock.wait(waitUtil - System.currentTimeMillis());
            }
            if (!started) {
                throw new EmbeddedElasticsearchStartupException("Failed to start elasticsearch within time-out");
            }
        }

        logger.info("ElasticSearch started...");
    }

    private void parseElasticLogLine(String line) {
        if (started) {
            return;
        }
        if (line.contains("] started")) {
            signalElasticStarted();
        } else if (line.contains("[transport") && line.contains("bound_addresses")) {
            tryExtractTransportPort(line);
        } else if (line.contains(", pid[")) {
            tryExtractPid(line);
        }
    }

    private void signalElasticStarted() {
        synchronized (startedLock) {
            started = true;
            startedLock.notifyAll();
        }
    }

    private void tryExtractTransportPort(String line) {
        Matcher matcher = Pattern.compile("publish_address \\{.*?:(\\d+)").matcher(line);
        isTrue(matcher.find());
        boundPort = Integer.parseInt(matcher.group(1));
        logger.info("Detected Elasticsearch server port : " + boundPort);
    }

    private void tryExtractPid(String line) {
        Matcher matcher = Pattern.compile("pid\\[(\\d+)\\]").matcher(line);
        isTrue(matcher.find());
        pid = Integer.parseInt(matcher.group(1));
        logger.info("Detected Elasticsearch PID : " + pid);
    }

    private void verify() throws IOException {
        if (instanceDescription.getPort() != boundPort) {
            throw new EmbeddedElasticsearchStartupException(format(
                    "Embedded elasticsearch started on a different port than the search service expects it; Actual port : {0}; expected : {1}. Is another instance running?",
                    boundPort, instanceDescription.getPort()));
        }
    }

    private void stopElasticServer() throws IOException, InterruptedException {
        logger.info("Stopping elasticsearch server...");
        if (pid > -1) {
            stopElasticGracefully();
        }
        pid = -1;
        if (elastic != null) {
            int rc = elastic.waitFor();
            logger.info("Elasticsearch exited with RC " + rc);
        }
        elastic = null;
        if (ownerThread != null) {
            ownerThread.join();
        }
        ownerThread = null;
    }

    private void stopElasticGracefully() throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            stopWindows();
        } else {
            stopSystemV();
        }
    }

    private void stopWindows() throws IOException {
        Runtime.getRuntime().exec("taskkill /f /pid " + pid);
    }

    private void stopSystemV() throws IOException {
        Runtime.getRuntime().exec("kill -SIGINT " + pid);
    }

    private void finalizeClose() {
        logger.info("Purging data...");
        deleteDataDirectory();
        logger.info("Finishing...");
        started = false;
    }
}
