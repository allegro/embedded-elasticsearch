package pl.allegro.tech.embeddedelasticsearch

import org.apache.http.HttpHost
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.*
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOKS_INDEX_7x
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CARS_TEMPLATE_7x

class EmbeddedElasticSpec extends EmbeddedElasticCoreApiBaseSpec {

    static final ELASTIC_VERSION = "7.0.0"
    static final HTTP_PORT_VALUE = 9999

    static EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(HTTP_PORT, HTTP_PORT_VALUE)
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withTemplate(CARS_TEMPLATE_NAME, CARS_TEMPLATE_7x)
            .withIndex(CARS_INDEX_NAME)
            .withIndex(BOOKS_INDEX_NAME, BOOKS_INDEX_7x)
            .withStartTimeout(2, MINUTES)
            .build()
            .start()

    static RestHighLevelClient client = createClient()

    def setup() {
        embeddedElastic.recreateIndices()
    }

    def cleanupSpec() {
        client.close()
        embeddedElastic.stop()
    }

    static RestHighLevelClient createClient() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", HTTP_PORT_VALUE)))
    }

    @Override
    List<String> fetchAllDocuments() {
        fetchAllDocuments(CARS_INDEX_NAME) + fetchAllDocuments(BOOKS_INDEX_NAME)
    }

    @Override
    List<String> fetchAllDocuments(String indexName) {
        final searchRequest = new SearchRequest(indexName)
                .source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

        client.search(searchRequest, RequestOptions.DEFAULT)
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    List<String> fetchAllDocuments(String indexName, String typeName) {
        fetchAllDocuments(indexName)
    }

    @Override
    List<String> fetchAllDocuments(String indexName, String typeName, String routing) {
        final searchRequest = new SearchRequest(indexName)
                .routing(routing)
                .source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()))

        client.search(searchRequest, RequestOptions.DEFAULT)
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    List<String> searchByTerm(String indexName, String typeName, String fieldName, String value) {
        final searchRequest = new SearchRequest()
                .source(new SearchSourceBuilder().query(QueryBuilders.termQuery(fieldName, value)));

        client.search(searchRequest, RequestOptions.DEFAULT)
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    String getById(String indexName, String typeName, String id) {
        final getRequest = new GetRequest(indexName, id)
        client.get(getRequest, RequestOptions.DEFAULT).sourceAsString
    }
}
