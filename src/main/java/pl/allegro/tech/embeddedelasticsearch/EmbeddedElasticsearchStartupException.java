package pl.allegro.tech.embeddedelasticsearch;

class EmbeddedElasticsearchStartupException extends RuntimeException {

    EmbeddedElasticsearchStartupException(String message) {
        super(message);
    }

    EmbeddedElasticsearchStartupException(Throwable cause) {
        super(cause);
    }

    EmbeddedElasticsearchStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
