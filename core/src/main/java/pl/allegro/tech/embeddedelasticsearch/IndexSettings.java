package pl.allegro.tech.embeddedelasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IndexSettings {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Optional<JsonNode> mappings;
    private final List<TypeWithMapping> types;
    private final Optional<JsonNode> settings;
    private final Optional<JsonNode> aliases;
    private boolean includeTypeName=false;
    public static Builder builder() {
        return new Builder();
    }

    private IndexSettings(List<TypeWithMapping> types, Optional<String> settings, Optional<String> aliases) {
        this.mappings = rawToJson(Optional.of("{}"));
        this.types = types;
        this.settings = rawToJson(settings);
        this.aliases = Optional.empty();
    }

    public IndexSettings(Optional<String>  mapping, Optional<String> settings) {
        includeTypeName = true;
        this.mappings = rawToJson(mapping);
        this.settings = rawToJson(settings);
        this.aliases = Optional.empty();
        this.types = new ArrayList<>();
    }

    private IndexSettings(Optional<String> mapping, Optional<String> settings, Optional<String> aliases) {
        this.mappings = rawToJson(mapping);
        includeTypeName = true;
        this.settings = rawToJson(settings);
        this.aliases = rawToJson(aliases);
        this.types = new ArrayList<>();
    }

    public static class Builder {

        private Optional<String> mapping  = Optional.empty();
        private Optional<String> settings = Optional.empty();
        private Optional<String> aliases = Optional.empty();
        private final List<TypeWithMapping> types = new ArrayList<>();

        /**
         * Type with mappings to create with index
         *
         * @param mapping mappings for created type
         */
        public Builder withMapping(Object mapping) throws IOException {
            String mappingString;
            if (mapping == null) {
                return this;
            }
            else if (mapping instanceof InputStream) {
                InputStream mappingStream = (InputStream) mapping;
                mappingString = IOUtils.toString(mappingStream, UTF_8);
            }
            else {
                mappingString = (String) mapping;
            }
            this.mapping = Optional.of(mappingString);
            return this;
        }

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
         * The aliases to create the index with
         *
         * @param aliases aliases in json/yaml/properties format
         */
        public Builder withAliases(InputStream aliases) throws IOException {
            return withAliases(IOUtils.toString(aliases, UTF_8));
        }

        /**
         * The aliases to create the index with
         *
         * @param aliases aliases in json/yaml/properties format
         */
        public Builder withAliases(String aliases) {
            this.aliases = Optional.of(aliases);
            return this;
        }

        /**
         * @return IndexSettings with specified parameters
         */
        public IndexSettings build() {
            return new IndexSettings(mapping, settings, aliases);
        }
    }

    public ObjectNode toJson() {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.set("settings", settings.orElse(OBJECT_MAPPER.createObjectNode()));
        objectNode.set("aliases", aliases.orElse(OBJECT_MAPPER.createObjectNode()));

        if (includeTypeName){
            objectNode.set("mappings",mappings.orElse(OBJECT_MAPPER.createObjectNode()));
        }
        else {
            ObjectNode mappingsObject = prepareMappingsObject();
            objectNode.set("mappings", mappingsObject);
        }

        return objectNode;
    }

    private ObjectNode prepareMappingsObject() {
        ObjectNode mappingsObject = OBJECT_MAPPER.createObjectNode();
        types.forEach(type -> mappingsObject.set(type.getType(), type.getMapping()));
        return mappingsObject;
    }

    private Optional<JsonNode> rawToJson(Optional<String> rawJson) {
        return rawJson.map(json -> {
            try {
                return OBJECT_MAPPER.readTree(json);
            } catch (IOException e) {
                throw new RuntimeException("Problem with provided settings for index", e);
            }
        });
    }
}
