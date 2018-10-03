package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT

class SynchronicitySpec extends Specification {
    static final ELASTIC_VERSION = "2.2.0"
    static final HTTP_PORT_VALUE = 9999

    def "should not throw exception on starting embedded instance more than once"() {
        when:
        final server = EmbeddedElastic.builder()
                .withElasticVersion(ELASTIC_VERSION)
                .withStartTimeout(TEST_START_TIMEOUT_IN_MINUTES, MINUTES)
                .withSetting(HTTP_PORT, HTTP_PORT_VALUE)
                .build()
        server.start()
        server.start()

        then:
        noExceptionThrown()

        cleanup:
        server.stop()
    }
    static final TEST_START_TIMEOUT_IN_MINUTES = 1

}
