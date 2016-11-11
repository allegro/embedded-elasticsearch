package pl.allegro.tech.embeddedelasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class InstanceSettings {

    public static final String TRANSPORT_TCP_PORT = "transport.tcp.port";
    public static final String CLUSTER_NAME = "cluster.name";


    private static final int DEFAULT_TRANSPORT_TCP_PORT = 9300;
    private static final String DEFAULT_CLUSTER_NAME = "elasticsearch";


    private final Map<String, Object> settings = new HashMap<>();

    InstanceSettings(Map<String, Object> settings) {
        this.settings.put(TRANSPORT_TCP_PORT, DEFAULT_TRANSPORT_TCP_PORT);
        this.settings.put(CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
        this.settings.putAll(settings);
    }

    int getPort() {
        return (int) settings.get(TRANSPORT_TCP_PORT);
    }

    String getClusterName() {
        return (String) settings.get(CLUSTER_NAME);
    }

    Collection<String> asCommandLineParams() {
        Collection<String> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : this.settings.entrySet()) {
            params.add("--" + entry.getKey());
            params.add(entry.getValue().toString());
        }
        return params;
    }
}
