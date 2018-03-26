# better-jieba-solr

适配[better-jieba](https://github.com/GlassyWing/better-jieba)到solr的适配包，可通过HBase加载分词库。

## 要求

本适配包要求Solr5或以上版本

## 安装

### 安装到maven本地仓库

1. 确认你已经安装好了`better-jieba`，若未安装，需要安装[better-jieba](https://github.com/GlassyWing/better-jieba)。
2. 下载该项目到任意位置。
3. 在项目根目录下，执行`mvn install -DskipTests`安装到本地仓库
4. 在其它项目中，通过在`pom.xml`文件中添加以下依赖引用本库：

```xml
<dependency>
    <groupId>org.manlier</groupId>
    <artifactId>better-jieba-solr</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 安装到solr

1. 在下载的项目根目录下，执行`mvn package -DskipTests`得到jar文件：

    ```sh
    better-jieba-solr-<version>.jar
    ```

2. 将`better-jieba-solr-<version>.jar`放在solr服务器的运行库中

    运行库相对于solr安装目录的位置为
    `server\solr-webapp\webapp\WEB-INF\lib`

    **注意**：为解决兼容性问题，需将lib目录下的`protobuf-java-3.1.0.jar`替换为`protobuf-java-2.5.0.jar`

## 使用

你可通过如下方式在solr中将better-jieba作为分词器：

```xml
    <!-- 处理中文 -->
    <fieldType name="text_jieba" class="solr.TextField" positionIncrementGap="100" multiValued="true">
      <analyzer type="index">
       <tokenizer class="org.manlier.analysis.jieba.HBaseJiebaTokenizerFactory"
          useDefaultDict="false"
          HMM="false"
          ZKQuorum="node1,node2,node3"
          ZKPort="2181"
          ZKZnode="/hbase-unsecure"/>
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt"/>
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
      <analyzer type="query">
        <tokenizer class="solr.WhitespaceTokenizerFactory"/>
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" />
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
    </fieldType>
```

### 参数说明

一般参数

| 参数           | 默认值 | 说明                                           |
| -------------- | ------ | ---------------------------------------------- |
| useDefaultDict | true   | 是否使用jieba内置的字典                        |
| HMM            | true   | 是否开启新词发现                               |
| segMode        | search | 分词模式，值可为search(搜索)和index(索引)      |
| dictionaries   |        | 字典文件名，可定义多个字典，以solr的分隔符分隔 |

除了文件字典，你可配置HBase作为字典源

HBase相关参数

| 参数     | 默认值     | 说明                               |
| -------- | ---------- | ---------------------------------- |
| ZKQuorum |            | 连接HBase所使用的zookeeper集群地址 |
| ZKPort   |            | 连接HBase所使用的zookeeper端口     |
| ZKZnode  |            | HBase的znode                       |
| table    | jieba_dict | 字典的表名                         |
| CF       | info       | 列簇名                             |
| WQ       | weight     | 词权重限定符（存储词的权重）       |
| TQ       | tag        | 词性限定符（存储词性）             |

### solr配置示例

你可在本项目的`example`目录下找到示例配置集合。