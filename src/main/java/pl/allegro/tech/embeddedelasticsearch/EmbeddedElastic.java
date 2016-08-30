package pl.allegro.tech.embeddedelasticsearch;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.elasticsearch.common.settings.Settings.settingsBuilder;

public final class EmbeddedElastic {

    private final InstanceDescription instanceDescription;
    private final IndicesDescription indicesDescription;
    private final InstallationDescription installationDescription;
    
    private ElasticServer elasticServer;
    private ElasticOps elasticOps;

    public static Builder builder() {
        return new Builder();
    }

    private EmbeddedElastic(InstanceDescription instanceDescription, IndicesDescription indicesDescription, InstallationDescription installationDescription) {
        this.instanceDescription = instanceDescription;
        this.indicesDescription = indicesDescription;
        this.installationDescription = installationDescription;
    }

    /**
     * Downloads Elasticsearch with specified plugins, setups them and starts
     */
    public EmbeddedElastic start() throws IOException, InterruptedException {
        installElastic();
        startElastic();
        createOps();
        createIndices();
        return this;
    }

    private void installElastic() throws IOException {
        ElasticSearchInstaller elasticSearchInstaller = new ElasticSearchInstaller(installationDescription);
        elasticSearchInstaller.install();
        File executableFile = elasticSearchInstaller.getExecutableFile();
        File dataDirectory = elasticSearchInstaller.getDataDirectory();
        elasticServer = new ElasticServer(instanceDescription, new InstallationDirectory(executableFile, dataDirectory));
    }

    private void startElastic() throws IOException, InterruptedException {
        if (!elasticServer.isStarted()) {
            elasticServer.start();
        }
    }

    /**
     * Create transport client, with default settings
     * @throws UnknownHostException in case when literal 'localhost' cannot be resolved by OS
     */
    public Client createClient() throws UnknownHostException {
        Settings settings = settingsBuilder()
                .put("cluster.name", instanceDescription.getClusterName())
                .build();

        return TransportClient.builder()
                .settings(settings)
                .build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), instanceDescription.getPort()));
    }

    private void createOps() throws UnknownHostException {
        Client client = createClient();
        elasticOps = new ElasticOps(client, indicesDescription);
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
        elasticOps.indexWithIds(indexName, indexType, idJsonMap.entrySet().stream()
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
     * @param json document represented as JSON
     */
    public void index(String indexName, String indexType, List<CharSequence> json) {
        elasticOps.index(indexName, indexType, json.stream().map(CharSequence::toString).collect(toList()));
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
        elasticOps.deleteIndices();
    }

    /**
     * Delete specified index
     * @param indexName index do delete
     */
    public void deleteIndex(String indexName) {
        elasticOps.deleteIndex(indexName);
    }

    /**
     * Create all indices
     */
    public void createIndices() {
        elasticOps.createIndices();
    }

    /**
     * Create specified index. Note that you can specify only index from list of indices specified during EmbeddedElastic creation
     * @param indexName index to create
     */
    public void createIndex(String indexName) {
        elasticOps.createIndex(indexName);
    }

    /**
     * Refresh indices. Can be useful in tests that uses multiple threads
     */
    public void refreshIndices() {
        elasticOps.refresh();
    }

    /**
     * Fetch all documents from specified indices. Useful for logging and debugging
     * @return list containing documents sources represented as JSON
     */
    public List<String> fetchAllDocuments(String... indices) throws UnknownHostException {
        SearchResponse searchResponse = createClient().prepareSearch(indices).execute().actionGet();
        return Stream.of(searchResponse.getHits().getHits())
                .map(SearchHit::getSourceAsString)
                .collect(Collectors.toList());
    }

    public static final class Builder {

        private int portNumber = 9300;
        private String clusterName = "elasticsearch";
        private Optional<String> version = Optional.empty();
        private List<InstallationDescription.Plugin> plugins = new ArrayList<>();
        private Optional<URL> downloadUrl = Optional.empty();
        private Map<String, IndexSettings> indices = new HashMap<>();

        private Builder() {
        }

        /**
         * Port number on which created Elasticsearch instance will listen
         */
        public Builder withPortNumber(int portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        /**
         * Cluster name that will be used by created Elasticsearch instance
         */
        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
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
         * Plugin that should be installed with created instance
         */
        public Builder withPlugin(String name, URL urlToDownload) {
            this.plugins.add(new InstallationDescription.Plugin(name, urlToDownload));
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

        public EmbeddedElastic build() {
            return new EmbeddedElastic(
                    new InstanceDescription(portNumber, clusterName), 
                    new IndicesDescription(indices), 
                    new InstallationDescription(version, downloadUrl, plugins));
        }
        
    }
}

