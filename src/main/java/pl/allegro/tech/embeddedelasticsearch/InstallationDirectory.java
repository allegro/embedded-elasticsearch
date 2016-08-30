package pl.allegro.tech.embeddedelasticsearch;

import java.io.File;

class InstallationDirectory {
    
    private final File executableLocation;
    private final File dataDirectory;

    InstallationDirectory(File executableLocation, File dataDirectory) {
        this.executableLocation = executableLocation;
        this.dataDirectory = dataDirectory;
    }

    File getExecutableLocation() {
        return executableLocation;
    }

    File getDataDirectory() {
        return dataDirectory;
    }
}
