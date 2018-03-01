package pl.allegro.tech.embeddedelasticsearch

import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

import static PopularProperties.CLUSTER_NAME
import static PopularProperties.TRANSPORT_TCP_PORT
import static java.util.concurrent.TimeUnit.MINUTES
import static org.elasticsearch.index.query.QueryBuilders.termQuery
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.*

class EmbeddedElasticSpec extends EmbeddedElasticCoreApiVersions5AndUnder {

    static final ELASTIC_VERSION = "5.5.1"
    static final TRANSPORT_TCP_PORT_VALUE = 9930
    static final CLUSTER_NAME_VALUE = "myTestCluster"

    static EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(TRANSPORT_TCP_PORT, TRANSPORT_TCP_PORT_VALUE)
            .withSetting(CLUSTER_NAME, CLUSTER_NAME_VALUE)
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withTemplate(CARS_TEMPLATE_NAME, CARS_TEMPLATE)
            .withIndex(CARS_INDEX_NAME)
            .withIndex(BOOKS_INDEX_NAME, BOOKS_INDEX_MULTI_TYPE)
            .withStartTimeout(1, MINUTES)
            .build()
            .start()

    static Client client = createClient()

    def setup() {
        embeddedElastic.recreateIndices()
    }

    def cleanupSpec() {
        client.close()
        embeddedElastic.stop()
    }

    static Client createClient() {
        Settings settings = Settings.builder().put("cluster.name", CLUSTER_NAME_VALUE).build()
        return new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), TRANSPORT_TCP_PORT_VALUE))
    }

    @Override
    List<String> fetchAllDocuments() {
        fetchAllDocuments(CARS_INDEX_NAME) + fetchAllDocuments(BOOKS_INDEX_NAME)
    }

    @Override
    List<String> fetchAllDocuments(String indexName) {
        client.prepareSearch(indexName)
                .execute().actionGet()
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    List<String> fetchAllDocuments(String indexName, String typeName) {
        client.prepareSearch(indexName)
                .setTypes(typeName)
                .execute().actionGet()
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    List<String> searchByTerm(String indexName, String typeName, String fieldName, String value) {
        client.prepareSearch(indexName)
                .setTypes(typeName)
                .setQuery(termQuery(fieldName, value))
                .execute().actionGet()
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    String getById(String indexName, String typeName, String id) {
        client.prepareGet(indexName, typeName, id).execute().actionGet().sourceAsString
    }
}
