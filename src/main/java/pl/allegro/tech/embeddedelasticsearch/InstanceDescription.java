package pl.allegro.tech.embeddedelasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

class InstanceDescription {
    private Map<String, Object> settings;

    InstanceDescription(Map<String, Object> settings) {
        this.settings = settings;
    }

    int getPort() {
        return (int) this.settings.get("transport.tcp.port");
    }

    String getClusterName() {
        return (String) this.settings.get("cluster.name");
    }

    Collection<String> asParams() {
        Collection<String> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : this.settings.entrySet()) {
            params.add("--" + entry.getKey());
            params.add(entry.getValue().toString());
        }
        return params;
    }
}
