# embedded-elasticsearch

![build status](https://travis-ci.org/allegro/embedded-elasticsearch.svg?branch=master)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.allegro.tech/embedded-elasticsearch/badge.svg)

Small utility for creating integration tests that uses Elasticsearch. Instead of using `Node` it downloads elastic search in specified version and starts it in seprate process. It also allows you to install required plugins which is not possible when using `NodeBuilder`. Utility was tested with 1.x, 2.x and 5.x versions of Elasticsearch.

## Introduction

All you need to do to use this tool is create `EmbeddedElastic` instance. To do so, use provided builder:

```
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

When you are done with creating it, just have to simply start it:

```
embeddedElastic.start()
```

And that's all, you can connect to your embedded-elastic instance on specified port and use it in your test.

## Available builder options

| Method | Description |
| ------------- | ------------- |
| `withElasticVersion(String version)` | version of Elasticsearch; based on that version download url to official Elasticsearch repository will be created |
| `withDownloadUrl(URL downloadUrl)` | if you prefer to download Elasticsearch from different location than official repositories you can do that using this method |
| `withSetting(String key, Object value)` | setting name and value as in elasticsearch.yml file |
| `withPlugin(String expression)` | plugin that should be installed into Elasticsearch; treat expression as argument to `./elasticsearch-plugin install <expression>` command; use multiple times for multiple plugins |
| `withIndex(String indexName, IndexSettings indexSettings)` | specify index that should be created and managed by EmbeddedElastic |
| `withStartTimeout(long value, TimeUnit unit)` | specify timeout you give Elasticsearch to start |
| `withEsJavaOpts(String javaOpts)` | value of `ES_JAVA_OPTS` variable to be set for Elasticsearch process |
| `getTransportTcpPort()` | get transport tcp port number used by Elasticsearch instance |
| `getHttpPort()` | get http port number used by Elasticsearch instance |

Available `IndexSettings.Builder` options

| Method | Description |
| ------------- | ------------- |
| `withType(String type, String mapping)` | specify type and it's mappings |
| `withSettings(String settings)` | specify index settings |

## Available operations

`EmbeddedElastic` provide following operations:

| Method | Description |
| ------------- | ------------- |
| `start()` | downloads Elasticsearch and specified plugins, setups everything and finally starts your Elasticsearch instance |
| `stop()` | stops your Elasticsearch instance and removes all data |
| `index` | index your document, comes with variation that takes only document, or document and it's id |
| `deleteIndex(String indexName)`, `deleteIndices()`  | deletes index with name specified during EmbeddedElastic creation |
| `createIndex(String indexName)`, `createIndices()` | creates index with name specified during EmbeddedElastic creation; note that this index is created during EmbeddedElastic startup, you will need this method only if you deleted your index using `deleteIndex` method |  
| `recreateIndex(String indexName)`, `recreateIndices()` | combination of `deleteIndex` and `createIndex` |
| `refreshIndices()` | refresh index; useful when you make changes in different thread, and want to check results instantly in tests |

## Example
If you want to see example, look at this spec: `pl.allegro.tech.search.embeddedelasticsearch.EmbeddedElasticSpec`

## Dependency
To start using embedded-elasticsearch in your project add it as a test dependency:

Gradle:

```
testCompile 'pl.allegro.tech:embedded-elasticsearch:2.1.0'
```

Maven:

```
<dependency>
    <groupId>pl.allegro.tech</groupId>
    <artifactId>embedded-elasticsearch</artifactId>
    <version>2.1.0</version>
    <scope>testCompile</scope>
</dependency>
```

## Known problems
If you build your project on Travis, you may have problems with OOM errors when using default settings. You can change Elasticsearch memory settings using `withEsJavaOpts` method. Example (from spec `pl.allegro.tech.embeddedelasticsearch.EmbeddedElasticSpec`):

```
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

## License

*embedded-elasticsearch* is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
