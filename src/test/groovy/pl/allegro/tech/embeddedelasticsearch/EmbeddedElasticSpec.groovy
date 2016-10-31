package pl.allegro.tech.embeddedelasticsearch

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.embeddedelasticsearch.ElasticInfo.ELASTIC_VERSION
import static pl.allegro.tech.embeddedelasticsearch.ElasticInfo.PLUGIN_BY_NAME
import static pl.allegro.tech.embeddedelasticsearch.ElasticInfo.PLUGIN_BY_URL
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.AMERICAN_PSYCHO
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.AUDIO_BOOK_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOKS_INDEX
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOKS_INDEX_NAME
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CARS_INDEX
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CARS_INDEX_NAME
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CAR_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CLUSTER_NAME
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.FIAT_126p
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.PAPER_BOOK_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.PORT
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.SHINING
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.toJson

class EmbeddedElasticSpec extends Specification {

    @Shared
    EmbeddedElastic embeddedElastic

    def setupSpec() {
        embeddedElastic = EmbeddedElastic.builder()
                .withElasticVersion(ELASTIC_VERSION)
                .withPlugin(PLUGIN_BY_NAME)
                .withPlugin(PLUGIN_BY_URL)
                .withPortNumber(PORT)
                .withClusterName(CLUSTER_NAME)
                .withIndex(CARS_INDEX_NAME, CARS_INDEX)
                .withIndex(BOOKS_INDEX_NAME, BOOKS_INDEX)
                .build()
                .start()
    }

    def setup() {
        embeddedElastic.recreateIndices()
    }

    def cleanupSpec() {
        embeddedElastic.stop()
    }

    def "should index document"() {
        when:
        index(FIAT_126p)

        then:
        final result = createClient()
                .prepareSearch(CARS_INDEX_NAME)
                .setTypes(CAR_INDEX_TYPE)
                .setQuery(QueryBuilders.termQuery("model", FIAT_126p.model))
                .execute().actionGet()
        result.hits.totalHits() == 1
        assertJsonsEquals(toJson(FIAT_126p), result.hits.hits[0].sourceAsString)
    }

    def "should index document with id"() {
        given:
        final id = "some-id"
        final document = toJson(FIAT_126p)

        when:
        embeddedElastic.index(CARS_INDEX_NAME, CAR_INDEX_TYPE, ["$id": document])

        then:
        final GetResponse result = createClient()
                .prepareGet(CARS_INDEX_NAME, CAR_INDEX_TYPE, id)
                .execute().actionGet()
        result.exists
        assertJsonsEquals(document, result.sourceAsString)
    }

    def "should recreate only specified index"() {
        given: "indices with some documents"
        index(FIAT_126p)
        index(SHINING)
        index(AMERICAN_PSYCHO)

        when: "recreating index"
        embeddedElastic.recreateIndex(BOOKS_INDEX_NAME)

        then: "recreated index should be empty"
        with(createClient().prepareSearch(BOOKS_INDEX_NAME).execute().actionGet()) { booksIndexSearchResult ->
            booksIndexSearchResult.hits.size() == 0
        }

        and: "other index should be untouched"
        with(createClient().prepareSearch(CARS_INDEX_NAME).execute().actionGet()) { carsIndexSearchResult ->
            carsIndexSearchResult.hits.size() == 1
        }
    }

    def "should recreate all indices"() {
        given: "indices with some documents"
        index(FIAT_126p)
        index(SHINING)
        index(AMERICAN_PSYCHO)

        when: "recreating index"
        embeddedElastic.recreateIndices()

        then: "recreated indices should be empty"
        final result = createClient().prepareSearch().execute().actionGet()
        result.hits.size() == 0
    }

    def "should place document under right index and type"() {
        when:
        index(SHINING)
        index(AMERICAN_PSYCHO)

        then:
        with(createClient().prepareSearch(BOOKS_INDEX_NAME).setTypes(PAPER_BOOK_INDEX_TYPE).execute().actionGet()) { paperBooksSearchResult ->
            paperBooksSearchResult.hits.hits.size() == 1
            assertJsonsEquals(toJson(SHINING), paperBooksSearchResult.hits.hits[0].sourceAsString)
        }

        and:
        with(createClient().prepareSearch(BOOKS_INDEX_NAME).setTypes(AUDIO_BOOK_INDEX_TYPE).execute().actionGet()) { audioBooksSearchResult ->
            audioBooksSearchResult.hits.hits.size() == 1
            assertJsonsEquals(toJson(AMERICAN_PSYCHO), audioBooksSearchResult.hits.hits[0].sourceAsString)
        }
    }

    def "should return all indexed documents from all indices"() {
        given:
        index(FIAT_126p)
        index(SHINING)
        index(AMERICAN_PSYCHO)

        when:
        final documents = embeddedElastic.fetchAllDocuments()

        then:
        documents.size() == 3
    }

    def "should return all indexed documents from selected indice"() {
        given:
        index(FIAT_126p)
        index(SHINING)
        index(AMERICAN_PSYCHO)

        when:
        final documents = embeddedElastic.fetchAllDocuments(CARS_INDEX_NAME)

        then:
        documents.size() == 1

        and:
        documents[0].contains(FIAT_126p.model)
    }

    def "plugins should be installed"() {
        given:
        NodesInfoRequest nodesInfoRequest = new NodesInfoRequest()
        nodesInfoRequest.plugins()

        when:
        NodesInfoResponse infoResponse = createClient().admin().cluster()
                .nodesInfo(nodesInfoRequest).actionGet()

        then:
        infoResponse.nodes.size() == 1
        def plugins = infoResponse.nodes.get(0).plugins.getPluginInfos()
                .stream().map({ plugin -> plugin.name }).collect()
        plugins.size() == 2
        plugins.contains("analysis-icu")
        plugins.contains("analysis-stempel")
    }

    Client createClient() {
        Settings settings = Settings.builder()
                .put("cluster.name", CLUSTER_NAME)
                .build();

        Client client = new PreBuiltTransportClient(settings)
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(SampleIndices.HOST), SampleIndices.PORT));
        return client;
    }

    void assertJsonsEquals(String expectedJson, String actualJson) {
        JSONAssert.assertEquals(expectedJson, actualJson, false)
    }

    void index(SampleIndices.Car car) {
        embeddedElastic.index(CARS_INDEX_NAME, CAR_INDEX_TYPE, toJson(car))
    }

    void index(SampleIndices.PaperBook book) {
        embeddedElastic.index(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(book))
    }

    void index(SampleIndices.AudioBook book) {
        embeddedElastic.index(BOOKS_INDEX_NAME, AUDIO_BOOK_INDEX_TYPE, toJson(book))
    }

}
