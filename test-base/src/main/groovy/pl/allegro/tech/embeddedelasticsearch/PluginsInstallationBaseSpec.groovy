package pl.allegro.tech.embeddedelasticsearch

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import spock.lang.Specification

abstract class PluginsInstallationBaseSpec extends Specification {

    static final HTTP_PORT_VALUE = 9200

    def "should install plugin from url"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin(pluginByUrlUrl())
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == [pluginByUrlName()].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    def "should install plugin from name"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin(pluginByName())
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == [pluginByNameName()].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    def "should install multiple plugins"() {
        given:
            final embeddedElastic = baseEmbeddedElastic()
                    .withPlugin(pluginByUrlUrl())
                    .withPlugin(pluginByName())
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == [pluginByUrlName(), pluginByNameName()].toSet()

        cleanup:
            embeddedElastic.stop()
    }

    def "should reuse plugins on restart"() {
        given:
            final installationDirectory = new File(System.getProperty("java.io.tmpdir"), "embedded-elasticsearch-for-reuse")
            installationDirectory.deleteDir()
            baseEmbeddedElastic()
                    .withCleanInstallationDirectoryOnStop(false)
                    .withInstallationDirectory(installationDirectory)
                    .withPlugin(pluginByUrlUrl())
                    .build().start().stop()
            final pluginCustomFile = new File(installationDirectory.listFiles()
                    .findAll { it.isDirectory() }
                    .find { it.name.startsWith("elasticsearch-")}, "plugins/" + pluginByUrlName() + "/foo.txt")
            pluginCustomFile.createNewFile();
            final embeddedElastic = baseEmbeddedElastic()
                    .withCleanInstallationDirectoryOnStop(false)
                    .withInstallationDirectory(installationDirectory)
                    .withPlugin(pluginByUrlUrl())
                    .build()

        when:
            embeddedElastic.start()

        then:
            pluginCustomFile.exists()

        cleanup:
            embeddedElastic?.stop()
            installationDirectory.deleteDir()
    }

    def "should reuse only specified plugins on restart"() {
        given:
            baseEmbeddedElastic()
                    .withCleanInstallationDirectoryOnStop(false)
                    .withPlugin(pluginByUrlUrl())
                    .withPlugin(pluginByName())
                    .build().start().stop()
            final embeddedElastic = baseEmbeddedElastic()
                    .withCleanInstallationDirectoryOnStop(false)
                    .withPlugin(pluginByName())
                    .build()

        when:
            embeddedElastic.start()

        then:
            fetchInstalledPluginsList() == [pluginByNameName()].toSet()

        cleanup:
            embeddedElastic?.stop()
    }

    abstract EmbeddedElastic.Builder baseEmbeddedElastic()

    Set<String> fetchInstalledPluginsList() {
        final request = new HttpGet("http://localhost:$HTTP_PORT_VALUE/_cat/plugins?format=json")
        final client = HttpClients.createDefault()
        final response = client.execute(request)
        final responseBody = response.entity.content.text
        final json = new JsonSlurper().parseText(responseBody)
        return json.collect { it.component }.toSet()
    }

    abstract String pluginByUrlUrl()

    abstract String pluginByUrlName()

    abstract String pluginByName()

    String pluginByNameName() { pluginByName() }

}
