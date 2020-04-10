package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification
import spock.lang.Unroll

class InstallationSourceSpec extends Specification {
    static {
        //Need to do this here since we read the value statically
        System.setProperty("os.name", "Linux")
    }

    def "should construct valid url for version"() {
        given:
            final installationSource = new InstallFromVersion("5.0.0-alpha1")
        when:
            final resolvedUrl = installationSource.resolveDownloadUrl()
        then:
            resolvedUrl != null
    }

    def "should construct valid url for platform specific version"() {
        given:
            final installationSource = new InstallFromVersion("7.6.2")
        when:
            final resolvedUrl = installationSource.resolveDownloadUrl()
        then:
            resolvedUrl != null
            resolvedUrl.toExternalForm() == "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.6.2-linux-x86_64.tar.gz"
    }

    def "should extract properly version from normal url"() {
        given:
            final expectedVersion = "2.3.4"
            final installationSource = new InstallFromDirectUrl(new URL("https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/2.3.4/elasticsearch-2.3.4.zip"))
        when:
            final determinedVersion = installationSource.determineVersion()
        then:
            determinedVersion == expectedVersion
    }

    @Unroll
    def "should extract properly version from non-standard urls"() {
        when:
            final extractedVersion = new InstallFromDirectUrl(new URL(url)).determineVersion()
        then:
            extractedVersion == version
        where:
            url                                                                   | version
            "http://elasticsearch-download.example.com/elasticsearch-4.0.0.zip"   | "4.0.0"
            "http://example.com/elasticsearch-10.0.0-SNAPSHOT-fix-branch-123.zip" | "10.0.0-SNAPSHOT-fix-branch-123"
            "http://example.com/abc-5.0.0.zip"                                    | "5.0.0"
    }

    def "should throw exception when version is missing in url"() {
        when:
            new InstallFromDirectUrl(new URL("http://example.com/elasticsearch"))
        then:
            thrown(IllegalArgumentException)
    }

    def "should throw exception on version with path traversal"() {
        given:
            final versionWithPathTraversal = "../../etc/shadow"

        when:
            new InstallFromVersion(versionWithPathTraversal)

        then:
            thrown(IllegalArgumentException)
    }

}
