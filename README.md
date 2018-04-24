# better-jieba-solr

适配[better-jieba](https://github.com/GlassyWing/better-jieba)到 solr 的适配包，可通过 HBase 加载分词库。

## 要求

本适配包要求 Solr5 或以上版本

## 安装

### 安装到 maven 本地仓库

1.  确认你已经安装好了`better-jieba`，若未安装，需要安装[better-jieba](https://github.com/GlassyWing/better-jieba)。
2.  下载该项目到任意位置。
3.  在项目根目录下，执行`mvn install -DskipTests`安装到本地仓库
4.  在其它项目中，通过在`pom.xml`文件中添加以下依赖引用本库：

```xml
<dependency>
    <groupId>org.manlier</groupId>
    <artifactId>better-jieba-solr</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 安装到 solr

1.  在下载的项目根目录下，执行`mvn package -DskipTests`得到 jar 文件：

    ```sh
    better-jieba-solr-<version>.jar
    ```

2.  将`better-jieba-solr-<version>.jar`放在 solr 服务器的运行库中

    运行库相对于 solr 安装目录的位置为
    `server\solr-webapp\webapp\WEB-INF\lib`

    **注意**：为解决兼容性问题，需将 lib 目录下的`protobuf-java-3.1.0.jar`替换为`protobuf-java-2.5.0.jar`

## 使用

你可通过如下方式在 solr 中将 better-jieba 作为分词器，除此之外，本适配包提供以数据库作为同义词库的同义词过滤器：

```xml
    <!-- 处理中文 -->
    <fieldType name="text_cn" class="solr.TextField" positionIncrementGap="100" multiValued="true">
      <analyzer type="index">
        <tokenizer class="org.manlier.analysis.jieba.JiebaTokenizerFactory"
          useDefaultDict="false"
          HMM="false"
          jdbcUrl="jdbc:phoenix:localhost:2181"
          synZKQuorum="localhost:2181"
          synZKPath="/test" />
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt"/>
        <filter class="org.manlier.analysis.filters.SynonymGraphFilterFactory"
          synonyms="synonyms.txt"
          expand="true"
          jdbcUrl="jdbc:phoenix:localhost:2181"
          tableName="THESAURUS_GROUP" />
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
      <analyzer type="query">
        <tokenizer class="org.manlier.analysis.jieba.JiebaTokenizerFactory"
          useDefaultDict="false"
          HMM="false"
          jdbcUrl="jdbc:phoenix:localhost:2181"
          synZKQuorum="localhost:2181"
          synZKPath="/test" />
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt"/>
        <filter class="org.manlier.analysis.filters.SynonymGraphFilterFactory"
          synonyms="synonyms.txt"
          expand="true"
          jdbcUrl="jdbc:phoenix:localhost:2181"
          tableName="THESAURUS_GROUP" />
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
    </fieldType>
```

### 参数说明

* 一般参数

| 参数           | 默认值 | 说明                                                       |
| -------------- | ------ | ---------------------------------------------------------- |
| useDefaultDict | true   | 是否使用 jieba 内置的字典                                  |
| HMM            | true   | 是否开启新词发现                                           |
| segMode        | search | 分词模式，值可为 search(搜索)和 index(索引)                |
| dictionaries   |        | 文件形式的字典文件名，可定义多个字典，以 solr 的分隔符分隔 |

除了文件字典，你可配置数据库作为字典源

* 数据库相关参数

| 参数      | 默认值 | 说明                                                         |
| --------- | ------ | ------------------------------------------------------------ |
| jdbcUrl   |        | 连接 数据库 所使用的 jdbcUrl 连接                            |
| tableName |        | 词库所在的表，表中前三个字段类型需为（Integer,String,String)即（单词，词频，词性） |

* 字典同步参数

字典同步借用 Zookeeper 实现，因此要使用同步功能，请确保已配置好 Zookeeper 服务。
**注意**：一旦注明以下参数，则认为该字典需要进行同步，参数也适用于本适配包提供的同义词过滤工厂。

| 参数        | 默认值 | 说明                                     |
| ----------- | ------ | ---------------------------------------- |
| synZKQuorum |        | zookeeper 链接                           |
| synZKPath   |        | 指定 znode 用于保存同步状态，如(`/test`) |

> 同步状态转移说明：
> 一旦其它进程更改`synZKPath`指定的znode的值，使其成为`DICT_SYN_REQ`，则会自动重载字典。重载完成后会自动更新`synZKPath`指定的znode的值成为`DICT_SYN_DONE`。其它进程可监听`synZKPath`的变化。

### solr 配置示例

你可在本项目的`example`目录下找到示例配置集合。
