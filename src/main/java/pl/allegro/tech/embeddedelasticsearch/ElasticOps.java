package pl.allegro.tech.embeddedelasticsearch;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

class ElasticOps {

    private static final Logger logger = LoggerFactory.getLogger(ElasticOps.class);
    
    private final Client elasticClient;
    private final IndicesDescription indicesDescription;

    ElasticOps(Client elasticClient, IndicesDescription indicesDescription) {
        this.elasticClient = elasticClient;
        this.indicesDescription = indicesDescription;
    }
    
    void createIndices() {
        indicesDescription.getIndicesNames().forEach(this::createIndex);
    }
    
    void createIndex(String indexName) {
        if (!indexExists(indexName)) {
            CreateIndexRequestBuilder createIndexRequestBuilder = elasticClient.admin().indices().prepareCreate(indexName);
            indicesDescription.getSettings(indexName).ifPresent(createIndexRequestBuilder::setSettings);
            indicesDescription.getIndexTypes(indexName).forEach(indexType -> createIndexRequestBuilder.addMapping(indexType.getType(), indexType.getMapping()));
            createIndexRequestBuilder.execute().actionGet();
            waitForClusterYellow();
        }
    }
    
    private boolean indexExists(String indexName) {
        return elasticClient.admin().indices().prepareExists(indexName).get().isExists();
    }

    private void waitForClusterYellow() {
        elasticClient.admin().cluster().prepareHealth().setWaitForYellowStatus().get();
    }

    void deleteIndices() {
        indicesDescription.getIndicesNames().forEach(this::deleteIndex);
    }

    void deleteIndex(String indexName) {
        if (indexExists(indexName)) {
            elasticClient.admin().indices().prepareDelete(indexName).execute().actionGet();
        } else {
            logger.warn("Index: {} does not exists so cannot be removed", indexName);
        }
    }

    void index(String indexName, String indexType, Collection<String> jsons) {
        BulkRequestBuilder bulkRequestBuilder = elasticClient.prepareBulk();
        jsons.stream()
                .map(json -> elasticClient.prepareIndex(indexName, indexType).setSource(json))
                .forEach(bulkRequestBuilder::add);
        bulkRequestBuilder.execute().actionGet();
        refresh();
    }

    void indexWithIds(String indexName, String indexType, Collection<DocumentWithId> idJsonMap) {
        BulkRequestBuilder bulkRequestBuilder = elasticClient.prepareBulk();
        idJsonMap.stream()
                .map(document -> elasticClient
                        .prepareIndex(indexName, indexType)
                        .setSource(document.getDocument())
                        .setId(document.getId()))
                .forEach(bulkRequestBuilder::add);
        bulkRequestBuilder.execute().actionGet();
        refresh();
    }

    void refresh() {
        elasticClient.admin().indices().prepareRefresh().execute().actionGet();
    }
}
