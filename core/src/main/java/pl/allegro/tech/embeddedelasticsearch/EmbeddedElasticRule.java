package pl.allegro.tech.embeddedelasticsearch;


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EmbeddedElasticRule implements TestRule {

    private static final Integer DEFAULT_PORT = 9200;

    public EmbeddedElastic embeddedElastic;
    private Integer port = 0;
    private String index;

    public EmbeddedElasticRule(String index, Integer port) {
        this.port = port;
        this.index = index;
    }

    public EmbeddedElasticRule(EmbeddedElastic builder) {
        this.embeddedElastic = builder;
    }

    @Override
    public Statement apply(final Statement base,
                           final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                validate();
                startElastic();

                try {
                    base.evaluate();
                } finally {
                    embeddedElastic.stop();
                }
            }
        };
    }

    private void startElastic() throws IOException, InterruptedException {
        if (embeddedElastic != null) {
            embeddedElastic.start();
        } else {
            embeddedElastic = EmbeddedElastic.builder()
                    .withElasticVersion("6.8.0")
                    .withSetting(PopularProperties.HTTP_PORT, port != 0 ? port : DEFAULT_PORT)
                    .withSetting(PopularProperties.CLUSTER_NAME, "test_cluster")
                    .withSetting("cluster.routing.allocation.disk.threshold_enabled", false)
                    .withStartTimeout(2, TimeUnit.MINUTES)
                    .withIndex(index)
                    .withDownloadDirectory(new File("./"))
                    .withInstallationDirectory(new File("./"))
                    .build();
            embeddedElastic.start();
        }
    }

    private void validate() {
        if (embeddedElastic != null) return;
        if (index == null || index.replace(" ", "").isEmpty()) {
            throw new IllegalArgumentException("Index can not be null");
        }
    }
}
