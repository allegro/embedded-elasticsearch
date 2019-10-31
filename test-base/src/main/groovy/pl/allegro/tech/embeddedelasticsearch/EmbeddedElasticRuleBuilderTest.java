package pl.allegro.tech.embeddedelasticsearch;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;

public class EmbeddedElasticRuleBuilderTest {

    @ClassRule
    public static EmbeddedElasticRule embeddedElasticRule = new EmbeddedElasticRule(EmbeddedElastic.builder()
            .withElasticVersion("6.8.0")
            .withSetting(PopularProperties.HTTP_PORT, 9200)
            .withSetting(PopularProperties.CLUSTER_NAME, "cluster_test")
            .withSetting("cluster.routing.allocation.disk.threshold_enabled", false)
            .withStartTimeout(2, TimeUnit.MINUTES)
            .withIndex("test_builder")
            .withDownloadDirectory(new File("./"))
            .withInstallationDirectory(new File("./"))
            .build());

    @Test
    public void shouldStartElastic() {
        assertTrue(embeddedElasticRule.embeddedElastic.isStarted());
    }

    @Test
    public void shouldCreateIndex() {
        Boolean createdIndex = TestHelper.existsIndex("test_builder", 9200);
        assertTrue(createdIndex);
    }
}
