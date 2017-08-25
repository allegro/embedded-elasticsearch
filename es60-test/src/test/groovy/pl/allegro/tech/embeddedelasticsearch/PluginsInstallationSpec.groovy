package pl.allegro.tech.embeddedelasticsearch

class PluginsInstallationSpec extends PluginsInstallationBaseSpec {

    static final HTTP_PORT_VALUE = 9200

    EmbeddedElastic.Builder baseEmbeddedElastic() {
        return EmbeddedElastic.builder()
                .withElasticVersion("6.0.0-beta1")
                .withEsJavaOpts("-Xms128m -Xmx512m")
                .withSetting(PopularProperties.HTTP_PORT, HTTP_PORT_VALUE)
    }

    @Override
    String pluginByUrlUrl() {
        return "https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-stempel/analysis-stempel-6.0.0-beta1.zip"
    }

    @Override
    String pluginByUrlName() {
        return "analysis-stempel"
    }

    @Override
    String pluginByName() {
        return "analysis-icu"
    }
}