package pl.allegro.tech.embeddedelasticsearch;

import java.io.File;

import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

class ElsFilesUtils {

    private static final String ELS_PACKAGE_PREFIX = "elasticsearch-";
    private static final File baseDirectory = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-temp-dir");

    static File getExecutableFile() {
        return fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch"));
    }

    static File getPluginManagerExecutable() {
        File elasticsearchPlugin = fileRelativeToInstallationDir("bin", systemDependentExtension("elasticsearch-plugin"));
        if (elasticsearchPlugin.exists()) {
            return elasticsearchPlugin;
        } else {
            return fileRelativeToInstallationDir("bin", systemDependentExtension("plugin"));
        }
    }
    
    static File getBinDirectory() {
        return fileRelativeToInstallationDir("bin");
    }
    
    static File getElasticsearchYmlConfigFile() {
        return fileRelativeToInstallationDir("config", "elasticsearch.yml");
    }
    
    static private String systemDependentExtension(String baseFileName) {
        return baseFileName + (IS_OS_WINDOWS ? ".bat" : "");
    }

    static private File fileRelativeToInstallationDir(String... path) {
        return getFile(getInstallationDirectory(), path);
    }

    static File getInstallationDirectory() {
        File[] foundFiles = baseDirectory.listFiles(file -> file.isDirectory() && file.getName().startsWith(ELS_PACKAGE_PREFIX));
        Require.require(foundFiles.length == 1, "Expected only one file matching name: " + ELS_PACKAGE_PREFIX + "{version} in directory: " + baseDirectory);
        return foundFiles[0];
    }
}

