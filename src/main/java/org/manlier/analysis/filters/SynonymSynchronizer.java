package org.manlier.analysis.filters;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.manlier.analysis.syn.DictSyn;

import java.io.IOException;
import java.text.ParseException;

public class SynonymSynchronizer implements DictSyn {
    private Logger logger = Logger.getLogger(getClass());

    private String formatClass;
    private ResourceLoader resourceLoader;

    private SynonymGraphFilterFactory factory;

    public SynonymSynchronizer(String formatClass
            , SynonymGraphFilterFactory filterFactory
            , ResourceLoader resourceLoader
    ) {
        this.formatClass = formatClass;
        this.factory = filterFactory;
        this.resourceLoader = resourceLoader;
        logger.info(formatClass);
        logger.info(resourceLoader.toString());
    }

    @Override
    public void synDict() throws IOException {
        try {
            factory.setSynonyms(factory.loadSynonyms(resourceLoader, formatClass, true, factory.getAnalyzer(resourceLoader)));
        } catch (IOException | ParseException e) {
            logger.error("", e);
        }
    }
}
