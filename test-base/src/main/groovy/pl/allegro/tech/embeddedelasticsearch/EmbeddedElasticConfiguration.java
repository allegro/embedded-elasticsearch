package pl.allegro.tech.embeddedelasticsearch;

/**
 * Common test configuration.
 */
public interface EmbeddedElasticConfiguration {
    /**
     * Default start timeout for test configurations.
     */
    int START_TIMEOUT_IN_MINUTES = 1;
    /**
     * Default Java options for test elasticsearch instances
     */
    String TEST_ES_JAVA_OPTS = "-Xms128m -Xmx512m";
}
