package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification
import spock.lang.Unroll

class InstallationSourceSpec extends Specification {

    def "should construct valid url for version"() {
        given:
            final installationSource = new InstallFromVersion("5.0.0-alpha1")
        when:
            final resolvedUrl = installationSource.resolveDownloadUrl()
        then:
            resolvedUrl != null
    }

    def "should construct valid url for oss flavor"() {
        given:
            final expectedVersion = "6.3.0"
            final installationSource = new InstallFromVersion("oss-6.3.0")
        when:
            final resolvedUrl = installationSource.resolveDownloadUrl()
            final determinedVersion = installationSource.determineVersion()
        then:
            resolvedUrl == new URL("https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-oss-6.3.0.zip")
            determinedVersion == expectedVersion
            installationSource.isOssFlavor()
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
            ossFlavor == ossFlavor
        where:
            url                                                                   | version | ossFlavor
            "http://elasticsearch-download.example.com/elasticsearch-4.0.0.zip"   | "4.0.0" | false
            "http://example.com/elasticsearch-10.0.0-SNAPSHOT-fix-branch-123.zip" | "10.0.0-SNAPSHOT-fix-branch-123" | false
            "http://example.com/abc-5.0.0.zip"                                    | "5.0.0" | false
            "http://example.com/abc-oss-5.0.0.zip"                                | "5.0.0" | true
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
