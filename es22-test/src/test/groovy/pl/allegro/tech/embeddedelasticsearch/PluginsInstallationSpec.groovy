package pl.allegro.tech.embeddedelasticsearch

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.TEST_ES_JAVA_OPTS
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.START_TIMEOUT_IN_MINUTES

class PluginsInstallationSpec extends Specification {

    static final HTTP_PORT_VALUE = 9200

    def "should install plugin from url"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-decompound/2.2.0.0/elasticsearch-analysis-decompound-2.2.0.0-plugin.zip")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["decompound"].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    def "should install plugin from name"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("analysis-stempel")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["analysis-stempel"].toSet()

        cleanup:
            embeddedElastic.stop()
    }

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

    def "should install multiple plugins"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-decompound/2.2.0.0/elasticsearch-analysis-decompound-2.2.0.0-plugin.zip")
                    .withPlugin("analysis-stempel")
                    .withPlugin("lmenezes/elasticsearch-kopf")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["decompound", "analysis-stempel", "kopf"].toSet()

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

    Set<String> fetchInstalledPluginsList() {
        final request = new HttpGet("http://localhost:$HTTP_PORT_VALUE/_cat/plugins?format=json")
        final client = HttpClients.createDefault()
        final response = client.execute(request)
        final responseBody = response.entity.content.text
        final json = new JsonSlurper().parseText(responseBody)
        return json.collect { it.component }.toSet()
    }

}
