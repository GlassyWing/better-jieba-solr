package org.manlier.analysis.jieba;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.manlier.analysis.jieba.dao.DictSource;
import org.manlier.analysis.jieba.dao.InputStreamDictSource;
import org.manlier.analysis.syn.DictStateSynService;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JiebaTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {

    private JiebaSegmenter.SegMode segMode; //  jieba分词模式
    private boolean HMM;                    //  是否启用HMM
    private String dictionaries;            //  指定字典文件名，通过solr标准分隔符分开
    private List<DictSource> dictSources = new LinkedList<>();  //  所要用到的所有字典源
    private JiebaSegmenter segmenter;

    public JiebaTokenizerFactory(Map<String, String> args) throws IOException {
        super(args);

        // 默认为search模式
        if (null == get(args, "segMode"))
            segMode = JiebaSegmenter.SegMode.SEARCH;
        else
            segMode = JiebaSegmenter.SegMode.valueOf(get(args, "segMode"));

        //  默认使用jieba内置字典
        //  是否使用jieba的默认字典
        boolean useDefaultDict;
        if (null == get(args, "useDefaultDict")) {
            useDefaultDict = true;
        } else {
            System.setProperty("jieba.defaultDict", "false");
            useDefaultDict = Boolean.valueOf(get(args, "useDefaultDict"));
        }

        //  指定文件类型的字典
        dictionaries = get(args, "dictionaries");

        //  默认开启HMM新词发现
        if (null == get(args, "HMM"))
            HMM = true;
        else
            HMM = Boolean.valueOf(get(args, "HMM"));

        // jdbc 连接设置
        String jdbcUrl = get(args, "jdbcUrl");

        // Jdbc链接
        if (jdbcUrl != null) {
            String tableName = get(args, "tableName");
            JiebaDBDictSource dictSource;
            if (tableName == null) {
                dictSource = new JiebaDBDictSource(jdbcUrl);
            } else {
                dictSource = new JiebaDBDictSource(jdbcUrl, tableName);
            }
            dictSources.add(dictSource);
        }

        // 必须设置一个字典
        if (dictSources.size() == 0 && !useDefaultDict) {
            throw new IllegalArgumentException(" You disable the default dictionary, you must set a dict source for tokenizer at least.");
        }

        segmenter = new JiebaSegmenter();
        for (DictSource dictSource : dictSources) {
            segmenter.loadUserDict(dictSource);
        }

        String synZKQuorum = get(args, "synZKQuorum");
        String synZKPath = get(args, "synZKPath");

        // 如果需要进行同步
        if (synZKQuorum != null && synZKPath != null) {
            DictStateSynService.getInstance().init(synZKQuorum, synZKPath);
            DictStateSynService.getInstance().addDictNeedToSyn(new JiebaDictSynchronizer(segmenter, dictSources));
        }

    }

    @Override
    public Tokenizer create(AttributeFactory attributeFactory) {
        return new JiebaTokenizer(segmenter, segMode, HMM, attributeFactory);
    }

    @Override
    public void inform(ResourceLoader resourceLoader) throws IOException {
        List<String> files = this.splitFileNames(this.dictionaries);
        for (String file : files) {
            dictSources.add(new InputStreamDictSource(resourceLoader.openResource(file)));
        }
    }
}
