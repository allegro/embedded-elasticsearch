package pl.allegro.tech.embeddedelasticsearch

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.START_TIMEOUT_IN_MINUTES
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.TEST_ES_JAVA_OPTS

class PluginsInstallationSpec extends PluginsInstallationBaseSpec {

    static final HTTP_PORT_VALUE = 9200

    def "should install plugin from github"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("lmenezes/elasticsearch-kopf")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["kopf"].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    EmbeddedElastic.Builder baseEmbeddedElastic() {
        return EmbeddedElastic.builder()
                .withElasticVersion("2.2.0")
                .withEsJavaOpts(TEST_ES_JAVA_OPTS)
                .withStartTimeout(START_TIMEOUT_IN_MINUTES, MINUTES)
                .withSetting(PopularProperties.HTTP_PORT, HTTP_PORT_VALUE)
    }

    @Override
    String pluginByUrlUrl() {
        return "http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-decompound/2.2.0.0/elasticsearch-analysis-decompound-2.2.0.0-plugin.zip"
    }

    @Override
    String pluginByUrlName() {
        return "decompound"
    }

    @Override
    String pluginByName() {
        return "analysis-stempel"
    }
}
