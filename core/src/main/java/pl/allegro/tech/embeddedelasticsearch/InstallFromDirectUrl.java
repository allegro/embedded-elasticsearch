package pl.allegro.tech.embeddedelasticsearch;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InstallFromDirectUrl implements InstallationSource {
    
    private final URL downloadUrl;
    private final String version;
    private final boolean ossFlavor;

    InstallFromDirectUrl(URL downloadUrl) {
        this.downloadUrl = downloadUrl;
        this.version = versionFromUrl(downloadUrl);
        this.ossFlavor = ossFlavorFromUrl(downloadUrl);
    }

    @Override
    public URL resolveDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public String determineVersion() {
        return version;
    }

    @Override
    public boolean isOssFlavor() {
        return ossFlavor;
    }

    private String versionFromUrl(URL url) {
        return parseUrl(url, "versionGroup");
    }

    private boolean ossFlavorFromUrl(URL url) {
        return "-oss".equals(parseUrl(url, "ossGroup"));
    }

    private String parseUrl(URL url, String groupName) {
        Pattern versionPattern = Pattern.compile("(?<ossGroup>-oss)?-(?<versionGroup>[^/]*).zip");
        Matcher matcher = versionPattern.matcher(url.toString());
        if (matcher.find()) {
            return matcher.group(groupName);
        }
        throw new IllegalArgumentException("Cannot find version in this url. Note that I was looking for zip archive with name in format: \"anyArchiveName-versionInAnyFormat.zip\". Examples of valid urls:\n" +
                "- http://example.com/elasticsearch-2.3.0.zip\n" +
                "- http://example.com/elasticsearch-oss-6.4.0.zip\n" +
                "- http://example.com/myDistributionOfElasticWithChangedName-1.0.0.zip\n" +
                "- http://example.com/myDistributionOfElasticWithChangedName-oss-6.4.0.zip");
    }
}
