package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.FileUtils;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.embeddedelasticsearch.InstallationDescription.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

class ElasticSearchInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchInstaller.class);
    private static final String ELS_PACKAGE_PREFIX = "elasticsearch-";

    private final InstanceSettings instanceSettings;
    private final InstallationDescription installationDescription;
    private final ElasticDownloader elasticDownloader;

    ElasticSearchInstaller(InstanceSettings instanceSettings, InstallationDescription installationDescription) {
        this.instanceSettings = instanceSettings;
        this.installationDescription = installationDescription;
        this.elasticDownloader = new ElasticDownloader(installationDescription);
    }

    File getExecutableFile() {
        return fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch"));
    }

    File getInstallationDirectory() {
        return getFile(installationDescription.getInstallationDirectory(), ELS_PACKAGE_PREFIX + installationDescription.getVersion());
    }

    void install() throws IOException, InterruptedException {
        Path downloadedTo = elasticDownloader.download();
        installElastic(downloadedTo);
        configureElastic();
        installPlugins();
    }

    private void installElastic(Path downloadedTo) throws IOException {
        File destination = getInstallationDirectory().getParentFile();
        if (installationDescription.isCleanInstallationDirectoryOnStop() || !fileRelativeToInstallationDir("bin").exists()) {
            if (getInstallationDirectory().exists()) {
                FileUtils.forceDelete(getInstallationDirectory());
            }
            logger.info("Installing Elasticsearch into {}...", destination);
            try {
                unzip(downloadedTo, destination);
                logger.info("Done");
            } catch (IOException e) {
                logger.info("Failure : " + e);
                throw new EmbeddedElasticsearchStartupException(e);
            }
        } else {
            logger.info("Cleaning the existing Elasticsearch directory for reuse.");
            File dataDirectory = fileRelativeToInstallationDir("data");
            if (dataDirectory.exists()) {
                FileUtils.forceDelete(dataDirectory);
            }
        }
    }

    private void unzip(Path downloadedTo, File destination) throws IOException {
        Archiver archiver = ArchiverFactory.createArchiver("zip");
        archiver.extract(downloadedTo.toFile(), destination);
    }

    private void configureElastic() throws IOException {
        File elasticsearchYml = getFile(getInstallationDirectory(), "config", "elasticsearch.yml");
        FileUtils.writeStringToFile(elasticsearchYml, instanceSettings.toYaml(), UTF_8);
    }

    /**
     * Installs all the plugins.
     */
    private void installPlugins() throws IOException, InterruptedException {
        File pluginManager = pluginManagerExecutable();
        Set<String> alreadyInstalledPlugins = getAlreadyInstalledPlugins();
        Set<String> unusedPlugins = new HashSet<>(alreadyInstalledPlugins);
        for (Plugin plugin : installationDescription.getPlugins()) {
            if (isPluginInstalled(plugin, alreadyInstalledPlugins)) {
                unusedPlugins.remove(getPluginName(plugin));
                logger.info("> Plugin {} already installed, skipping", plugin.getPluginName());
            } else {
                installPlugin(pluginManager, plugin);
            }
        }
        uninstallUnusedPlugins(pluginManager, unusedPlugins);
    }

    /**
     * @param pluginManager the plugin manager to launch to un-install the plugins
     * @param additionalPlugins all the plugins to un-install.
     */
    private void uninstallUnusedPlugins(File pluginManager, Set<String> additionalPlugins) throws IOException, InterruptedException {
        for (String pluginName : additionalPlugins) {
            uninstallPlugin(pluginManager, pluginName);
        }
    }

    /**
     * @return the plugins that are already installed.
     */
    private Set<String> getAlreadyInstalledPlugins() {
        File pluginsDir = new File(getInstallationDirectory(), "plugins");
        String[] pluginList = pluginsDir.list();
        if (pluginList != null && pluginList.length > 0) {
            return Stream.of(pluginList).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Executes a command.
     * @param args the command and its parameters to execute.
     * @param consumer the consumer of the output of the command
     * @return the exit value of the command. By convention, the value 0 indicates normal termination.
     */
    private int executeCommand(String[] args, Consumer<String> consumer) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectOutput(Redirect.PIPE);
        builder.redirectErrorStream(true);
        builder.command(args);
        ElasticServer.forceDeleteElasticTempDirectory();
        Process process = builder.start();
        try (BufferedReader bReader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
            String line;
            while ((line = bReader.readLine()) != null) {
                consumer.accept(line);
            }
            return process.waitFor();
        } finally {
            ElasticServer.forceDeleteElasticTempDirectory();
        }
    }

    /**
     * Launches the command that will install the plugin
     * @param pluginManager the plugin manager to launch to install the plugin
     * @param plugin the plugin to install
     */
    private void installPlugin(File pluginManager, Plugin plugin) throws IOException, InterruptedException {
        logger.info("> {} install ", pluginManager, plugin.getExpression());
        if (executeCommand(prepareInstallCommand(pluginManager, plugin), line -> logger.info("Plugin install {}: {}", plugin, line)) != 0) {
            throw new EmbeddedElasticsearchStartupException(String.format("Unable to install plugin: %s",  plugin));
        }
    }

    /**
     * Launches the command that will un-install the plugin
     * @param pluginManager the plugin manager to launch to uninstall the plugin
     * @param pluginName the name of the plugin to un-install
     */
    private void uninstallPlugin(File pluginManager, String pluginName) throws IOException, InterruptedException {
        logger.info("> {} uninstall ", pluginManager, pluginName);
        if (executeCommand(prepareUninstallCommand(pluginManager, pluginName), line -> logger.info("Plugin uninstall {}: {}", pluginName, line)) != 0) {
            throw new EmbeddedElasticsearchStartupException(String.format("Unable to uninstall plugin: %s", pluginName));
        }
    }

    /**
     * @param plugin the plugin to check
     * @param alreadyInstalledPlugins the already installed plugins.
     * @return {@code true} if the plugin has already been installed, {@code false} otherwise.
     */
    private boolean isPluginInstalled(Plugin plugin, Set<String> alreadyInstalledPlugins) {
        return alreadyInstalledPlugins.contains(getPluginName(plugin));
    }

    /**
     * @param plugin the plugin from which the name must be extracted
     * @return the name of the plugin
     */
    private String getPluginName(Plugin plugin) {
        String pluginName = plugin.getPluginName();
        if (installationDescription.versionIs1x()) {
            pluginName = extractPluginName(pluginName);
        }
        return pluginName;
    }

    /**
     * @param name the original name of the plugin
     * @return the extracted name of the plugin
     */
    private String extractPluginName(String name) {
        String[] elements = name.split("/");
        String pluginName;
        if (elements.length > 1) {
            // We consider the form: username/pluginname or username/pluginname/version
            pluginName = elements[1];
        } else {
            // We consider the simplest form: pluginname
            pluginName = elements[0];
        }
        if (pluginName.startsWith("elasticsearch-")) {
            // remove elasticsearch- prefix
            pluginName = pluginName.substring("elasticsearch-".length());
        } else if (name.startsWith("es-")) {
            // remove es- prefix
            pluginName = pluginName.substring("es-".length());
        }
        return pluginName;
    }

    /**
     * @param pluginManager the plugin manager to launch
     * @param plugin the plugin to install.
     * @return the command and its parameters to use to install the given plugin.
     */
    private String[] prepareInstallCommand(File pluginManager, Plugin plugin) {
        if (installationDescription.versionIs1x()) {
            if (plugin.expressionIsUrl()) {
                return new String[]{pluginManager.getAbsolutePath(), "--install", plugin.getPluginName(), "--url", plugin.getExpression()};
            }
            return new String[]{pluginManager.getAbsolutePath(), "--install", plugin.getPluginName()};
        }
        if (installationDescription.versionIs2x()) {
            return new String[]{pluginManager.getAbsolutePath(), "install", plugin.getExpression()};
        }
        return new String[]{pluginManager.getAbsolutePath(), "install", "--batch", plugin.getExpression()};
    }

    /**
     * @param pluginManager the plugin manager to launch
     * @param pluginName the name of the plugin to un-install.
     * @return the command and its parameters to use to un-install the given plugin.
     */
    private String[] prepareUninstallCommand(File pluginManager, String pluginName) {
        if (installationDescription.versionIs1x()) {
            return new String[]{pluginManager.getAbsolutePath(), "--remove", pluginName};
        }
        if (installationDescription.versionMatchOrAfter("5.5.0")) {
            return new String[]{pluginManager.getAbsolutePath(), "remove", "--purge", pluginName};
        }
        return new String[]{pluginManager.getAbsolutePath(), "remove", pluginName};
    }

    private File pluginManagerExecutable() throws IOException {
        File elasticsearchPlugin = fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch-plugin"));
        File pluginManager;
        if (elasticsearchPlugin.exists()) {
            pluginManager = elasticsearchPlugin;
        } else {
            pluginManager = fileRelativeToInstallationDir("bin", systemDependentExtension("plugin"));
        }
        return pluginManager;
    }

    private String systemDependentExtension(String baseFileName) {
        return baseFileName + (IS_OS_WINDOWS ? ".bat" : "");
    }

    private File fileRelativeToInstallationDir(String... path) {
        return getFile(getInstallationDirectory(), path);
    }

}
