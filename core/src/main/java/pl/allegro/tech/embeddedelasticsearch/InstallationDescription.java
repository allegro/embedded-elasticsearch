package pl.allegro.tech.embeddedelasticsearch;

import static org.apache.commons.io.FileUtils.getFile;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.io.FilenameUtils;

import static pl.allegro.tech.embeddedelasticsearch.Require.require;

class InstallationDescription {

    private static final Supplier<File> DEFAULT_INSTALL_DIR = () -> new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-temp-dir");
    private static final Supplier<File> DEFAULT_DOWNLOAD_DIR = DEFAULT_INSTALL_DIR;

    private final String version;
    private final URL downloadUrl;
    private final List<Plugin> plugins;
    private final boolean cleanInstallationDirectoryOnStop;
    private final File installationDirectory;
    private final File downloadDirectory;

    InstallationDescription(
            Optional<String> versionMaybe,
            Optional<URL> downloadUrlMaybe,
            Optional<File> downloadDirectory,
            Optional<File> installationDirectory,
            boolean cleanInstallationDirectoryOnStop,
            List<Plugin> plugins) {
        require(versionMaybe.isPresent() ^ downloadUrlMaybe.isPresent(), "You must specify elasticsearch version, or download url");
        if (versionMaybe.isPresent()) {
            this.version = versionMaybe.get();
            this.downloadUrl = ElasticDownloadUrlUtils.urlFromVersion(versionMaybe.get());
        } else {
            this.version = ElasticDownloadUrlUtils.versionFromUrl(downloadUrlMaybe.get());
            this.downloadUrl = downloadUrlMaybe.get();
        }
        this.plugins = plugins;
        this.cleanInstallationDirectoryOnStop = cleanInstallationDirectoryOnStop;
        this.installationDirectory = getFile(installationDirectory.orElseGet(DEFAULT_INSTALL_DIR));
        this.downloadDirectory = getFile(downloadDirectory.orElseGet(DEFAULT_DOWNLOAD_DIR));
    }

    String getVersion() {
        return version;
    }

    URL getDownloadUrl() {
        return downloadUrl;
    }

    List<Plugin> getPlugins() {
        return plugins;
    }

    boolean versionIs1x() {
        return version.startsWith("1.");
    }

    boolean isCleanInstallationDirectoryOnStop() {
        return cleanInstallationDirectoryOnStop;
    }

    File getInstallationDirectory() {
        return installationDirectory;
    }

    public File getDownloadDirectory() {
        return downloadDirectory;
    }

    static class Plugin {
        private String expression;

        Plugin(String expression) {
            this.expression = expression;
        }

        String getExpression() {
            return expression;
        }

        @Override
        public String toString() {
            return expression;
        }

        String getPluginName() {
            if (expressionIsUrl()) {
                return FilenameUtils.getBaseName(expression).replaceAll("-[\\d].*", "");
            }
            return expression;
        }

        boolean expressionIsUrl() {
            return expression.startsWith("http");
        }
    }
}
