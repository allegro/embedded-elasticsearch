package pl.allegro.tech.embeddedelasticsearch;


public class TypeWithMapping {
    
    private final String type;
    private final String mapping;

    public TypeWithMapping(String type, String mapping) {
        this.type = type;
        this.mapping = mapping;
    }

    public String getType() {
        return type;
    }

    public String getMapping() {
        return mapping;
    }
}
