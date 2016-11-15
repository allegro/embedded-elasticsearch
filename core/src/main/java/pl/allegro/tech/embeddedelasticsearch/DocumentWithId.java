package pl.allegro.tech.embeddedelasticsearch;

class DocumentWithId {

    private final String id;
    private final String document;

    DocumentWithId(String id, String document) {
        this.id = id;
        this.document = document;
    }

    String getId() {
        return id;
    }

    String getDocument() {
        return document;
    }
}
