package pl.allegro.tech.embeddedelasticsearch

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import spock.lang.Specification

class PluginsInstallationSpec extends Specification {

    static final HTTP_PORT_VALUE = 9200

    def "should install plugin from url"() {
        given:
        final embeddedElastic = baseEmbeddedElastic()
                .withPlugin("http://download.elasticsearch.org/elasticsearch/elasticsearch-cloud-aws/elasticsearch-cloud-aws-2.7.1.zip")
                .build()

        when:
        embeddedElastic.start()

        then:
        fetchInstalledPluginsList() == ["cloud-aws"].toSet()

        cleanup:
        embeddedElastic.stop()
    }

    def "should install plugin from name"() {
        given:
        final embeddedElastic = baseEmbeddedElastic()
                .withPlugin("elasticsearch/elasticsearch-analysis-stempel/2.7.0")
                .build()

        when:
        embeddedElastic.start()

        then:
        fetchInstalledPluginsList() == ["analysis-stempel"].toSet()

        cleanup:
        embeddedElastic.stop()
    }

    def "should install multiple plugins"() {
        given:
        final embeddedElastic = baseEmbeddedElastic()
                .withPlugin("elasticsearch/elasticsearch-analysis-stempel/2.7.0")
                .withPlugin("elasticsearch/elasticsearch-cloud-aws/2.7.1")
                .build()

        when:
        embeddedElastic.start()

        then:
        fetchInstalledPluginsList() == ["cloud-aws", "analysis-stempel"].toSet()

        cleanup:
        embeddedElastic.stop()
    }

    EmbeddedElastic.Builder baseEmbeddedElastic() {
        return EmbeddedElastic.builder()
                .withElasticVersion("1.7.5")
                .withEsJavaOpts("-Xms128m -Xmx512m")
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
