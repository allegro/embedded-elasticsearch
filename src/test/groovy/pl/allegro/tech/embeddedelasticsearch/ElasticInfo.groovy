package pl.allegro.tech.embeddedelasticsearch

interface ElasticInfo {
    
    static final ELASTIC_VERSION = "2.2.0"
    static final ELASTIC_DOWNLOAD_URL = new URL("https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/zip/elasticsearch/2.2.0/elasticsearch-2.2.0.zip")
    
    static final DECOMPOUND_PLUGIN = "decompound"
    static final DECOMPOUND_PLUGIN_DOWNLOAD_URL = new URL("http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-analysis-decompound/2.2.0.0/elasticsearch-analysis-decompound-2.2.0.0-plugin.zip")
    
}
