package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Throwables.propagate;

class ElasticDownloadUrlUtils {

    static URL urlFromVersion(String elasticVersion) {
        assertVersionValid(elasticVersion);
        try {
            return new URL("https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-" + elasticVersion + ".zip");
        } catch (MalformedURLException e) {
            throw propagate(e);
        }
    }

    private static void assertVersionValid(String elasticVersion) {
        if (!Pattern.matches("[a-zA-Z0-9\\-_\\.]+", elasticVersion)) {
            throw new IllegalArgumentException("Invalid version: " + elasticVersion);
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
