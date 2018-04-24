package org.manlier.analysis.jieba;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.manlier.analysis.jieba.dao.DictSource;
import org.manlier.analysis.syn.DictSyn;

import java.io.IOException;
import java.util.List;

public class JiebaDictSynchronizer implements DictSyn {
    private Logger logger = Logger.getLogger(getClass());
    private JiebaSegmenter segmenter;
    private List<DictSource> dictSources;

    public JiebaDictSynchronizer(JiebaSegmenter segmenter, List<DictSource> dictSources) {
        this.segmenter = segmenter;
        this.dictSources = dictSources;
    }

    @Override
    public void synDict() throws IOException {
        logger.info("Try to load jieba dict sources");
        for (DictSource dictSource : dictSources) {
            segmenter.loadUserDict(dictSource);
        }
        logger.info("Load jieba dic sources done!");
    }
}
