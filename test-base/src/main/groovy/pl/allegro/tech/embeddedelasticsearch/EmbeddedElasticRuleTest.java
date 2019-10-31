package pl.allegro.tech.embeddedelasticsearch;

import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class EmbeddedElasticRuleTest {

    @ClassRule
    public static EmbeddedElasticRule embeddedElasticRule = new EmbeddedElasticRule("index_name", 9200);

    @Test
    public void shouldStartElastic() {
        assertTrue(embeddedElasticRule.embeddedElastic.isStarted());
    }

    @Test
    public void shouldCreateIndex() {
        Boolean createdIndex = TestHelper.existsIndex("index_name", 9200);
        assertTrue(createdIndex);
    }

}
