package pl.allegro.tech.embeddedelasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IndexSettings {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<TypeWithMapping> types;
    private final Optional<JsonNode> settings;

    public static Builder builder() {
        return new Builder();
    }

    public IndexSettings(List<TypeWithMapping> types, Optional<String> settings) {
        this.types = types;
        this.settings = settings.map(rawSettingsJson -> {
            try {
                return OBJECT_MAPPER.readTree(rawSettingsJson);
            } catch (IOException e) {
                throw new RuntimeException("Problem with provided settings for index", e);
            }
        });
    }

    public static class Builder {

        private final List<TypeWithMapping> types = new ArrayList<>();
        private Optional<String> settings = Optional.empty();

        /**
         * Specify type inside created index
         *
         * @param type    name of type
         * @param mapping mapping for created type
         */
        public Builder withType(String type, InputStream mapping) throws IOException {
            return withType(type, IOUtils.toString(mapping, UTF_8));
        }

        /**
         * Type with mappings to create with index
         *
         * @param type    name of type
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
            return withSettings(IOUtils.toString(settings, UTF_8));
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

    public ObjectNode toJson() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.set("settings", settings.orElse(OBJECT_MAPPER.createObjectNode()));
        ObjectNode mappingsObject = prepareMappingsObject();
        objectNode.set("mappings", mappingsObject);
        return objectNode;
    }

    private ObjectNode prepareMappingsObject() {
        ObjectNode mappingsObject = OBJECT_MAPPER.createObjectNode();
        types.forEach(type -> mappingsObject.set(type.getType(), type.getMapping()));
        return mappingsObject;
    }
}
