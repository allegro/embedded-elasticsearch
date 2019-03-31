package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

class InstallationDescription {

    private static final File DEFAULT_INSTALL_DIR = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-temp-dir");
    private static final File DEFAULT_DOWNLOAD_DIR = DEFAULT_INSTALL_DIR;

    private final InstallationSource installationSource;
    private final List<Plugin> plugins;
    private final boolean cleanInstallationDirectoryOnStop;
    private final File installationDirectory;
    private final File downloadDirectory;
    private final int downloaderConnectionTimeoutInMs;
    private final int downloaderReadTimeoutInMs;
    private final Proxy downloadProxy;

    InstallationDescription(
            InstallationSource installationSource,
            File downloadDirectory,
            File installationDirectory,
            boolean cleanInstallationDirectoryOnStop,
            List<Plugin> plugins,
            int downloaderConnectionTimeoutInMs,
            int downloaderReadTimeoutInMs, Proxy downloadProxy) {
        this.installationSource = installationSource;
        this.plugins = plugins;
        this.cleanInstallationDirectoryOnStop = cleanInstallationDirectoryOnStop;
        this.installationDirectory = ObjectUtils.firstNonNull(installationDirectory, DEFAULT_INSTALL_DIR);
        this.downloadDirectory = ObjectUtils.firstNonNull(downloadDirectory, DEFAULT_DOWNLOAD_DIR);
        this.downloaderConnectionTimeoutInMs = downloaderConnectionTimeoutInMs;
        this.downloaderReadTimeoutInMs = downloaderReadTimeoutInMs;
        this.downloadProxy = downloadProxy;
    }

    String getVersion() {
        return installationSource.determineVersion();
    }

    URL getDownloadUrl() {
        return installationSource.resolveDownloadUrl();
    }

    List<Plugin> getPlugins() {
        return plugins;
    }

    boolean versionIs1x() {
        return getVersion().startsWith("1.");
    }

    boolean versionIs2x() {
        return getVersion().startsWith("2.");
    }

    /**
     * Indicates whether a given version matches or is after a given reference version.
     * @param version the version to test.
     * @param referenceVersion the reference version.
     * @return {@code true} if the given version matches or is after a given reference version, {@code false} otherwise.
     */
    static boolean versionMatchOrAfter(String version, String referenceVersion) {
        String[] versionsSource = version.split("\\.");
        String[] versionsTarget = referenceVersion.split("\\.");
        for (int i = 0; i < versionsSource.length && i < versionsTarget.length; i++) {
            String versionSource = versionsSource[i];
            int index = -1;
            if (i == versionsSource.length - 1) {
                index = versionSource.indexOf('-');
                if (index != -1) {
                    versionSource = versionSource.substring(0, index);
                }
            }
            int iVersionTarget = Integer.parseInt(versionsTarget[i]);
            int iVersionSource = Integer.parseInt(versionSource);
            if (iVersionTarget > iVersionSource || (index != -1 && iVersionTarget == iVersionSource)) {
                return false;
            }
        }
        return true;
    }

    boolean versionMatchOrAfter(String startVersion) {
        return versionMatchOrAfter(getVersion(), startVersion);
    }

    boolean isCleanInstallationDirectoryOnStop() {
        return cleanInstallationDirectoryOnStop;
    }

    File getInstallationDirectory() {
        return installationDirectory;
    }

    File getDownloadDirectory() {
        return downloadDirectory;
    }

    int getDownloaderConnectionTimeoutInMs() {
        return downloaderConnectionTimeoutInMs;
    }

    int getDownloaderReadTimeoutInMs() {
        return downloaderReadTimeoutInMs;
    }

    Proxy getDownloadProxy() {
        return downloadProxy;
    }

    static class Plugin {
        private String expression;

        Plugin(String expression) {
            this.expression = expression;
        }

        String getExpression() {
            return expression;
        }

        @Override
        public String toString() {
            return expression;
        }

        String getPluginName() {
            if (expressionIsUrl()) {
                return FilenameUtils.getBaseName(expression).replaceAll("-[\\d].*", "");
            }
            return expression;
        }

        boolean expressionIsUrl() {
            return expression.startsWith("http");
        }
    }
}
