package pl.allegro.tech.embeddedelasticsearch;

class InstanceDescription {
    private final int port;
    private final String clusterName;

    InstanceDescription(int port, String clusterName) {
        this.port = port;
        this.clusterName = clusterName;
    }

    int getPort() {
        return port;
    }

    String getClusterName() {
        return clusterName;
    }
}
