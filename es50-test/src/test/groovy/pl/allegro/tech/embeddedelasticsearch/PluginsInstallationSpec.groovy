package pl.allegro.tech.embeddedelasticsearch

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.TEST_ES_JAVA_OPTS
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.TEST_START_TIMEOUT

class PluginsInstallationSpec extends Specification {

    static final HTTP_PORT_VALUE = 9200

    def "should install plugin from url"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-stempel/analysis-stempel-5.0.0.zip")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["analysis-stempel"].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    def "should install plugin from name"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("analysis-icu")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["analysis-icu"].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    def "should install multiple plugins"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin("https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-stempel/analysis-stempel-5.0.0.zip")
                    .withPlugin("analysis-icu")
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == ["analysis-stempel", "analysis-icu"].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    EmbeddedElastic.Builder baseEmbeddedElastic() {
        return EmbeddedElastic.builder()
                .withElasticVersion("5.0.0")
                .withEsJavaOpts(TEST_ES_JAVA_OPTS)
                .withStartTimeout(TEST_START_TIMEOUT, MINUTES)
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
