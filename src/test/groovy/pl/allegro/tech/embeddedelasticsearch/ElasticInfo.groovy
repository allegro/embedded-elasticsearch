package pl.allegro.tech.embeddedelasticsearch

interface ElasticInfo {
    
    static final ELASTIC_VERSION = "5.0.0"
    static final ELASTIC_DOWNLOAD_URL = new URL("https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.0.0.zip")
    
    static final ANALYSIS_ICU_PLUGIN = "analysis-icu"
    static final ANALYSIS_ICU_PLUGIN_DOWNLOAD_URL = new URL("https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/analysis-icu-5.0.0.zip")
    
}
