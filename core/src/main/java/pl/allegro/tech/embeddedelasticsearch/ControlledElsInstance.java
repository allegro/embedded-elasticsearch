package pl.allegro.tech.embeddedelasticsearch;

import java.io.IOException;

class ControlledElsInstance implements ElsInstance {

    private final InstallationDescription installationDescription;

    private String connectionUrl;
    private ElasticServer elasticServer;

    ControlledElsInstance(InstallationDescription installationDescription) {
        this.installationDescription = installationDescription;
    }

    @Override
    public void start() {
        try {
            installElastic();
            startElastic();
            connectionUrl = "http://localhost:" + elasticServer.getHttpPort();
        } catch (InterruptedException | IOException e) {
            throw new EmbeddedElasticsearchException("Error during embedded-elasticsearch setup", e);
        }
    }

    private void installElastic() throws IOException, InterruptedException {
        ElasticSearchInstaller elasticSearchInstaller = new ElasticSearchInstaller(installationDescription);
        elasticSearchInstaller.install();
        elasticServer = elasticSearchInstaller.createElasticServerInstance();
    }

    private void startElastic() throws IOException, InterruptedException {
        if (!elasticServer.isStarted()) {
            elasticServer.start();
        }
    }

    @Override
    public void stop() {
        elasticServer.stop();
    }

    @Override
    public ElasticRestClient createRestClient() {
        return new ElasticRestClient(connectionUrl);
    }

    @Override
    public int getTransportTcpPort() {
        return elasticServer.getTransportTcpPort();
    }

    @Override
    public int getHttpPort() {
        return elasticServer.getHttpPort();
    }
}
