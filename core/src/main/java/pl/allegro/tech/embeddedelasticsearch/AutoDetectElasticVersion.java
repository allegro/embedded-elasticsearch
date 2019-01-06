package pl.allegro.tech.embeddedelasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import static java.io.File.separatorChar;
import static java.lang.Integer.parseInt;
import static java.util.Collections.list;
import static java.util.Comparator.comparingInt;

/**
 * Try to determine elasticsearch version from jars loaded on the classpath.<br/>
 * Note that auto-detection only works with elasticearch 5 and higher.
 */
public class AutoDetectElasticVersion {

  private static final Logger LOG = LoggerFactory.getLogger(AutoDetectElasticVersion.class);

  /**
   * @return highest elasticsearch client version found on classpath or null when no version is found
   */
  public static String detect() {
    return getElasticsearchClientResources()
        .map(URL::getFile)
        .map(pathString -> {
          final int versionStart = pathString.lastIndexOf('-');
          final int versionEnds = pathString.lastIndexOf('.');
          return pathString.substring(versionStart + 1, versionEnds);
        })
        .max(comparingInt(AutoDetectElasticVersion::getSortableVersion))
        .orElse(null);
  }

  private static Stream<URL> getElasticsearchClientResources() {
    try {
      return list(ClassLoader.getSystemClassLoader()
          .getResources("org" + separatorChar + "elasticsearch" + separatorChar + "client"))
          .stream();
    } catch (IOException e) {
      LOG.error("Error loading jars from classpath", e);
      return Stream.empty();
    }
  }

  private static int getSortableVersion(final String versionString) {
    final String[] versionSplit = versionString.split("\\.");
    final int major = parseInt(versionSplit[0]);
    final int minor = parseInt(versionSplit[1]);
    final int patch = parseInt(versionSplit[2]);
    return major * 1_000_000 + minor * 1_000 + patch;
  }


}
