package pl.allegro.tech.embeddedelasticsearch;

import org.apache.http.client.methods.HttpHead;

import static pl.allegro.tech.embeddedelasticsearch.HttpStatusCodes.OK;

public class TestHelper {

    public static boolean existsIndex(String index, Integer port) {
        final HttpClient httpClient = new HttpClient();
        HttpHead request = new HttpHead("http://localhost:" + port + "/" + index);
        return httpClient.execute(request, response -> response.getStatusLine().getStatusCode() == OK);
    }
}
