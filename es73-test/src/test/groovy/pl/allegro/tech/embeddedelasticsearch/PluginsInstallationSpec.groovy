package pl.allegro.tech.embeddedelasticsearch

import static java.util.concurrent.TimeUnit.MINUTES

class PluginsInstallationSpec extends PluginsInstallationBaseSpec {

    static final HTTP_PORT_VALUE = 9200

    EmbeddedElastic.Builder baseEmbeddedElastic() {
        return EmbeddedElastic.builder()
                .withElasticVersion("7.3.2")
                .withEsJavaOpts("-Xms128m -Xmx512m")
                .withSetting(PopularProperties.HTTP_PORT, HTTP_PORT_VALUE)
                .withStartTimeout(2, MINUTES)
    }

    @Override
    String pluginByUrlUrl() {
        return "https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-stempel/analysis-stempel-7.3.2.zip"
    }

    @Override
    String pluginByUrlName() {
        return "analysis-stempel"
    }

    @Override
    String pluginByName() {
        return "mapper-size"
    }
}