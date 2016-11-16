package pl.allegro.tech.embeddedelasticsearch

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static SampleIndices.AudioBook
import static SampleIndices.Car
import static SampleIndices.PaperBook
import static java.util.concurrent.TimeUnit.MINUTES

class EmbeddedElasticSpec extends Specification {

    static final ELASTIC_VERSION = "1.7.5"
    static final TRANSPORT_TCP_PORT_VALUE = 9930
    static final CLUSTER_NAME_VALUE = "myTestCluster"

    static EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(PopularProperties.TRANSPORT_TCP_PORT, TRANSPORT_TCP_PORT_VALUE)
            .withSetting(PopularProperties.CLUSTER_NAME, CLUSTER_NAME_VALUE)
            .withIndex(SampleIndices.CARS_INDEX_NAME, SampleIndices.CARS_INDEX)
            .withIndex(SampleIndices.BOOKS_INDEX_NAME, SampleIndices.BOOKS_INDEX)
            .withStartTimeout(1, MINUTES)
            .build()
            .start()
    static Client client = createClient()

    def setup() {
        embeddedElastic.recreateIndices()
    }

    def cleanupSpec() {
        embeddedElastic.stop()
    }

    def "should index document"() {
        when:
        index(SampleIndices.FIAT_126p)

        then:
        final result = client
                .prepareSearch(SampleIndices.CARS_INDEX_NAME)
                .setTypes(SampleIndices.CAR_INDEX_TYPE)
                .setQuery(QueryBuilders.termQuery("model", SampleIndices.FIAT_126p.model))
                .execute().actionGet()
        result.hits.totalHits() == 1
        assertJsonsEquals(SampleIndices.toJson(SampleIndices.FIAT_126p), result.hits.hits[0].sourceAsString)
    }

    def "should index document with id"() {
        given:
        final id = "some-id"
        final document = SampleIndices.toJson(SampleIndices.FIAT_126p)

        when:
        embeddedElastic.index(SampleIndices.CARS_INDEX_NAME, SampleIndices.CAR_INDEX_TYPE, ["$id": document])

        then:
        final GetResponse result = client
                .prepareGet(SampleIndices.CARS_INDEX_NAME, SampleIndices.CAR_INDEX_TYPE, id)
                .execute().actionGet()
        result.exists
        assertJsonsEquals(document, result.sourceAsString)
    }

    def "should recreate only specified index"() {
        given: "indices with some documents"
        index(SampleIndices.FIAT_126p)
        index(SampleIndices.SHINING)
        index(SampleIndices.AMERICAN_PSYCHO)

        when: "recreating index"
        embeddedElastic.recreateIndex(SampleIndices.BOOKS_INDEX_NAME)

        then: "recreated index should be empty"
        with(client.prepareSearch(SampleIndices.BOOKS_INDEX_NAME).execute().actionGet()) { booksIndexSearchResult ->
            booksIndexSearchResult.hits.size() == 0
        }

        and: "other index should be untouched"
        with(client.prepareSearch(SampleIndices.CARS_INDEX_NAME).execute().actionGet()) { carsIndexSearchResult ->
            carsIndexSearchResult.hits.size() == 1
        }
    }

    def "should recreate all indices"() {
        given: "indices with some documents"
        index(SampleIndices.FIAT_126p)
        index(SampleIndices.SHINING)
        index(SampleIndices.AMERICAN_PSYCHO)

        when: "recreating index"
        embeddedElastic.recreateIndices()

        then: "recreated indices should be empty"
        final result = client.prepareSearch().execute().actionGet()
        result.hits.size() == 0
    }

    def "should place document under right index and type"() {
        when:
        index(SampleIndices.SHINING)
        index(SampleIndices.AMERICAN_PSYCHO)

        then:
        with(client.prepareSearch(SampleIndices.BOOKS_INDEX_NAME).setTypes(SampleIndices.PAPER_BOOK_INDEX_TYPE).execute().actionGet()) { paperBooksSearchResult ->
            paperBooksSearchResult.hits.hits.size() == 1
            assertJsonsEquals(SampleIndices.toJson(SampleIndices.SHINING), paperBooksSearchResult.hits.hits[0].sourceAsString)
        }

        and:
        with(client.prepareSearch(SampleIndices.BOOKS_INDEX_NAME).setTypes(SampleIndices.AUDIO_BOOK_INDEX_TYPE).execute().actionGet()) { audioBooksSearchResult ->
            audioBooksSearchResult.hits.hits.size() == 1
            assertJsonsEquals(SampleIndices.toJson(SampleIndices.AMERICAN_PSYCHO), audioBooksSearchResult.hits.hits[0].sourceAsString)
        }
    }

    static Client createClient() {
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", CLUSTER_NAME_VALUE).build();
        return new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), TRANSPORT_TCP_PORT_VALUE));
    }

    void assertJsonsEquals(String expectedJson, String actualJson) {
        JSONAssert.assertEquals(expectedJson, actualJson, false)
    }

    void index(Car car) {
        embeddedElastic.index(SampleIndices.CARS_INDEX_NAME, SampleIndices.CAR_INDEX_TYPE, SampleIndices.toJson(car))
    }

    void index(PaperBook book) {
        embeddedElastic.index(SampleIndices.BOOKS_INDEX_NAME, SampleIndices.PAPER_BOOK_INDEX_TYPE, SampleIndices.toJson(book))
    }

    void index(AudioBook book) {
        embeddedElastic.index(SampleIndices.BOOKS_INDEX_NAME, SampleIndices.AUDIO_BOOK_INDEX_TYPE, SampleIndices.toJson(book))
    }

}
