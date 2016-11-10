package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification

class ValidationSpec extends Specification {

    static final ELASTIC_VERSION = "2.2.0"
    static final ELASTIC_DOWNLOAD_URL = new URL("https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/2.2.0/elasticsearch-2.2.0.zip")
    
    def "should throw exception on missing elastic version and download url"() {
        when:
            EmbeddedElastic.builder()
                    .build()
                    .start()
        then:
            thrown(InvalidSetupException)
    }
    
    def "should construct embedded elastic with minimal required arguments and version"() {
        when:
            EmbeddedElastic.builder()
                    .withElasticVersion(ELASTIC_VERSION)
                    .build()
                    .start()
                    .stop()
        then: 
            noExceptionThrown()
    }

    def "should construct embedded elastic with minimal required arguments and url"() {
        when:
            EmbeddedElastic.builder()
                    .withDownloadUrl(ELASTIC_DOWNLOAD_URL)
                    .build()
                    .start()
                    .stop()
        then:
            noExceptionThrown()
    }

    def "should throw exception on download url without specified elastic version inside"() {
        when:
            EmbeddedElastic.builder()
                    .withDownloadUrl(new URL("http://some.invalid.link.example.com"))
                    .build()
                    .start()
                    .stop()
        then:
            thrown(IllegalArgumentException)
    }

}
