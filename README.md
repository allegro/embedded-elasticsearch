# embedded-elasticsearch
Small utility for creating integration tests that uses Elasticsearch. Instead of using `Node` it downloads elastic search in specified version and starts it in seprate process. It also allows you to install required plugins which is not possible when using `NodeBuilder`. Utility was tested with 2.x version of Elasticsearch.

## Introduction

All you need to do to use this tool is create `EmbeddedElastic` instance. To do so, use provided builder:

```
final embeddedElastic = EmbeddedElastic.builder()
        .withElasticVersion("2.2.0")
        .withPortNumber(9300)
        .withClusterName("my_cluster")
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
| `withPortNumber(int portNumber)` | port number on which Elasticsearch will be started |
| `withClusterName(String clusterName)` | cluster name for created Elasticsearch instance |
| `withMapping(InputStream mapping)`, `withMapping(String mapping)` | JSON with mapping of your index |
| `withSettings(InputStream settings)`, `withSettings(String settings)` | JSON with settings of your index |
| `withPlugin(String name, URL urlToDownload)` | plugin that should be installed into Elasticsearch; use multiple times for multiple plugins |
| `withIndex(String indexName, IndexSettings indexSettings)` | specify index that should be created and managed by EmbeddedElastic |

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
| `createClient()` | create transport client withc default settings |

## Example
If you want to see example, look at this spec: `pl.allegro.tech.search.embeddedelasticsearch.EmbeddedElasticSpec`

## License

*embedded-elasticsearch* is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
