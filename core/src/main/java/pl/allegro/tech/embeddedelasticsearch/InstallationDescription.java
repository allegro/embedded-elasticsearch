package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static pl.allegro.tech.embeddedelasticsearch.Require.require;

class InstallationDescription {

    private final String version;
    private final URL downloadUrl;
    private final List<Plugin> plugins;
    private final boolean cleanInstallationDirectoryOnStop;
    private final Optional<File> installationDirectory;

    InstallationDescription(
            Optional<String> versionMaybe,
            Optional<URL> downloadUrlMaybe,
            Optional<File> installationDirectory,
            boolean cleanInstallationDirectoryOnStop,
            List<Plugin> plugins) {
        require(versionMaybe.isPresent() || downloadUrlMaybe.isPresent(), "You must specify elasticsearch version, or download url");
        if (versionMaybe.isPresent()) {
            this.version = versionMaybe.get();
            this.downloadUrl = ElasticDownloadUrlUtils.urlFromVersion(versionMaybe.get());
        } else {
            this.version = ElasticDownloadUrlUtils.versionFromUrl(downloadUrlMaybe.get());
            this.downloadUrl = downloadUrlMaybe.get();
        }
        this.plugins = plugins;
        this.cleanInstallationDirectoryOnStop = cleanInstallationDirectoryOnStop;
        this.installationDirectory = installationDirectory;
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

    Optional<File> getInstallationDirectory() {
        return installationDirectory;
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
