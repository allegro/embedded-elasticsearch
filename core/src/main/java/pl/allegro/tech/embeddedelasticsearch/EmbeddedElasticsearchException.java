package pl.allegro.tech.embeddedelasticsearch;

public class EmbeddedElasticsearchException extends RuntimeException {
    public EmbeddedElasticsearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
