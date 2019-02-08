package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.IOUtils;
import pl.allegro.tech.embeddedelasticsearch.InstallationDescription.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.embeddedelasticsearch.Require.require;

public final class EmbeddedElastic {

    private final String esJavaOpts;
    private final InstanceSettings instanceSettings;
    private final IndicesDescription indicesDescription;
    private final TemplatesDescription templatesDescription;
    private final InstallationDescription installationDescription;
    private final long startTimeoutInMs;

    private ElasticServer elasticServer;
    private ElasticRestClient elasticRestClient;
    private volatile boolean started = false;
    private final JavaHomeOption javaHome;

    public static Builder builder() {
        return new Builder();
    }

    private EmbeddedElastic(String esJavaOpts, InstanceSettings instanceSettings,
                            IndicesDescription indicesDescription, TemplatesDescription templatesDescription,
                            InstallationDescription installationDescription, long startTimeoutInMs, JavaHomeOption javaHome) {
        this.esJavaOpts = esJavaOpts;
        this.instanceSettings = instanceSettings;
        this.indicesDescription = indicesDescription;
        this.templatesDescription = templatesDescription;
        this.installationDescription = installationDescription;
        this.startTimeoutInMs = startTimeoutInMs;
        this.javaHome = javaHome;
    }

    /**
     * Downloads Elasticsearch with specified plugins, setups them and starts
     */
    public synchronized EmbeddedElastic start() throws IOException, InterruptedException {
        if (!started) {
            started = true;
            installElastic();
            startElastic();
            createRestClient();
            createTemplates();
            createIndices();
        }
        return this;
    }

    private void installElastic() throws IOException, InterruptedException {
        ElasticSearchInstaller elasticSearchInstaller = new ElasticSearchInstaller(instanceSettings, installationDescription);
        elasticSearchInstaller.install();
        File executableFile = elasticSearchInstaller.getExecutableFile();
        File installationDirectory = elasticSearchInstaller.getInstallationDirectory();
        elasticServer = new ElasticServer(esJavaOpts, installationDirectory, executableFile, startTimeoutInMs,
                installationDescription.isCleanInstallationDirectoryOnStop(), javaHome);
    }

    private void startElastic() throws IOException, InterruptedException {
        if (!elasticServer.isStarted()) {
            elasticServer.start();
        }
    }

    private void createRestClient() throws UnknownHostException {
        elasticRestClient = new ElasticRestClient(elasticServer.getHttpPort(), new HttpClient(), indicesDescription, templatesDescription);
    }

    /**
     * Stops Elasticsearch instance and removes data
     */
    public synchronized void stop() {
        if (elasticServer != null && started) {
            started = false;
            elasticServer.stop();
        }
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param indexType target index type
     * @param idJsonMap map where keys are documents ids and values are documents represented as JSON
     */
    public void index(String indexName, String indexType, Map<CharSequence, CharSequence> idJsonMap) {
        index(
                idJsonMap.entrySet().stream()
                        .map(entry -> new IndexRequest.IndexRequestBuilder(indexName, indexType, entry.getValue().toString())
                                .withId(entry.getKey().toString()).build()
                        )
                        .collect(toList())
        );
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param indexType target index name
     * @param json      document represented as JSON
     */
    public void index(String indexName, String indexType, String... json) {
        index(
                Arrays.stream(json)
                        .map(item -> new IndexRequest.IndexRequestBuilder(indexName, indexType, item).build())
                        .collect(toList())
        );
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param indexType target index name
     * @param jsons     documents represented as JSON
     */
    public void index(String indexName, String indexType, List<CharSequence> jsons) {
        index(
                jsons.stream()
                        .map(json -> new IndexRequest.IndexRequestBuilder(indexName, indexType, json.toString()).build())
                        .collect(toList())
        );
    }

    /**
     * Index single document document with routing
     *
     * @param indexRequests document to be indexed along with metadata
     */
    public void index(List<IndexRequest> indexRequests) {
        elasticRestClient.bulkIndex(indexRequests);
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
        elasticRestClient.deleteIndices();
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
        elasticRestClient.createIndices();
    }

    /**
     * Create specified index. Note that you can specify only index from list of indices specified during EmbeddedElastic creation
     *
     * @param indexName index to create
     */
    public void createIndex(String indexName) {
        elasticRestClient.createIndex(indexName);
    }

    public void createTemplates() {
        elasticRestClient.createTemplates();
    }


    /**
     * Recreates all templates (i.e. deletes and creates them again)
     */
    public void recreateTemplates() {
        deleteTemplates();
        createTemplates();
    }

    /**
     * Recreates specified template (i.e. deletes and creates it again)
     *
     * @param templateName index to recreate
     */
    public void recreateTemplate(String templateName) {
        deleteTemplate(templateName);
        createTemplate(templateName);
    }

    /**
     * Delete all templates
     */
    public void deleteTemplates() {
        elasticRestClient.deleteTemplates();
    }

    /**
     * Delete specified template
     *
     * @param templateName template do delete
     */
    public void deleteTemplate(String templateName) {
        elasticRestClient.deleteTemplate(templateName);
    }

    /**
     * Create specified template. Note that you can specify only template from list of templates specified during EmbeddedElastic creation
     *
     * @param templateName template to create
     */
    public void createTemplate(String templateName) {
        elasticRestClient.createTemplate(templateName);
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

        private InstallationSource installationSource = null;
        private List<Plugin> plugins = new ArrayList<>();
        private Map<String, Optional<IndexSettings>> indices = new HashMap<>();
        private Map<String, String> templates = new HashMap<>();
        private InstanceSettings settings = new InstanceSettings();
        private String esJavaOpts = "";
        private long startTimeoutInMs = 15_000;
        private boolean cleanInstallationDirectoryOnStop = true;
        private File installationDirectory = null;
        private File downloadDirectory = null;
        private int downloaderConnectionTimeoutInMs = 3_000;
        private int downloaderReadTimeoutInMs = 300_000;
        private Proxy downloadProxy = null;
        private JavaHomeOption javaHome = JavaHomeOption.useSystem();

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

        public Builder withInstallationDirectory(File installationDirectory) {
            this.installationDirectory = installationDirectory;
            return this;
        }

        public Builder withDownloadDirectory(File downloadDirectory) {
            this.downloadDirectory = downloadDirectory;
            return this;
        }

        public Builder withCleanInstallationDirectoryOnStop(boolean cleanInstallationDirectoryOnStop) {
            this.cleanInstallationDirectoryOnStop = cleanInstallationDirectoryOnStop;
            return this;
        }

        /**
         * Desired version of Elasticsearch. It will be used to generate download URL to official mirrors
         */
        public Builder withElasticVersion(String version) {
            this.installationSource = new InstallFromVersion(version);
            return this;
        }

        /**
         * <p>Elasticsearch download URL. Will overwrite download url generated by withElasticVersion method.</p>
         * <p><strong>Specify urls only to locations that you trust!</strong></p>
         */
        public Builder withDownloadUrl(URL downloadUrl) {
            this.installationSource = new InstallFromDirectUrl(downloadUrl);
            return this;
        }

        /**
         * In resource path to Elasticsearch zip archive.
         */
        public Builder withInResourceLocation(String inResourcePath) {
            this.installationSource = new InstallFromResources(inResourcePath);
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
        public Builder withIndex(String indexName, IndexSettings indexSettings) {
            this.indices.put(indexName, Optional.of(indexSettings));
            return this;
        }

        public Builder withIndex(String indexName) {
            this.indices.put(indexName, Optional.empty());
            return this;
        }

        /**
         * add a template that will be created after Elasticsearch cluster started
         *
         * @param name
         * @param templateBody
         * @return
         */
        public Builder withTemplate(String name, String templateBody) {
            this.templates.put(name, templateBody);
            return this;
        }

        /**
         * add a template that will be created after Elasticsearch cluster started
         *
         * @param name
         * @param templateBody
         * @return
         * @throws IOException
         */
        public Builder withTemplate(String name, InputStream templateBody) throws IOException {
            return withTemplate(name, IOUtils.toString(templateBody, UTF_8));
        }

        /**
         * How long should embedded-elasticsearch wait for elasticsearch to startup. Defaults to 15 seconds
         */
        public Builder withStartTimeout(long value, TimeUnit unit) {
            startTimeoutInMs = unit.toMillis(value);
            return this;
        }

        /**
         * Set connection timeout for HTTP client used by downloader
         */
        public Builder withDownloaderConnectionTimeout(long value, TimeUnit unit) {
            downloaderConnectionTimeoutInMs = (int) unit.toMillis(value);
            return this;
        }

        /**
         * Set read timeout for HTTP client used by downloader
         */
        public Builder withDownloaderReadTimeout(long value, TimeUnit unit) {
            downloaderReadTimeoutInMs = (int) unit.toMillis(value);
            return this;
        }

        /**
         * Set proxy that should be used to download elastic package
         */
        public Builder withDownloadProxy(Proxy proxy) {
            downloadProxy = proxy;
            return this;
        }

        public Builder withJavaHome(JavaHomeOption javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public EmbeddedElastic build() {
            require(installationSource != null, "You must specify elasticsearch version, or download url");
            return new EmbeddedElastic(
                    esJavaOpts,
                    settings,
                    new IndicesDescription(indices),
                    new TemplatesDescription(templates),
                    new InstallationDescription(installationSource, downloadDirectory, installationDirectory, cleanInstallationDirectoryOnStop, plugins, downloaderConnectionTimeoutInMs, downloaderReadTimeoutInMs, downloadProxy),
                    startTimeoutInMs,
                    javaHome);
        }

    }
}

