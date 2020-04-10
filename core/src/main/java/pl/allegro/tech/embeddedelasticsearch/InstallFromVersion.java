package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

class InstallFromVersion implements InstallationSource {

    private final URL downloadUrl;
    private final String version;

    public InstallFromVersion(String version) {
        this.version = version;
        this.downloadUrl = urlFromVersion(version);
    }

    @Override
    public String determineVersion() {
        return version;
    }

    @Override
    public URL resolveDownloadUrl() {
        return downloadUrl;
    }

    private URL urlFromVersion(String version) {
        ElsDownloadUrl elsDownloadUrl = ElsDownloadUrl.getByVersion(version);
        try {
            return new URL(StringUtils.replace(elsDownloadUrl.getDownloadUrl(), "{VERSION}", version));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private enum ElsDownloadUrl {
        ELS_1x("1.", "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-{VERSION}.zip"),
        ELS_2x("2.", "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/{VERSION}/elasticsearch-{VERSION}.zip"),
        ELS_5x("5.", "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-{VERSION}.zip"),
        ELS_6x("6.", ELS_5x.downloadUrl),
        ELS_7x("7.", "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-{VERSION}-{PLATFORM}-x86_64.{EXTENSION}") {
            @Override
            String getDownloadUrl() {
                if (SystemUtils.IS_OS_LINUX) {
                    String result = StringUtils.replace(downloadUrl, PLATFORM, "linux");
                    return StringUtils.replace(result, EXTENSION, "tar.gz");
                } else if (SystemUtils.IS_OS_MAC) {
                    String result = StringUtils.replace(downloadUrl, PLATFORM, "darwin");
                    return StringUtils.replace(result, EXTENSION, "tar.gz");
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    String result = StringUtils.replace(downloadUrl, PLATFORM, "windows");
                    return StringUtils.replace(result, EXTENSION, "zip");
                }
                throw new IllegalArgumentException(("Unsupported OS version " + SystemUtils.OS_NAME));
            }
        };

        private static final String PLATFORM = "{PLATFORM}";
        private static final String EXTENSION = "{EXTENSION}";

        String versionPrefix;
        String downloadUrl;

        ElsDownloadUrl(String versionPrefix, String downloadUrl) {
            this.versionPrefix = versionPrefix;
            this.downloadUrl = downloadUrl;
        }

        boolean versionMatch(String elasticVersion) {
            return elasticVersion.startsWith(versionPrefix);
        }

        String getDownloadUrl() {
            return downloadUrl;
        }

        static ElsDownloadUrl getByVersion(String elasticVersion) {
            return Arrays.stream(ElsDownloadUrl.values())
                    .filter(u -> u.versionMatch(elasticVersion))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid version: " + elasticVersion));
        }
    }
}
