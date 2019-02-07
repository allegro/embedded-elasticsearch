package pl.allegro.tech.embeddedelasticsearch;

public class IndexRequest {

    private final String indexName;
    private final String indexType;
    private final String id;
    private final String routing;
    private final String json;

    public IndexRequest(String indexName, String indexType, String json) {
        this(indexName, indexType, json, null, null);
    }

    public IndexRequest(String indexName, String indexType, String json, String id) {
        this(indexName, indexType, json, id, null);
    }

    public IndexRequest(String indexName, String indexType, String json, String id, String routing) {
        this.indexName = indexName;
        this.indexType = indexType;
        this.id = id;
        this.routing = routing;
        this.json = json;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexType() {
        return indexType;
    }

    public String getId() {
        return id;
    }

    public String getRouting() {
        return routing;
    }

    public String getJson() {
        return json;
    }
}
