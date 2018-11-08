package pl.allegro.tech.embeddedelasticsearch;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InstallFromResources implements InstallationSource {

    private final URL resource;
    private final String version;
    private final boolean ossFlavor;

    InstallFromResources(String inResourcePath) {
        resource = Thread.currentThread().getContextClassLoader().getResource(inResourcePath);
        version = versionFromUrl(resource);
        ossFlavor = ossVersionFromUrl(resource);
    }
    
    @Override
    public String determineVersion() {
        return version;
    }

    @Override
    public URL resolveDownloadUrl() {
        return resource;
    }

    @Override
    public boolean isOssFlavor() {
        return ossFlavor;
    }

    private String versionFromUrl(URL url) {
        return parseUrl(url, "versionGroup");
    }

    private String parseUrl(URL url, String groupName) {
        Pattern versionPattern = Pattern.compile("(?<ossGroup>-oss)?-(?<versionGroup>[^/]*).zip");
        Matcher matcher = versionPattern.matcher(url.toString());
        if (matcher.find()) {
            return matcher.group(groupName);
        }
        throw new IllegalArgumentException("Cannot find version in this archive name. Note that I was looking for zip archive with name in format: \"anyArchiveName-versionInAnyFormat.zip\". Examples of valid urls:\n" +
                "- elasticsearch-2.3.0.zip\n" +
                "- elasticsearch-oss-6.4.0.zip\n" +
                "- myDistributionOfElasticWithChangedName-1.0.0.zip\n" +
                "- myDistributionOfElasticWithChangedName-oss-6.4.0.zip");
    }

    private boolean ossVersionFromUrl(URL url) {
        return "-oss".equals(parseUrl(url, "ossGroup"));
    }
}
