package pl.allegro.tech.embeddedelasticsearch

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Shared
import spock.lang.Specification

import static PopularProperties.CLUSTER_NAME
import static PopularProperties.TRANSPORT_TCP_PORT
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.*

class EmbeddedElasticSpec extends Specification {
    
    static final ELASTIC_VERSION = "5.0.0"
    static final TRANSPORT_TCP_PORT_VALUE = 9930
    static final CLUSTER_NAME_VALUE = "myTestCluster"
    
    @Shared
    EmbeddedElastic embeddedElastic
    
    def setupSpec() {
        embeddedElastic = EmbeddedElastic.builder()
                .withElasticVersion(ELASTIC_VERSION)
                .withSetting(TRANSPORT_TCP_PORT ,TRANSPORT_TCP_PORT_VALUE)
                .withSetting(CLUSTER_NAME, CLUSTER_NAME_VALUE)
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
    
    Client createClient() {
        Settings settings = Settings.builder().put("cluster.name", CLUSTER_NAME_VALUE).build();
        return new PreBuiltTransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), TRANSPORT_TCP_PORT_VALUE));
    }
    
    void assertJsonsEquals(String expectedJson, String actualJson) {
        JSONAssert.assertEquals(expectedJson, actualJson, false)
    }
    
    void index(Car car) {
        embeddedElastic.index(CARS_INDEX_NAME, CAR_INDEX_TYPE, toJson(car))
    }

    void index(PaperBook book) {
        embeddedElastic.index(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(book))
    }

    void index(AudioBook book) {
        embeddedElastic.index(BOOKS_INDEX_NAME, AUDIO_BOOK_INDEX_TYPE, toJson(book))
    }
    
}
