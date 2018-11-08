package pl.allegro.tech.embeddedelasticsearch;

import java.net.URL;

interface InstallationSource {
    String determineVersion();

    URL resolveDownloadUrl();

    boolean isOssFlavor();
}

