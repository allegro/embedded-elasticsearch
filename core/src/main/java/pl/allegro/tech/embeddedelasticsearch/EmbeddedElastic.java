package pl.allegro.tech.embeddedelasticsearch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import pl.allegro.tech.embeddedelasticsearch.InstallationDescription.Plugin;

import static java.util.stream.Collectors.toList;

public final class EmbeddedElastic {

    private final String esJavaOpts;
    private final InstanceSettings instanceSettings;
    private final IndicesDescription indicesDescription;
    private final InstallationDescription installationDescription;
    private final long startTimeoutInMs;

    private ElasticServer elasticServer;
    private ElasticRestClient elasticRestClient;

    public static Builder builder() {
        return new Builder();
    }

    private EmbeddedElastic(String esJavaOpts, InstanceSettings instanceSettings, IndicesDescription indicesDescription, InstallationDescription installationDescription, long startTimeoutInMs) {
        this.esJavaOpts = esJavaOpts;
        this.instanceSettings = instanceSettings;
        this.indicesDescription = indicesDescription;
        this.installationDescription = installationDescription;
        this.startTimeoutInMs = startTimeoutInMs;
    }

    /**
     * Downloads Elasticsearch with specified plugins, setups them and starts
     */
    public EmbeddedElastic start() throws IOException, InterruptedException {
        installElastic();
        startElastic();
        createRestClient();
        createIndices();
        return this;
    }

    private void installElastic() throws IOException, InterruptedException {
        ElasticSearchInstaller elasticSearchInstaller = new ElasticSearchInstaller(instanceSettings, installationDescription);
        elasticSearchInstaller.install();
        File executableFile = elasticSearchInstaller.getExecutableFile();
        File installationDirectory = elasticSearchInstaller.getInstallationDirectory();
        elasticServer = new ElasticServer(esJavaOpts, installationDirectory, executableFile, startTimeoutInMs);
    }

    private void startElastic() throws IOException, InterruptedException {
        if (!elasticServer.isStarted()) {
            elasticServer.start();
        }
    }

    private void createRestClient() throws UnknownHostException {
        elasticRestClient = new ElasticRestClient(elasticServer.getHttpPort(), new HttpClient(), indicesDescription);
    }

    /**
     * Stops Elasticsearch instance and removes data
     */
    public void stop() {
        elasticServer.stop();
    }

    /**
     * Index documents
     * @param indexName target index
     * @param indexType target index type
     * @param idJsonMap map where keys are documents ids and values are documents represented as JSON
     */
    public void index(String indexName, String indexType, Map<CharSequence, CharSequence> idJsonMap) {
        elasticRestClient.indexWithIds(indexName, indexType, idJsonMap.entrySet().stream()
                .map(entry -> new DocumentWithId(entry.getKey().toString(), entry.getValue().toString()))
                .collect(toList()));
    }

    /**
     * Index documents
     * @param indexName target index
     * @param indexType target index name
     * @param json document represented as JSON
     */
    public void index(String indexName, String indexType, String... json) {
        index(indexName, indexType, Arrays.asList(json));
    }

    /**
     * Index documents
     * @param indexName target index
     * @param indexType target index name
     * @param jsons documents represented as JSON
     */
    public void index(String indexName, String indexType, List<CharSequence> jsons) {
        elasticRestClient.indexWithIds(indexName, indexType, jsons.stream().map(json -> new DocumentWithId(null, json.toString())).collect(Collectors.toList()));
    }

    /**
     * Recreates all instances (i.e. deletes and creates them again)
     */
    public void recreateIndices() {
        deleteIndices();
        createIndices();
    }

    /**
     * Recreates specified index (i.e. deletes and creates it again)
     * @param indexName index to recreate
     */
    public void recreateIndex(String indexName) {
        deleteIndex(indexName);
        createIndex(indexName);
    }

    /**
     * Delete all indices
     */
    public void deleteIndices() {
        elasticRestClient.deleteIndices();
    }

    /**
     * Delete specified index
     * @param indexName index do delete
     */
    public void deleteIndex(String indexName) {
        elasticRestClient.deleteIndex(indexName);
    }

    /**
     * Create all indices
     */
    public void createIndices() {
        elasticRestClient.createIndices();
    }

    /**
     * Create specified index. Note that you can specify only index from list of indices specified during EmbeddedElastic creation
     * @param indexName index to create
     */
    public void createIndex(String indexName) {
        elasticRestClient.createIndex(indexName);
    }

    /**
     * Refresh indices. Can be useful in tests that uses multiple threads
     */
    public void refreshIndices() {
        elasticRestClient.refresh();
    }

    /**
     * Fetch all documents from specified indices. Useful for logging and debugging
     * @return list containing documents sources represented as JSON
     */
    public List<String> fetchAllDocuments(String... indices) throws UnknownHostException {
        return elasticRestClient.fetchAllDocuments(indices);
    }

    /**
     * Get transport tcp port number used by Elasticsearch
     */
    public int getTransportTcpPort() {
        return elasticServer.getTransportTcpPort();
    }

    /**
     * Get http port number
     */
    public int getHttpPort() {
        return elasticServer.getHttpPort();
    }

    public static final class Builder {

        private Optional<String> version = Optional.empty();
        private List<Plugin> plugins = new ArrayList<>();
        private Optional<URL> downloadUrl = Optional.empty();
        private Map<String, IndexSettings> indices = new HashMap<>();
        private InstanceSettings settings = new InstanceSettings();
        private String esJavaOpts = "";
        private long startTimeoutInMs = 15_000;

        private Builder() {
        }

        public Builder withSetting(String name, Object value) {
            settings = settings.withSetting(name, value);
            return this;
        }
        
        public Builder withEsJavaOpts(String javaOpts) {
            this.esJavaOpts = javaOpts;
            return this;
        }

        /**
         * Desired version of Elasticsearch. It will be used to generate download URL to official mirrors
         */
        public Builder withElasticVersion(String version) {
            this.version = Optional.of(version);
            return this;
        }

        /**
         * <p>Elasticsearch download URL. Will overwrite download url generated by withElasticVersion method.</p>
         * <p><strong>Specify urls only to locations that you trust!</strong></p>
         */
        public Builder withDownloadUrl(URL downloadUrl) {
            this.downloadUrl = Optional.of(downloadUrl);
            return this;
        }

        /**
         * Plugin that should be installed with created instance. Treat invocation of this method as invocation of elasticsearch-plugin install command:
         * 
         * <pre>./elasticsearch-plugin install EXPRESSION</pre>
         */
        public Builder withPlugin(String expression) {
            this.plugins.add(new Plugin(expression));
            return this;
        }

        /**
         * Index that will be created in created Elasticsearch cluster
         */
        public Builder withIndex(String indexName) {
            return withIndex(indexName, IndexSettings.builder().build());
        }

        /**
         * Index that will be created in created Elasticsearch cluster
         */
        public Builder withIndex(String indexName, IndexSettings indexSettings) {
            this.indices.put(indexName, indexSettings);
            return this;
        }

        /**
         * How long should embedded-elasticsearch wait for elasticsearch to startup. Defaults to 15 seconds
         */
        public Builder withStartTimeout(long value, TimeUnit unit) {
            startTimeoutInMs = unit.toMillis(value);
            return this;
        }

        public EmbeddedElastic build() {
            return new EmbeddedElastic(
                    esJavaOpts,
                    settings, 
                    new IndicesDescription(indices), 
                    new InstallationDescription(version, downloadUrl, plugins),
                    startTimeoutInMs);
        }
        
    }
}

