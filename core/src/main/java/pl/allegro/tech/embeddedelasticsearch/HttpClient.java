package pl.allegro.tech.embeddedelasticsearch;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

class HttpClient {

    private final CloseableHttpClient internalHttpClient = HttpClients.createDefault();

    CloseableHttpResponse execute(HttpRequestBase request) {
        try {
            return internalHttpClient.execute(request);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    static class HttpRequestException extends RuntimeException {
        HttpRequestException(IOException cause) {
            super(cause);
        }
    }
}
