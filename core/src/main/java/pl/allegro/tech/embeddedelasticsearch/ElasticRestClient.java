package pl.allegro.tech.embeddedelasticsearch;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static pl.allegro.tech.embeddedelasticsearch.HttpStatusCodes.OK;

class ElasticRestClient {

    private static final Logger logger = LoggerFactory.getLogger(ElasticRestClient.class);

    private int elasticsearchHttpPort;
    private final HttpClient httpClient;
    private final IndicesDescription indicesDescription;

    ElasticRestClient(int elasticsearchHttpPort, HttpClient httpClient, IndicesDescription indicesDescription) {
        this.elasticsearchHttpPort = elasticsearchHttpPort;
        this.httpClient = httpClient;
        this.indicesDescription = indicesDescription;
    }

    void createIndices() {
        indicesDescription.getIndicesNames().forEach(this::createIndex);
    }

    void createIndex(String indexName) {
        if (!indexExists(indexName)) {
            HttpPut request = new HttpPut(url("/" + indexName));
            request.setEntity(new StringEntity(indicesDescription.getIndexSettings(indexName).toJson().toString(), APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                String responseBody = readBodySafely(response);
                throw new RuntimeException("Call to elasticsearch resulted in error:\n" + responseBody);
            }
            waitForClusterYellow();
        }
    }

    private boolean indexExists(String indexName) {
        HttpHead request = new HttpHead(url("/" + indexName));
        CloseableHttpResponse response = httpClient.execute(request);
        return response.getStatusLine().getStatusCode() == OK;
    }

    private void waitForClusterYellow() {
        HttpGet request = new HttpGet(url("/_cluster/health?wait_for_status=yellow&timeout=60s"));
        CloseableHttpResponse response = httpClient.execute(request);
        assertOk(response, "Cluster does not reached yellow status in specified timeout");
    }

    void deleteIndices() {
        indicesDescription.getIndicesNames().forEach(this::deleteIndex);
    }

    void deleteIndex(String indexName) {
        if (indexExists(indexName)) {
            HttpDelete request = new HttpDelete(url("/" + indexName));
            assertOk(httpClient.execute(request), "Delete request resulted in error");
            waitForClusterYellow();
        } else {
            logger.warn("Index: {} does not exists so cannot be removed", indexName);
        }
    }

    void indexWithIds(String indexName, String indexType, Collection<DocumentWithId> idJsonMap) {
        String bulkRequestBody = idJsonMap.stream()
                .flatMap(json -> Stream.of(indexMetadataJsonWithId(json.getId()), json.getDocument()))
                .map((jsonNodes) -> jsonNodes.replace('\n', ' ').replace('\r', ' '))
                .collect(joining("\n")) + "\n";

        HttpPost request = new HttpPost(url("/" + indexName + "/" + indexType + "/_bulk"));
        request.setEntity(new StringEntity(bulkRequestBody, UTF_8));
        CloseableHttpResponse response = httpClient.execute(request);
        assertOk(response, "Request finished with error");
        refresh();
    }

    private String indexMetadataJsonWithId(String id) {
        return "{ \"index\": { \"_id\": \"" + id + "\"} }";
    }

    void refresh() {
        HttpPost request = new HttpPost(url("/_refresh"));
        httpClient.execute(request);
    }

    private String url(String path) {
        return "http://localhost:" + elasticsearchHttpPort + path;
    }

    private void assertOk(CloseableHttpResponse response, String message) {
        if (response.getStatusLine().getStatusCode() != OK) {
            throw new IllegalStateException(message + "\nResponse body:\n" + readBodySafely(response));
        }
    }

    private String readBodySafely(CloseableHttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), UTF_8);
        } catch (IOException e) {
            logger.error("Error during reading response body", e);
            return "";
        }
    }

    List<String> fetchAllDocuments(String... indices) {
        if (indices.length == 0) {
            return searchForDocuments(Optional.empty()).collect(toList());
        } else {
            return Stream.of(indices)
                    .flatMap((index) -> searchForDocuments(Optional.of(index)))
                    .collect(toList());
        }
    }

    private Stream<String> searchForDocuments(Optional<String> indexMaybe) {
        String searchCommand = indexMaybe
                .map(index -> "/" + index + "/_search")
                .orElse("/_search");
        HttpGet request = new HttpGet(url(searchCommand));

        CloseableHttpResponse response = httpClient.execute(request);
        assertOk(response, "Error during search (" + searchCommand + ")");
        String body = readBodySafely(response);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            return StreamSupport.stream(jsonNode.get("hits").get("hits").spliterator(), false)
                    .map(hitNode -> hitNode.get("_source"))
                    .map(JsonNode::toString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
