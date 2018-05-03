package pl.allegro.tech.embeddedelasticsearch;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static pl.allegro.tech.embeddedelasticsearch.ElasticDownloadUrlUtils.constructLocalFileName;

class ElasticSearchInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInstaller.class);
    private static final String ELS_PACKAGE_STATUS_FILE_SUFFIX = "-downloaded";
    private static final String ELS_PACKAGE_PREFIX = "elasticsearch-";
    private static final List<String> ELS_EXECUTABLE_FILES = Arrays.asList("elasticsearch", "elasticsearch.in.sh");

    private final InstanceSettings instanceSettings;
    private final InstallationDescription installationDescription;

    ElasticSearchInstaller(InstanceSettings instanceSettings, InstallationDescription installationDescription) {
        this.instanceSettings = instanceSettings;
        this.installationDescription = installationDescription;
    }

    File getExecutableFile() {
        return fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch"));
    }

    File getInstallationDirectory() {
        return getFile(installationDescription.getInstallationDirectory(), ELS_PACKAGE_PREFIX + installationDescription.getVersion());
    }

    File getDownloadDirectory() {
        return getFile(installationDescription.getDownloadDirectory());
    }

    void install() throws IOException, InterruptedException {
        prepareDirectories();
        installElastic();
        configureElastic();
        installPlugins();
        applyElasticPermissionRights();
    }

    private void prepareDirectories() throws IOException {
        forceMkdir(getInstallationDirectory());
        forceMkdir(getDownloadDirectory());
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
        File pluginsDir = new File(getInstallationDirectory(), "plugins");
        String[] pluginList = pluginsDir.list();

        for (InstallationDescription.Plugin plugin : installationDescription.getPlugins()) {
            logger.info("> " + pluginManager + " install " + plugin.getExpression());
            if(isPluginInstalled(plugin, pluginList)) {
               logger.info("> Plugin " + plugin.getPluginName() + " already installed, skipping");
               continue;
            }
            ProcessBuilder builder = new ProcessBuilder();
            builder.redirectOutput(Redirect.INHERIT);
            builder.redirectError(Redirect.INHERIT);
            builder.command(prepareInstallCommand(pluginManager, plugin));
            Process process = builder.start();
            if (process.waitFor() != 0) {
                throw new EmbeddedElasticsearchStartupException("Unable to install plugin: " + plugin);
            }
        }
    }

    private static boolean isPluginInstalled(InstallationDescription.Plugin plugin, String[] pluginList) {
        if(pluginList == null) return false;
        for(String filename : pluginList){
            if(filename.equals(plugin.getPluginName())) return true;
        }
        return false;
    }

    private String[] prepareInstallCommand(File pluginManager, InstallationDescription.Plugin plugin) {
        if (installationDescription.versionIs1x() && plugin.expressionIsUrl()) {
            return new String[]{pluginManager.getAbsolutePath(), "--install", plugin.getPluginName(), "--url", plugin.getExpression()};
        }
        return new String[]{pluginManager.getAbsolutePath(), "install", plugin.getExpression()};
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
        File target = new File(getDownloadDirectory(), constructLocalFileName(source));
        File statusFile = new File(target.getParentFile(), target.getName() + ELS_PACKAGE_STATUS_FILE_SUFFIX);

        removeBrokenDownload(target, statusFile);

        if (!target.exists()) {
            download(source, target, statusFile);
        } else if (!statusFile.exists() && maybeDownloading(target)) {
            waitForDownload(target, statusFile);
        } else if (!statusFile.exists()) {
            throw new IOException("Broken download. File '"  + target + "' exits but status '" + statusFile + "' file wash not created");
        } else {
            logger.info("Download skipped");
        }
        return target.toPath();
    }

    private void removeBrokenDownload(final File target, final File statusFile) throws IOException {
        if (target.exists() && !statusFile.exists() && !maybeDownloading(target)) {
            logger.info("Removing broken download file {}", target);
            FileUtils.forceDelete(target);
        }
    }

    private boolean maybeDownloading(final File target) {
        // Check based on assumption that if other thread or jvm is currently downloading file on disk should be modified
        // at least every 10 seconds as new data is being downloaded. This will not work on file system
        // without support for lastmodified field or on very slow internet connection
        return System.currentTimeMillis() - target.lastModified() < TimeUnit.SECONDS.toMillis(10L);
    }

    private void download(final URL source, final File target, final File statusFile) throws IOException {
        logger.info("Downloading {} to {} ...", source, target);
        FileUtils.copyURLToFile(source, target);
        FileUtils.touch(statusFile);
        logger.info("Download complete");
    }

    private void waitForDownload(final File target, final File statusFile) throws IOException {
        boolean downloaded;
        do {
            logger.info("File {} (size={}) is probably being downloaded by another thread/jvm. Waiting ...", target, target.length());
            downloaded = FileUtils.waitFor(statusFile, 30);
        } while (!downloaded && maybeDownloading(target));
        if (!downloaded) {
            throw new IOException("Broken download. Another party probably failed to download " + target);
        }
        logger.info("File was downloaded by another thread/jvm. Download skipped");
    }

    private void install(String what, String relativePath, Path downloadedFile) throws IOException {
        Path destination = new File(getInstallationDirectory().getParentFile(), relativePath).toPath();
        FileUtils.forceDelete(getInstallationDirectory());
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
        File binDirectory = getFile(getInstallationDirectory(), "bin");
        for (String fn : ELS_EXECUTABLE_FILES) {
            setExecutable(new File(binDirectory, fn));
        }
    }

    private void setExecutable(File executableFile) throws IOException {
        logger.info("Applying executable permissions on " + executableFile);
        executableFile.setExecutable(true);
    }
}
