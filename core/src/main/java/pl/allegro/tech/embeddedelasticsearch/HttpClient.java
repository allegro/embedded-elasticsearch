package pl.allegro.tech.embeddedelasticsearch;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

class HttpClient {

    private final CloseableHttpClient internalHttpClient = HttpClients.createDefault();
    
    CloseableHttpResponse execute(HttpRequestBase request) {
        try {
            CloseableHttpResponse execute = internalHttpClient.execute(request);
            request.releaseConnection();
            return execute;
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
