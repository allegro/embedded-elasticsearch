package pl.allegro.tech.embeddedelasticsearch;

class ExistingElsInstance implements ElsInstance {
    private final String connectionUrl;

    ExistingElsInstance(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public ElasticRestClient createRestClient() {
        return new ElasticRestClient(connectionUrl);
    }

    @Override
    public int getTransportTcpPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHttpPort() {
        throw new UnsupportedOperationException();
    }
}
