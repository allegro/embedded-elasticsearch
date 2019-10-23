# embedded-elasticsearch

[![Build Status](https://travis-ci.org/allegro/embedded-elasticsearch.svg?branch=master)](https://travis-ci.org/allegro/embedded-elasticsearch)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.allegro.tech/embedded-elasticsearch/badge.svg)](http://central.maven.org/maven2/pl/allegro/tech/embedded-elasticsearch)

Small utility for creating integration tests that use Elasticsearch. Instead of using `Node` it downloads Elasticsearch in specified version and starts it in a separate process. It also allows you to install required plugins which is not possible when using `NodeBuilder`. Utility was tested with 1.x, 2.x, 5.x, 6.x and 7.x versions of Elasticsearch.

## Introduction

All you need to do to use this tool is create `EmbeddedElastic` instance. To do so, use provided builder:

```java
final embeddedElastic = EmbeddedElastic.builder()
        .withElasticVersion("5.0.0")
        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
        .withSetting(PopularProperties.CLUSTER_NAME, "my_cluster")
        .withPlugin("analysis-stempel")
        .withIndex("cars", IndexSettings.builder()
            .withType("car", getSystemResourceAsStream("car-mapping.json"))
            .build())
        .withIndex("books", IndexSettings.builder()
            .withType(PAPER_BOOK_INDEX_TYPE, getSystemResourceAsStream("paper-book-mapping.json"))
            .withType("audio_book", getSystemResourceAsStream("audio-book-mapping.json"))
            .withSettings(getSystemResourceAsStream("elastic-settings.json"))
            .build())
        .build()
        .start()
```

When you are done with creating it, you can start it real simple:

```java
embeddedElastic.start()
```

And that's all, you can connect to your embedded-elastic instance on specified port and use it in your tests.

## Available builder options

| Method | Description |
| ------------- | ------------- |
| `withElasticVersion(String version)` | version of Elasticsearch; based on that version download url to official Elasticsearch repository will be created |
| `withDownloadUrl(URL downloadUrl)` | if you prefer to download Elasticsearch from a different location than official repositories you can do that using this method |
| `withInResourceLocation(String inResourcePath)` | you can also have your Elasticsearch package inside resource directory, specify it's location with this option  |
| `withSetting(String key, Object value)` | setting name and value as in elasticsearch.yml file |
| `withPlugin(String expression)` | plugin that should be installed into Elasticsearch; treat expression as argument to `./elasticsearch-plugin install <expression>` command; use multiple times for multiple plugins |
| `withIndex(String indexName, IndexSettings indexSettings)` | specify index that should be created and managed by EmbeddedElastic |
| `withTemplate(String templateName, String templateBody)` | specify a template that should be created and managed by EmbeddedElastic |
| `withStartTimeout(long value, TimeUnit unit)` | specify timeout you give Elasticsearch to start |
| `withInstallationDirectory(File installationDirectory)` | specify custom installation directory |
| `withDownloadDirectory(File downloadDirectory)` | specify custom download directory where downloaded distribution packages will be saved |
| `withCleanInstallationDirectoryOnStop(boolean cleanInstallationDirectoryOnStop)` | specify whether clean the installation directory after Elasticsearch stop |
| `withEsJavaOpts(String javaOpts)` | value of `ES_JAVA_OPTS` variable to be set for Elasticsearch process |
| `withJavaHome(JavaHomeOption javaHomeOption)` | select java environment to run in. For available options see below |
| `getTransportTcpPort()` | get transport tcp port number used by Elasticsearch instance |
| `getHttpPort()` | get http port number used by Elasticsearch instance |
| `withDownloadProxy(Proxy proxy)` | proxy that should be used for downloading Elasticsearch package |
| `withDownloaderConnectionTimeout(long value, TimeUnit unit)` | connection timeout that should be used by downloader |
| `withDownloaderReadTimeout(long value, TimeUnit unit)` | socket timeout that should be used by downloader |

Available `IndexSettings.Builder` options

| Method | Description |
| ------------- | ------------- |
| `withType(String type, String mapping)` | specify type and it's mappings |
| `withMapping(String mapping)` | starting from Elasticseatch 7, there is no more types, so when using an ES version 7.0 and above this method should be used insted of withType method |
| `withSettings(String settings)` | specify index settings |


Availabe `JavaHomeOption` options

| Method | Description |
| ------------- | ------------- |
| `useSystem()` | default behavior, lets elasticsearch startup script determine the JRE to run in |
| `inheritTestSuite()` | use the same JRE as the process starting it |
| `path(String path)` | manually set the path of the JRE to execute the embedded elastic |


## Available operations

`EmbeddedElastic` provides following operations:

| Method | Description |
| ------------- | ------------- |
| `start()` | downloads Elasticsearch and specified plugins, setups everything and finally starts your Elasticsearch instance |
| `stop()` | stops your Elasticsearch instance and removes all data |
| `index` | index your document, comes with variants that take only document, or document and it's id |
| `deleteIndex(String indexName)`, `deleteIndices()`  | deletes index with name specified during EmbeddedElastic creation |
| `createIndex(String indexName)`, `createIndices()` | creates index with name specified during EmbeddedElastic creation; note that this index is created during EmbeddedElastic startup, you will need this method only if you deleted your index using `deleteIndex` method |  
| `recreateIndex(String indexName)`, `recreateIndices()` | combination of `deleteIndex` and `createIndex` |
| `refreshIndices()` | refresh index; useful when you make changes in different thread, and want to check results instantly in tests |
| `deleteTemplate(String templateName)`, `deleteTemplates()`  | deletes a template of the specified name/all templates during EmbeddedElastic creation |
| `createTemplate(String templateName)`, `createTemplates()` | creates a template with the specified name/all templates during EmbeddedElastic creation; note that this template is created during EmbeddedElastic startup, you will need this method only if you deleted your template using `deleteTemplate` method |  
| `recreateTemplate(String templateName)`, `recreateTemplates()` | combination of `deleteTemplate` and `createTemplate` |

## Example
If you want to see example, look at this spec: `pl.allegro.tech.search.embeddedelasticsearch.EmbeddedElasticSpec`

## Dependency
To start using embedded-elasticsearch in your project add it as a test dependency:

Gradle:

```groovy
testCompile 'pl.allegro.tech:embedded-elasticsearch:2.7.0'
```

Maven:

```xml
<dependency>
    <groupId>pl.allegro.tech</groupId>
    <artifactId>embedded-elasticsearch</artifactId>
    <version>2.7.0</version>
    <scope>test</scope>
</dependency>
```

SBT:

```
libraryDependencies ++= Seq("pl.allegro.tech" % "embedded-elasticsearch" % "2.7.0" % "test")
```

## Known problems
If you build your project on Travis, you may have problems with OOM errors when using default settings. You can change Elasticsearch memory settings using `withEsJavaOpts` method. Example (from spec `pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticSpec`):

```java
    static EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(TRANSPORT_TCP_PORT, TRANSPORT_TCP_PORT_VALUE)
            .withSetting(CLUSTER_NAME, CLUSTER_NAME_VALUE)
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withIndex(CARS_INDEX_NAME, CARS_INDEX)
            .withIndex(BOOKS_INDEX_NAME, BOOKS_INDEX)
            .withStartTimeout(1, MINUTES)
            .build()
            .start()
```

## Running more then one Elasticsearch instance

There are cases where you might want to run more than one Elasticsearch instance e.g.:
- running tests of one project in parallel (e.g. using _gradle --parallel_ or _mvn -T1C_)
- running tests of different projects on the same physical mashine (e.g. Jenkins jobs running on the same server)
- running integration tests which require more than one Elasticsearch instance

In such situations you should use distinct values for following settings for each instance:

- `withSetting(PopularProperties.TRANSPORT_TCP_PORT, ...)`
- `withSetting(PopularProperties.HTTP_PORT, ...)`
- `withInstallationDirectory(...)`

With such configuration *embedded-elasticsearch* will redownload elasticsearch installation package for every distinct
installation directory. To avoid this behavior and reuse downloaded installation package you should
set common location of downloaded files with `withDownloadDirectory(...)` for every *embedded-elasticsearch* configuration.

## License

*embedded-elasticsearch* is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
