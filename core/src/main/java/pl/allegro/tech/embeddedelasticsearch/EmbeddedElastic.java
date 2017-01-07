package pl.allegro.tech.embeddedelasticsearch;

import pl.allegro.tech.embeddedelasticsearch.InstallationDescription.Plugin;

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

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.embeddedelasticsearch.Require.require;

public final class EmbeddedElastic {

    private final IndicesDescription indicesDescription;
    private final ElsInstance elsInstance;

    private ElasticRestClient elasticRestClient;

    public static Builder builder() {
        return new Builder();
    }

    private EmbeddedElastic(IndicesDescription indicesDescription, ElsInstance elsInstance) {
        this.indicesDescription = indicesDescription;
        this.elsInstance = elsInstance;
    }

    /**
     * Downloads Elasticsearch with specified plugins, setups them and starts
     */
    public EmbeddedElastic start() {
        elsInstance.start();
        elasticRestClient = elsInstance.createRestClient();
        createIndices();
        return this;
    }

    /**
     * Stops Elasticsearch instance and removes data
     */
    public void stop() {
        elsInstance.stop();
    }

    /**
     * Index documents
     *
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
     *
     * @param indexName target index
     * @param indexType target index name
     * @param json      document represented as JSON
     */
    public void index(String indexName, String indexType, String... json) {
        index(indexName, indexType, Arrays.asList(json));
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param indexType target index name
     * @param jsons     documents represented as JSON
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
     *
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
        indicesDescription.getIndicesNames().forEach(elasticRestClient::deleteIndex);
    }

    /**
     * Delete specified index
     *
     * @param indexName index do delete
     */
    public void deleteIndex(String indexName) {
        elasticRestClient.deleteIndex(indexName);
    }

    /**
     * Create all indices
     */
    public void createIndices() {
        indicesDescription.getIndicesNames().forEach(indexName ->
                elasticRestClient.createIndex(indexName, indicesDescription.getIndexSettings(indexName)));
    }

    /**
     * Create specified index. Note that you can specify only index from list of indices specified during EmbeddedElastic creation
     *
     * @param indexName index to create
     */
    public void createIndex(String indexName) {
        elasticRestClient.createIndex(indexName, indicesDescription.getIndexSettings(indexName));
    }

    /**
     * Refresh indices. Can be useful in tests that uses multiple threads
     */
    public void refreshIndices() {
        elasticRestClient.refresh();
    }

    /**
     * Fetch all documents from specified indices. Useful for logging and debugging
     *
     * @return list containing documents sources represented as JSON
     */
    public List<String> fetchAllDocuments(String... indices) throws UnknownHostException {
        return elasticRestClient.fetchAllDocuments(indices);
    }

    /**
     * Get transport tcp port number used by Elasticsearch
     * 
     * Not supported when you are attaching to existing cluster
     */
    public int getTransportTcpPort() {
        return elsInstance.getTransportTcpPort();
    }

    /**
     * Get http port number
     *
     * Not supported when you are attaching to existing cluster
     */
    public int getHttpPort() {
        return elsInstance.getHttpPort();
    }

    public static final class Builder {

        private Optional<String> version = Optional.empty();
        private List<Plugin> plugins = new ArrayList<>();
        private Optional<URL> downloadUrl = Optional.empty();
        private Map<String, IndexSettings> indices = new HashMap<>();
        private InstanceSettings settings = new InstanceSettings();
        private String esJavaOpts = "";
        private long startTimeoutInMs = 15_000;
        private Optional<String> existingInstanceUrl = Optional.empty();

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
         * <p>
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

        /**
         * Instead of installing new instance of elastic search, attach to existing cluster
         */
        public Builder useExistingInstance(String existingInstanceUrl) {
            this.existingInstanceUrl = Optional.of(existingInstanceUrl);
            return this;
        }

        public EmbeddedElastic build() {
            ElsInstance elsInstance = createElsInstance();
            return new EmbeddedElastic(new IndicesDescription(indices), elsInstance);
        }

        private ElsInstance createElsInstance() {
            if (existingInstanceUrl.isPresent()) {
                require(!version.isPresent() && !downloadUrl.isPresent() && plugins.isEmpty(),
                        "You cannot specify version, downloadUrl and plugins when using existing instance");
                return new ExistingElsInstance(existingInstanceUrl.get());
            } else {
                return new ControlledElsInstance(new InstallationDescription(version, downloadUrl, plugins, settings, startTimeoutInMs, esJavaOpts));
            }
        }
    }
}

