package pl.allegro.tech.embeddedelasticsearch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

class ElasticDownloadUrlUtils {

    private static final String ELASTIC_2x_DOWNLOAD_URL = "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/{VERSION}/elasticsearch-{VERSION}.zip";
    private static final String ELASTIC_5x_DOWNLOAD_URL = "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-{VERSION}.zip";

    static URL urlFromVersion(String elasticVersion) {
        try {
            if (is2xVersion(elasticVersion)) {
                return new URL(StringUtils.replace(ELASTIC_2x_DOWNLOAD_URL, "{VERSION}", elasticVersion));
            }
            if (is5xVersion(elasticVersion)) {
                return new URL(StringUtils.replace(ELASTIC_5x_DOWNLOAD_URL, "{VERSION}", elasticVersion));
            }
            throw new IllegalArgumentException("Invalid version: " + elasticVersion);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean is2xVersion(String elasticVersion) {
        return elasticVersion.startsWith("2.");
    }

    private static boolean is5xVersion(String elasticVersion) {
        return elasticVersion.startsWith("5.");
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
