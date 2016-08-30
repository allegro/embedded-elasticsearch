package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IndexSettings {

    private final List<TypeWithMapping> types;
    private final Optional<String> settings;
    
    public static Builder builder() {
        return new Builder();
    }

    public IndexSettings(List<TypeWithMapping> types, Optional<String> settings) {
        this.types = types;
        this.settings = settings;
    }

    public List<TypeWithMapping> getTypes() {
        return types;
    }

    public Optional<String> getSettings() {
        return settings;
    }

    public static class Builder {

        private final List<TypeWithMapping> types = new ArrayList<>();
        private Optional<String> settings = Optional.empty();

        /**
         * Specify type inside created index
         * 
         * @param type name of type
         * @param mapping mapping for created type
         */
        public Builder withType(String type, InputStream mapping) throws IOException {
            return withType(type, IOUtils.toString(mapping));
        }

        /**
         * Type with mappings to create with index
         *
         * @param type name of type
         * @param mapping mapping for created type
         */
        public Builder withType(String type, String mapping) {
            types.add(new TypeWithMapping(type, mapping));
            return this;
        }

        /**
         * The settings to create the index with
         * 
         * @param settings settings in json/yaml/properties format
         */
        public Builder withSettings(InputStream settings) throws IOException {
            return withSettings(IOUtils.toString(settings));
        }

        /**
         * The settings to create the index with
         *
         * @param settings settings in json/yaml/properties format
         */
        public Builder withSettings(String settings) {
            this.settings = Optional.of(settings);
            return this;
        }

        /**
         * @return IndexSettings with specified parameters
         */
        public IndexSettings build() {
            return new IndexSettings(types, settings);
        }
    }
    
}
