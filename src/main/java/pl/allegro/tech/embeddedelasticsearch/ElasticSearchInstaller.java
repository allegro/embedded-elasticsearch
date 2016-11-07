package pl.allegro.tech.embeddedelasticsearch;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static pl.allegro.tech.embeddedelasticsearch.ElasticDownloadUrlUtils.constructLocalFileName;

class ElasticSearchInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInstaller.class);
    private static final String ELS_PACKAGE_PREFIX = "elasticsearch-";
    private static final Set<String> ELS_EXECUTABLE_FILES = ImmutableSet.of("elasticsearch", "elasticsearch.in.sh");

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
        installPlugins();
        applyElasticPermissionRights();
    }

    private void prepareDirectory() throws IOException {
        forceMkdir(baseDirectory);
    }

    private void installElastic() throws IOException {
        Path downloadedTo = download(installationDescription.getDownloadUrl());
        install("Elasticsearch", "", downloadedTo, name -> name);
    }

    private void installPlugins() throws IOException {
        String prefix = ELS_PACKAGE_PREFIX + installationDescription.getVersion() + File.separator + "plugins" + File.separator;

        for (InstallationDescription.Plugin plugin : installationDescription.getPlugins()) {
            Path downloadedTo = download(plugin.getUrl());
            install(plugin.getName(), prefix + plugin.getName(), downloadedTo, FilenameUtils::getName);
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

    private void install(String what, String relativePath, Path downloadedFile, NameMapper nameMapper) throws IOException {
        Path destination = new File(baseDirectory, relativePath).toPath();
        logger.info("Installing " + what + " into " + destination + "...");
        try {
            ZipUtil.unpack(downloadedFile.toFile(), destination.toFile(), nameMapper);
            logger.info("Done");
        } catch (RuntimeException e) {
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
