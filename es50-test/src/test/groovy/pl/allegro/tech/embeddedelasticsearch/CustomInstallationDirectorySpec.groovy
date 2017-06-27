package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification
import spock.lang.Stepwise

import static PopularProperties.CLUSTER_NAME
import static PopularProperties.TRANSPORT_TCP_PORT
import static java.util.concurrent.TimeUnit.MINUTES

@Stepwise
class CustomInstallationDirectorySpec extends Specification {
    
    static final ELASTIC_VERSION = "5.4.0"
    static final TRANSPORT_TCP_PORT_VALUE = 9930
    static final CLUSTER_NAME_VALUE = "customDirectoryTestCluster"
    static final File INSTALLATION_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-custom-dir")
    static final String ELASTIC_DIRECTORY_NAME = "elasticsearch-${ELASTIC_VERSION}"

    static EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(TRANSPORT_TCP_PORT, TRANSPORT_TCP_PORT_VALUE)
            .withSetting(CLUSTER_NAME, CLUSTER_NAME_VALUE)
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withInstallationDirectory(INSTALLATION_DIRECTORY)
            .withCleanInstallationDirectoryOnStop(false)
            .withStartTimeout(1, MINUTES)
            .build()

    void setupSpec() {
        INSTALLATION_DIRECTORY.deleteDir()
    }

    def cleanupSpec() {
        INSTALLATION_DIRECTORY.deleteDir()
    }
    
    def "should install embedded elastic on custom directory"() {
        when:
        embeddedElastic.start()

        then:
        INSTALLATION_DIRECTORY.exists()
        INSTALLATION_DIRECTORY.listFiles()
                .findAll { it.isDirectory() }
                .findAll { it.name == ELASTIC_DIRECTORY_NAME }.size() == 1
    }

    def "should not delete custom installation directory after stop"() {
        when:
        embeddedElastic.stop()

        then:
        INSTALLATION_DIRECTORY.exists()
        INSTALLATION_DIRECTORY.listFiles()
                .findAll { it.isDirectory() }
                .findAll { it.name == ELASTIC_DIRECTORY_NAME }.size() == 1
    }

}
