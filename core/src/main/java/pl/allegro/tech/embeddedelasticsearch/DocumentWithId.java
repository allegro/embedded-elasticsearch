package pl.allegro.tech.embeddedelasticsearch;

class DocumentWithId {

    private final String id;
    private final String document;
    private final String routing;

    DocumentWithId(String id, String document) {
        this.id = id;
        this.document = document;
        this.routing = null;
    }

    DocumentWithId(String id, String document, String routing) {
        this.id = id;
        this.document = document;
        this.routing = routing;
    }

    String getId() {
        return id;
    }

    String getDocument() {
        return document;
    }

    public String getRouting() {
        return routing;
    }
}
