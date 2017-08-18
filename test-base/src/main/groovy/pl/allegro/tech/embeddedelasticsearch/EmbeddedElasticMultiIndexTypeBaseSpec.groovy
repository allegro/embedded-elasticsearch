package pl.allegro.tech.embeddedelasticsearch

import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.AMERICAN_PSYCHO
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.AUDIO_BOOK_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.BOOKS_INDEX_NAME
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.PAPER_BOOK_INDEX_TYPE
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.SHINING
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.toJson

abstract class EmbeddedElasticMultiIndexTypeBaseSpec extends EmbeddedElasticCoreApiBaseSpec {

    def "should place document under right index and type"() {
        when:
        index(SHINING)
        index(AMERICAN_PSYCHO)

        then:
        with(fetchAllDocuments(BOOKS_INDEX_NAME, PAPER_BOOK_INDEX_TYPE)) { List<String> it ->
            it.size() == 1
            assertJsonsEquals(toJson(SHINING), it[0])
        }

        and:
        with(fetchAllDocuments(BOOKS_INDEX_NAME, AUDIO_BOOK_INDEX_TYPE)) { List<String> it ->
            it.size() == 1
            assertJsonsEquals(toJson(AMERICAN_PSYCHO), it[0])
        }
    }
}