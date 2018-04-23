package org.manlier.analysis.filters;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.manlier.analysis.syn.DictSyn;

import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;

public class SynonymSynchronizer implements DictSyn {
    private Logger logger = Logger.getLogger(getClass());

    private SynonymMap synonymMap;
    private String formatClass;
    private ResourceLoader resourceLoader;

    private SynonymGraphFilterFactory factory;

    public SynonymSynchronizer(SynonymMap synonymMap
            , String formatClass
            , SynonymGraphFilterFactory filterFactory
            , ResourceLoader resourceLoader
    ) {
        this.synonymMap = synonymMap;
        this.formatClass = formatClass;
        this.factory = filterFactory;
        this.resourceLoader = resourceLoader;
        logger.info(formatClass);
        logger.info(resourceLoader.toString());
    }

    @Override
    public void synDict() throws IOException {
        try {
            synonymMap = factory.loadSynonyms(resourceLoader, formatClass, true, factory.getAnalyzer(resourceLoader));
        } catch (IOException | ParseException e) {
            logger.error("", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynonymSynchronizer that = (SynonymSynchronizer) o;
        return Objects.equals(logger, that.logger) &&
                Objects.equals(synonymMap, that.synonymMap) &&
                Objects.equals(formatClass, that.formatClass) &&
                Objects.equals(resourceLoader, that.resourceLoader) &&
                Objects.equals(factory, that.factory);
    }

    @Override
    public int hashCode() {

        return Objects.hash(logger, synonymMap, formatClass, resourceLoader, factory);
    }
}
