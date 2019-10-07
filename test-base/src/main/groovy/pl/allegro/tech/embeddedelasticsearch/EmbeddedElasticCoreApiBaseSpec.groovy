package pl.allegro.tech.embeddedelasticsearch

import groovy.json.JsonOutput
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.AMERICAN_PSYCHO
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.AUDIO_BOOK_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOKS_INDEX_NAME
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOK_ALIAS_1
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOK_ALIAS_2
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CARS_INDEX_NAME
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CAR_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.CUJO
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.FIAT_126p
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.MISERY
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.PAPER_BOOK_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.SHINING
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.toJson

abstract class EmbeddedElasticCoreApiBaseSpec extends Specification {

    def "should index document"() {
        when:
        index(FIAT_126p)

        then:
        final result = searchByTerm(CARS_INDEX_NAME, CAR_INDEX_TYPE, "model", FIAT_126p.model)
        result.size() == 1
        assertJsonsEquals(toJson(FIAT_126p), result[0])
    }

    def "should index multiple documents without explicit id"() {
        when:
        index(CUJO)
        index(MISERY)
        index(SHINING)

        then:
        final result = fetchAllDocuments(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE)
        result.size() == 3
    }

    def "should index document with id"() {
        given:
        final id = "some-id"
        final document = toJson(FIAT_126p)

        when:
        index(CARS_INDEX_NAME, CAR_INDEX_TYPE, ["$id": document])

        then:
        final result = getById(CARS_INDEX_NAME, CAR_INDEX_TYPE, id)
        result != null
        assertJsonsEquals(document, result)
    }

    def "should recreate only specified index"() {
        given: "indices with some documents"
        index(FIAT_126p)
        index(SHINING)
        index(AMERICAN_PSYCHO)

        when: "recreating index"
        embeddedElastic.recreateIndex(BOOKS_INDEX_NAME)

        then: "recreated index should be empty"
        fetchAllDocuments(BOOKS_INDEX_NAME).size() == 0

        and: "other index should be untouched"
        fetchAllDocuments(CARS_INDEX_NAME).size() == 1
    }

    def "should recreate all indices"() {
        given: "indices with some documents"
        index(FIAT_126p)
        index(SHINING)
        index(AMERICAN_PSYCHO)

        when: "recreating index"
        embeddedElastic.recreateIndices()

        then: "recreated indices should be empty"
        fetchAllDocuments().size() == 0
    }

    def "should fetch books from aliases"() {
        given:
        index(SHINING)
        index(CUJO)

        when:
        final result1 = fetchAllDocuments(BOOK_ALIAS_1)

        and:
        final result2 = fetchAllDocuments(BOOK_ALIAS_2)

        then:
        result1.size() == 2
        result2.size() == 2
    }

    def "should index document in a bulk"() {
        given:
        index(new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(SHINING))
                .withId("id1")
                .build()
        )

        when:
        final result = fetchAllDocuments()

        then:
        result.size() == 1
        assertJsonsEquals(toJson(SHINING), result[0])
    }

    def "should index documents in a bulk"() {
        given:
        index([
                new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(SHINING))
                        .withId("id1").build(),
                new IndexRequest.IndexRequestBuilder(CARS_INDEX_NAME, CAR_INDEX_TYPE, toJson(FIAT_126p))
                        .withId("id2").build()
        ])

        when:
        final result = fetchAllDocuments()

        then:
        result.size() == 2
    }

    def "should index a document using specified routing"() {
        given:
        index([
                new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(SHINING))
                        .withId("id1").withRouting("bookShard1").build(),
                new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(CUJO))
                        .withId("id2").withRouting("bookShard2").build(),
        ])

        when:
        final result1 = fetchAllDocuments(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, "bookShard1")

        and:
        final result2 = fetchAllDocuments(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, "bookShard2")

        and:
        final resultAll = fetchAllDocuments(BOOKS_INDEX_NAME)

        then:
        result1.size() == 1
        assertJsonsEquals(toJson(SHINING), result1[0])
        result2.size() == 1
        assertJsonsEquals(toJson(CUJO), result2[0])
        resultAll.size() == 2
    }

    void assertJsonsEquals(String expectedJson, String actualJson) {
        JSONAssert.assertEquals(expectedJson, actualJson, false)
    }

    void index(SampleIndices.Car car) {
        index(new IndexRequest.IndexRequestBuilder(CARS_INDEX_NAME, CAR_INDEX_TYPE, toJson(car)).build())
    }

    void index(String indexName, String indexType, Map idJsonMap) {
        embeddedElastic.index(indexName, indexType, idJsonMap)
    }

    void index(SampleIndices.PaperBook book) {
        index(new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE, toJson(book)).build())
    }

    void index(SampleIndices.AudioBook book) {
        index(new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, AUDIO_BOOK_INDEX_TYPE, toJson(book)).build())
    }

    void index(IndexRequest indexRequest) {
        index(Arrays.asList(indexRequest))
    }

    void index(List<IndexRequest> indexRequests) {
        embeddedElastic.index(indexRequests)
    }

    abstract EmbeddedElastic getEmbeddedElastic()

    abstract List<String> fetchAllDocuments()

    abstract List<String> fetchAllDocuments(String indexName)

    abstract List<String> fetchAllDocuments(String indexName, String typeName)

    abstract List<String> fetchAllDocuments(String indexName, String typeName, String routing)

    abstract List<String> searchByTerm(String indexName, String typeName, String fieldName, String value)

    abstract String getById(String indexName, String indexType, String id)
}