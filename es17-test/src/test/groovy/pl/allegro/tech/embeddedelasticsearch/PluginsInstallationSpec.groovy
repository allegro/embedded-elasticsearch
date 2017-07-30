package pl.allegro.tech.embeddedelasticsearch

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.START_TIMEOUT_IN_MINUTES
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.TEST_ES_JAVA_OPTS

class PluginsInstallationSpec extends PluginsInstallationBaseSpec {

    static final HTTP_PORT_VALUE = 9200

    EmbeddedElastic.Builder baseEmbeddedElastic() {
        return EmbeddedElastic.builder()
                .withElasticVersion("1.7.5")
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

    @Override
    String pluginByUrlUrl() {
        return "http://download.elasticsearch.org/elasticsearch/elasticsearch-cloud-aws/elasticsearch-cloud-aws-2.7.1.zip"
    }

    @Override
    String pluginByUrlName() {
        return "cloud-aws"
    }

    @Override
    String pluginByName() {
        return "elasticsearch/elasticsearch-analysis-stempel/2.7.0"
    }

    @Override
    String pluginByNameName() {
        return "analysis-stempel"
    }
}
