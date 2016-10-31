package pl.allegro.tech.embeddedelasticsearch;

import com.google.common.collect.ImmutableSet;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static pl.allegro.tech.embeddedelasticsearch.ElasticDownloadUrlUtils.constructLocalFileName;

class ElasticSearchInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInstaller.class);
    private static final String ELS_PACKAGE_PREFIX = "elasticsearch-";
    private static final Set<String> ELS_EXECUTABLE_FILES = ImmutableSet.of("elasticsearch", "elasticsearch.in.sh", "elasticsearch-plugin");
    private static final int INSTALL_PLUGIN_TIMEOUT_SEC = 30;

    private final File baseDirectory = new File(System.getProperty("java.io.tmpdir"), "elasticsearch-test-utils");
    private final InstallationDescription installationDescription;

    ElasticSearchInstaller(InstallationDescription installationDescription) {
        this.installationDescription = installationDescription;
    }

    File getExecutableFile() {
        return getFile(elasticInstallationDirectory(), "bin", IS_OS_WINDOWS ? "elasticsearch.bat" : "elasticsearch");
    }

    File getDataDirectory() {
        return getFile(elasticInstallationDirectory(), "data");
    }

    private File elasticInstallationDirectory() {
        return getFile(baseDirectory, ELS_PACKAGE_PREFIX + installationDescription.getVersion());
    }

    void install() throws IOException {
        prepareDirectory();
        installElastic();
        applyElasticPermissionRights();
        installPlugins();
    }

    private void prepareDirectory() throws IOException {
        forceMkdir(baseDirectory);
    }

    private void installElastic() throws IOException {
        Path downloadedTo = download(installationDescription.getDownloadUrl());
        install("Elasticsearch", "", downloadedTo);
    }

    private void installPlugins() throws IOException {
        for (InstallationDescription.Plugin plugin : installationDescription.getPlugins()) {
            String installPluginCmd = elasticInstallationDirectory().getAbsolutePath() + "/bin/elasticsearch-plugin";
            logger.info("Installing plugin: " + plugin.getPlugin());
            Process installPluginProcess = Runtime.getRuntime().exec(installPluginCmd + " install " + plugin.getPlugin());
            boolean success = false;
            try {
                success = installPluginProcess.waitFor(INSTALL_PLUGIN_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            if (!success) {
                logger.error("Error installing plugin: " + plugin.getPlugin());
                return;
            }
            logger.info("Plugin: " + plugin.getPlugin() + " installed");
        }
    }

    private Path download(URL source) throws IOException {
        File target = new File(baseDirectory, constructLocalFileName(source));
        if (!target.exists()) {
            logger.info("Downloading : " + source + " to " + target + "...");
            FileUtils.copyURLToFile(source, target);
            logger.info("Download complete");
        } else {
            logger.info("Download skipped");
        }
        return target.toPath();
    }

    private void install(String what, String relativePath, Path downloadedFile) throws IOException {
        Path destination = new File(baseDirectory, relativePath).toPath();
        logger.info("Installing " + what + " into " + destination + "...");
        try {
            ZipFile zipFile = new ZipFile(downloadedFile.toFile());
            zipFile.extractAll(destination.toString());
            logger.info("Done");
        } catch (ZipException e) {
            logger.info("Failure : " + e);
            throw new EmbeddedElasticsearchStartupException(e);
        }
    }

    private void applyElasticPermissionRights() throws IOException {
        if (IS_OS_WINDOWS) {
            return;
        }
        File binDirectory = getFile(baseDirectory, ELS_PACKAGE_PREFIX + installationDescription.getVersion(), "bin");
        for (String fn : ELS_EXECUTABLE_FILES) {
            setExecutable(new File(binDirectory, fn));
        }
    }

    private void setExecutable(File executableFile) throws IOException {
        logger.info("Applying executable permissions on " + executableFile);
        executableFile.setExecutable(true);
    }
}
