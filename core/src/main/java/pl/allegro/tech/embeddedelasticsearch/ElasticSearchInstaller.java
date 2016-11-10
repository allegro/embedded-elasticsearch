package pl.allegro.tech.embeddedelasticsearch;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static pl.allegro.tech.embeddedelasticsearch.ElasticDownloadUrlUtils.constructLocalFileName;

class ElasticSearchInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInstaller.class);
    private static final String ELS_PACKAGE_PREFIX = "elasticsearch-";
    private static final List<String> ELS_EXECUTABLE_FILES = Arrays.asList("elasticsearch", "elasticsearch.in.sh");

    private final File baseDirectory = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-temp-dir");
    private InstanceSettings instanceSettings;
    private final InstallationDescription installationDescription;

    ElasticSearchInstaller(InstanceSettings instanceSettings, InstallationDescription installationDescription) {
        this.instanceSettings = instanceSettings;
        this.installationDescription = installationDescription;
    }

    File getExecutableFile() {
        return fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch"));
    }

    File getInstallationDirectory() {
        return getFile(baseDirectory, ELS_PACKAGE_PREFIX + installationDescription.getVersion());
    }

    void install() throws IOException, InterruptedException {
        prepareDirectory();
        installElastic();
        configureElastic();
        installPlugins();
        applyElasticPermissionRights();
    }

    private void prepareDirectory() throws IOException {
        forceMkdir(baseDirectory);
    }

    private void installElastic() throws IOException {
        Path downloadedTo = download(installationDescription.getDownloadUrl());
        install("Elasticsearch", "", downloadedTo);
    }

    private void configureElastic() throws IOException {
        File elasticsearchYml = getFile(getInstallationDirectory(), "config", "elasticsearch.yml");
        FileUtils.writeStringToFile(elasticsearchYml, instanceSettings.toYaml(), UTF_8);
    }

    private void installPlugins() throws IOException, InterruptedException {
        File pluginManager = pluginManagerExecutable();
        setExecutable(pluginManager);

        for (InstallationDescription.Plugin plugin : installationDescription.getPlugins()) {
            logger.info("> " + pluginManager + " install " + plugin.getExpression());
            ProcessBuilder builder = new ProcessBuilder();
            builder.redirectOutput(Redirect.INHERIT);
            builder.redirectError(Redirect.INHERIT);
            builder.command(pluginManager.getAbsolutePath(), "install", plugin.getExpression());
            Process process = builder.start();
            if (process.waitFor() != 0) {
                throw new EmbeddedElasticsearchStartupException("Unable to install plugin: " + plugin);
            }
        }
    }

    private File pluginManagerExecutable() {
        File elasticsearchPlugin = fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch-plugin"));
        if (elasticsearchPlugin.exists()) {
            return elasticsearchPlugin;
        } else {
            return fileRelativeToInstallationDir("bin", systemDependentExtension("plugin"));
        }
    }

    private String systemDependentExtension(String baseFileName) {
        return baseFileName + (IS_OS_WINDOWS ? ".bat" : "");
    }

    private File fileRelativeToInstallationDir(String... path) {
        return getFile(getInstallationDirectory(), path);
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
            ZipFile zipFile = new ZipFile(downloadedFile.toString());
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
