package org.manlier.analysis.jieba;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.manlier.analysis.jieba.dao.DictSource;
import org.manlier.analysis.jieba.dao.InputStreamDictSource;
import org.manlier.common.schemes.HBaseJiebaDictQuery;

import java.io.IOException;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HBaseJiebaTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {

    private JiebaSegmenter.SegMode segMode; //  jieba分词模式
    private boolean HMM;                    //  是否启用HMM
    private boolean useDefaultDict;         //  是否使用jieba的默认字典
    private String dictionaries;            //  指定字典文件名，通过solr标准分隔符分开
    private List<DictSource> dictSources = new LinkedList<>();  //  所要用到的所有字典源

    public HBaseJiebaTokenizerFactory(Map<String, String> args) {
        super(args);

        // 默认为search模式
        if (null == get(args, "segMode"))
            segMode = JiebaSegmenter.SegMode.SEARCH;
        else
            segMode = JiebaSegmenter.SegMode.valueOf(get(args, "segMode"));

        //  默认使用jieba内置字典
        if (null == get(args, "useDefaultDict")) {
            useDefaultDict = true;
        } else {
            useDefaultDict = Boolean.valueOf(get(args, "useDefaultDict"));
        }

        //  指定文件类型的字典
        dictionaries = get(args, "dictionaries");

        //  默认开启HMM新词发现
        if (null == get(args, "HMM"))
            HMM = true;
        else
            HMM = Boolean.valueOf(get(args, "HMM"));

        // HBase 连接设置
        String ZKQuorum = get(args, "ZKQuorum");
        String ZKPort = get(args, "ZKPort");
        String ZKZnode = get(args, "ZKZnode");

        BitSet bits = new BitSet(3);
        bits.set(0, ZKQuorum != null);
        bits.set(1, ZKPort != null);
        bits.set(2, ZKZnode != null);
        // 三个配置都需设置，或者都不设置(不设置意味着不使用HBase字典)
        if (bits.cardinality() == 3) {
            Configuration config = HBaseConfiguration.create();
            config.set("hbase.zookeeper.quorum", ZKQuorum);
            config.set("hbase.zookeeper.property.clientPort", ZKPort);
            config.set("zookeeper.znode.parent", ZKZnode);
            HBaseJiebaDictSource dictSource = new HBaseJiebaDictSource(config);
            dictSources.add(dictSource);

            if (null != get(args, "table")) {
                HBaseJiebaDictQuery.TABLE_NAME = get(args, "table");
            }
            if (null != get(args, "CF")) {
                HBaseJiebaDictQuery.INFO_COLUMNFAMILY = Bytes.toBytes(get(args, "CF"));
            }
            if (null != get(args, "WQ")) {
                HBaseJiebaDictQuery.WEIGHT_QUALIFIER = Bytes.toBytes(get(args, "WQ"));
            }
            if (null != get(args, "TQ")) {
                HBaseJiebaDictQuery.TAG_QUALIFIER = Bytes.toBytes(get(args, "TQ"));
            }
        } else if (bits.cardinality() != 0) {
            throw new IllegalArgumentException("Params ZKQuorum, ZKPort and ZKZnode must be set together.");
        }

    }

    @Override
    public Tokenizer create(AttributeFactory attributeFactory) {
        // 必须设置一个字典
        if (dictSources.size() == 0 && !useDefaultDict) {
            throw new IllegalArgumentException(" You disable the default dictionary, you must set a dict source for tokenizer at least.");
        }
        try {
            return new HBaseJiebaTokenizer(dictSources, segMode, useDefaultDict, HMM, attributeFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void inform(ResourceLoader resourceLoader) throws IOException {
        List<String> files = this.splitFileNames(this.dictionaries);
        for (String file : files) {
            dictSources.add(new InputStreamDictSource(resourceLoader.openResource(file)));
        }
    }
}
