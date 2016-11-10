package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification
import spock.lang.Unroll

class ElasticDownloadUrlUtilsSpec extends Specification {

    def "should construct valid url for version"() {
        when:
            final url = ElasticDownloadUrlUtils.urlFromVersion("5.0.0-alpha1")
        then:
            url != null
    }

    def "should extract properly version from normal url"() {
        given:
            final downloadUrl = new URL("https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/2.3.4/elasticsearch-2.3.4.zip")
            final version = "2.3.4"
        when:
            final extractedVersion = ElasticDownloadUrlUtils.versionFromUrl(downloadUrl)
        then:
            version == extractedVersion
    }

    @Unroll
    def "should extract properly version from non-standard urls"() {
        when:
            final extractedVersion = ElasticDownloadUrlUtils.versionFromUrl(new URL(url))
        then:
            extractedVersion == version
        where:
            url                                                                   | version
            "http://elasticsearch-download.example.com/elasticsearch-4.0.0.zip"   | "4.0.0"
            "http://example.com/elasticsearch-10.0.0-SNAPSHOT-fix-branch-123.zip" | "10.0.0-SNAPSHOT-fix-branch-123"
            "http://example.com/abc-5.0.0.zip"                                    | "5.0.0"
    }

    def "should throw exception when version is missing in url"() {
        given:
            final downloadUrl = new URL("http://example.com/elasticsearch")
        when:
            ElasticDownloadUrlUtils.versionFromUrl(downloadUrl)
        then:
            thrown(IllegalArgumentException)
    }

    def "should construct local file name if one is given in the url"() {
        when:
            final constructedLocalFileName = ElasticDownloadUrlUtils.constructLocalFileName(new URL(url))

        then:
            constructedLocalFileName == expectedLocalFileName

        where:
            url                                                             | expectedLocalFileName
            "http://plugin-download.example.com/a/b/c/plugin-4.0.0.zip"     | "plugin-4.0.0.zip"
            "http://plugin-download.example.com/a/b/c/plugin-4.0.0.zip?a=b" | "plugin-4.0.0.zip"
            "file:/C:/bin/distributions/morfologik-2.3.4.0-plugin.zip"      | "morfologik-2.3.4.0-plugin.zip"
            "file:/home/bin/distributions/morfologik-2.3.4.0-plugin.zip"    | "morfologik-2.3.4.0-plugin.zip"
    }

    def "should use random local file name if it can't be extracted from the url"() {
        given:
            def urlWithoutFileName = new URL("http://plugin-download.example.com")

        when:
            final constructedLocalFileName = ElasticDownloadUrlUtils.constructLocalFileName(urlWithoutFileName)

        then:
            constructedLocalFileName ==~ /\w{10}/
    }
    
    def "should throw exception on version with path traversal"() {
        given:
            final versionWithPathTraversal = "../../etc/shadow"
        
        when:
            ElasticDownloadUrlUtils.urlFromVersion(versionWithPathTraversal)
        
        then:
            thrown(IllegalArgumentException)
    }
    
    def "constructLocalFileName should be immune to null byte injection"() {
        given:
            final urlWithNullByte = "http://example.com/\0file.txt"
        
        when:
            ElasticDownloadUrlUtils.constructLocalFileName(new URL(urlWithNullByte))
        
        then:
            thrown(IllegalArgumentException)
    }
}
