package pl.allegro.tech.embeddedelasticsearch;

import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class MavenPluginTest {

    private static final EmbeddedElastic EMBEDDED_ELASTIC;

    static {
        try {
            int httpPort = Integer.parseInt(System.getProperty("elsHttpPort"));
            EMBEDDED_ELASTIC = EmbeddedElastic.builder()
                    .useExistingInstance("http://localhost:" + httpPort)
                    .withIndex("maven-plugin-test", IndexSettings.builder()
                            .withType("my-type", Thread.currentThread().getContextClassLoader().getResourceAsStream("my-type-mapping.json"))
                            .build())
                    .build()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void should_use_existing_els_instance() throws UnknownHostException {
        EMBEDDED_ELASTIC.index("maven-plugin-test", "my-type", "{ \"foo\": \"some text\", \"bar\": 10 }");
        List<String> documents = EMBEDDED_ELASTIC.fetchAllDocuments("maven-plugin-test");
        assertTrue(documents.size() == 1);
    }

}
