package pl.allegro.tech.embeddedelasticsearch

public interface ElasticInfo {
    
    static final ELASTIC_VERSION = "5.0.0"
    static final ELASTIC_DOWNLOAD_URL = new URL("https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.0.0.zip")
    
    static final PLUGIN_BY_NAME = "analysis-icu"
    static final PLUGIN_BY_URL = "https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-stempel/analysis-stempel-5.0.0.zip"
    
}
