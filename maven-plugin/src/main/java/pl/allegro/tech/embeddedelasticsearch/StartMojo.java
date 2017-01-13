package pl.allegro.tech.embeddedelasticsearch;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartMojo extends AbstractMojo {

    @Parameter(property = "embedded-elasticsearch.version")
    private String version;
    @Parameter(property = "embedded-elasticsearch.downloadUrl")
    private URL downloadUrl;
    @Parameter(property = "embedded-elasticsearch.plugins")
    private List<String> plugins = Collections.emptyList();
    @Parameter(property = "embedded-elasticsearch.esJavaOpts")
    private String esJavaOpts;
    @Parameter(property = "embedded-elasticsearch.startTimeoutInMs")
    private Long startTimeoutInMs;
    @Parameter(property = "embedded-elasticsearch.settings")
    private Map<String, String> settings = Collections.emptyMap();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        EmbeddedElastic embeddedElastic = prepareInstance();
        getLog().info("Starting embedded elasticsearch instance...");
        embeddedElastic.start();
        getLog().info("Embedded-elasticsearch started");
    }

    private EmbeddedElastic prepareInstance() {
        EmbeddedElastic.Builder builder = EmbeddedElastic.builder();
        Optional.ofNullable(version).ifPresent(builder::withElasticVersion);
        Optional.ofNullable(downloadUrl).ifPresent(builder::withDownloadUrl);
        plugins.forEach(builder::withPlugin);
        Optional.ofNullable(esJavaOpts).ifPresent(builder::withEsJavaOpts);
        Optional.ofNullable(startTimeoutInMs).ifPresent(startTimeoutInMs -> builder.withStartTimeout(startTimeoutInMs, MILLISECONDS));
        settings.forEach(builder::withSetting);
        return builder.build();
    }

}
