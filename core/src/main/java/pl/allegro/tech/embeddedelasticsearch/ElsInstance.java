package pl.allegro.tech.embeddedelasticsearch;

public interface ElsInstance {
    void start();
    void stop();
    ElasticRestClient createRestClient();
    int getTransportTcpPort();
    int getHttpPort();
}
