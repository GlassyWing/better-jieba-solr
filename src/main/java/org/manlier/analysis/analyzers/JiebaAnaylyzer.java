package org.manlier.analysis.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.manlier.analysis.jieba.HBaseJiebaTokenizer;
import org.manlier.analysis.jieba.dao.DictSource;

import java.io.IOException;
import java.util.List;

public class JiebaAnaylyzer extends Analyzer {

    private List<DictSource> dictSources;

    public JiebaAnaylyzer(List<DictSource> dictSources) {
        this.dictSources = dictSources;
    }


    @Override
    protected TokenStreamComponents createComponents(String s) {
        try {
            return new TokenStreamComponents(new HBaseJiebaTokenizer(dictSources));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
