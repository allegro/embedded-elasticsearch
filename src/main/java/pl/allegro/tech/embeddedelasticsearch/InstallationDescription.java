package pl.allegro.tech.embeddedelasticsearch;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static pl.allegro.tech.embeddedelasticsearch.Require.require;

class InstallationDescription {

    private final String version;
    private final URL downloadUrl;
    private final List<Plugin> plugins;

    InstallationDescription(
            Optional<String> versionMaybe,
            Optional<URL> downloadUrlMaybe,
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

    static class Plugin {
        private final String plugin;

        public Plugin(String plugin) {
            this.plugin = plugin;
        }

        public String getPlugin() {
            return plugin;
        }
    }
}
