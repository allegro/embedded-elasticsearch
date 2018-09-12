package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification

import static java.util.concurrent.TimeUnit.MINUTES

class SynchronicitySpec extends Specification {
    static final ELASTIC_VERSION = "2.2.0"

    def "should not throw exception on starting embedded instance more than once from different threads"() {
        when:
        final server = EmbeddedElastic.builder()
                .withElasticVersion(ELASTIC_VERSION)
                .withStartTimeout(TEST_START_TIMEOUT_IN_MINUTES, MINUTES)
                .build()
        def errorsInStartup = false
        final startJob = {
            try {
                server.start()
            } catch(Exception e) {
                errorsInStartup = true
            }
        }
        final asyncStarter1 = new Thread(startJob)
        final asyncStarter2 = new Thread(startJob)
        asyncStarter1.start()
        asyncStarter2.start()
        asyncStarter1.join()
        asyncStarter2.join()
        then:
        !errorsInStartup

        cleanup:
        server.stop()
    }
    static final TEST_START_TIMEOUT_IN_MINUTES = 1

}
