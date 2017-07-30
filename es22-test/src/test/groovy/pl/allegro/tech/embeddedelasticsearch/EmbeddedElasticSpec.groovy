package pl.allegro.tech.embeddedelasticsearch

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress

import static EmbeddedElasticConfiguration.START_TIMEOUT_IN_MINUTES
import static java.util.concurrent.TimeUnit.MINUTES
import static org.elasticsearch.index.query.QueryBuilders.termQuery
import static pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticConfiguration.TEST_ES_JAVA_OPTS
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.CLUSTER_NAME
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.TRANSPORT_TCP_PORT
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.*

class EmbeddedElasticSpec extends EmbeddedElasticBaseSpec {

    static final ELASTIC_VERSION = "2.2.0"
    static final TRANSPORT_TCP_PORT_VALUE = 9930
    static final CLUSTER_NAME_VALUE = "myTestCluster"

    static EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(TRANSPORT_TCP_PORT, TRANSPORT_TCP_PORT_VALUE)
            .withSetting(CLUSTER_NAME, CLUSTER_NAME_VALUE)
            .withEsJavaOpts(TEST_ES_JAVA_OPTS)
            .withIndex(CARS_INDEX_NAME, CARS_INDEX)
            .withIndex(BOOKS_INDEX_NAME, BOOKS_INDEX)
            .withStartTimeout(START_TIMEOUT_IN_MINUTES, MINUTES)
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
        Settings settings = Settings.settingsBuilder().put("cluster.name", CLUSTER_NAME_VALUE).build();
        TransportClient client = TransportClient.builder().settings(settings).build();
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), TRANSPORT_TCP_PORT_VALUE));
        return client;
    }

    @Override
    List<String> fetchAllDocuments() {
        client.prepareSearch().execute().actionGet().hits.hits.toList().collect { it.sourceAsString }
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
