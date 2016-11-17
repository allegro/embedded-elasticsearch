package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ElasticDownloadUrlUtils {

    enum ElsDownloadUrl {
        ELS_1x("1.", "https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-{VERSION}.zip"),
        ELS_2x("2.", "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/{VERSION}/elasticsearch-{VERSION}.zip"),
        ELS_5x("5.", "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-{VERSION}.zip");

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

    static URL urlFromVersion(String elasticVersion) {
        ElsDownloadUrl elsDownloadUrl = ElsDownloadUrl.getByVersion(elasticVersion);
        try {
            return new URL(StringUtils.replace(elsDownloadUrl.downloadUrl, "{VERSION}", elasticVersion));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    static String versionFromUrl(URL url) {
        Pattern versionPattern = Pattern.compile("-([^\\/]*).zip");
        Matcher matcher = versionPattern.matcher(url.toString());
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Cannot find version in this url. Note that I was looking for zip archive with name in format: \"anyArchiveName-versionInAnyFormat.zip\". Examples of valid urls:\n" +
                "- http://example.com/elasticsearch-2.3.0.zip\n" +
                "- http://example.com/myDistributionOfElasticWithChangedName-1.0.0.zip");
    }

    static String constructLocalFileName(URL url) {
        String path = url.getPath();
        if (path.isEmpty()) {
            return RandomStringUtils.randomAlphanumeric(10);
        }
        return FilenameUtils.getName(path);
    }
}
