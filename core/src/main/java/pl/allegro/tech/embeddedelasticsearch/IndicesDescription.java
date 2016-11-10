package pl.allegro.tech.embeddedelasticsearch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class IndicesDescription {

    private final Map<String, IndexSettings> indices;

    IndicesDescription(Map<String, IndexSettings> indices) {
        this.indices = indices;
    }
    
    Collection<String> getIndicesNames() {
        return indices.keySet();
    }

    IndexSettings getIndexSettings(String indexName) {
        return indices.get(indexName);
    }
}
