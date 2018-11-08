package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

class InstallFromVersion implements InstallationSource {

    private final URL downloadUrl;
    private final String version;
    private final boolean ossFlavor;

    public InstallFromVersion(String version) {
        this.version = version.replaceFirst("oss-", "");
        this.ossFlavor = version.startsWith("oss-");
        this.downloadUrl = urlFromVersion();
    }

    @Override
    public String determineVersion() {
        return version;
    }

    @Override
    public URL resolveDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public boolean isOssFlavor() {
        return ossFlavor;
    }

    private URL urlFromVersion() {
        ElsDownloadUrl elsDownloadUrl = ElsDownloadUrl.getByVersion(version);
        try {
            String versionToUse = ossFlavor ? "oss-" + version : version;
            return new URL(StringUtils.replace(elsDownloadUrl.downloadUrl, "{VERSION}", versionToUse));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private enum ElsDownloadUrl {
        ELS_1x("1.", "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-{VERSION}.zip"),
        ELS_2x("2.", "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/{VERSION}/elasticsearch-{VERSION}.zip"),
        ELS_5x("5.", "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-{VERSION}.zip"),
        ELS_6x("6.", ELS_5x.downloadUrl);

        String versionPrefix;
        String downloadUrl;

        ElsDownloadUrl(String versionPrefix, String downloadUrl) {
            this.versionPrefix = versionPrefix;
            this.downloadUrl = downloadUrl;
        }

        boolean versionMatch(String elasticVersion) {
            return elasticVersion.startsWith(versionPrefix);
        }

        static ElsDownloadUrl getByVersion(String elasticVersion) {
            return Arrays.stream(ElsDownloadUrl.values())
                    .filter(u -> u.versionMatch(elasticVersion))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid version: " + elasticVersion));
        }
    }
}
