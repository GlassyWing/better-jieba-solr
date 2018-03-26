package org.manlier.analysis.jieba;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.SegmentingTokenizerBase;
import org.apache.lucene.util.AttributeFactory;
import org.manlier.analysis.jieba.JiebaSegmenter;
import org.manlier.analysis.jieba.SegToken;
import org.manlier.analysis.jieba.dao.DictSource;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.manlier.analysis.jieba.JiebaSegmenter.*;

/**
 * 自定义jieba分词器
 */
public final class HBaseJiebaTokenizer extends SegmentingTokenizerBase {
    private static final BreakIterator sentenceProto;
    private final JiebaSegmenter segmenter;
    private final SegMode segMode;
    private final boolean HMM;
    private Iterator<SegToken> tokens;
    private final CharTermAttribute termAttr;
    private final OffsetAttribute offsetAttr;

    public HBaseJiebaTokenizer() throws IOException {
        this(null, SegMode.SEARCH, true, true, DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
    }

    public HBaseJiebaTokenizer(List<DictSource> dictSources) throws IOException {
        this(dictSources, SegMode.SEARCH, true, true, DEFAULT_TOKEN_ATTRIBUTE_FACTORY);
    }

    public HBaseJiebaTokenizer(List<DictSource> dictSources
            , SegMode segMode, boolean useDefaultDict, boolean HMM
            , AttributeFactory factory) throws IOException {
        super(factory, (BreakIterator) sentenceProto.clone());
        if (!useDefaultDict) {
            System.setProperty("jieba.defaultDict", "false");
        }
        segmenter = new JiebaSegmenter();
        if (dictSources != null) {
            for (DictSource dictSource : dictSources) {
                segmenter.loadUserDict(dictSource);
            }
        }
        this.segMode = segMode;
        this.HMM = HMM;
        this.termAttr = addAttribute(CharTermAttribute.class);
        this.offsetAttr = addAttribute(OffsetAttribute.class);
    }


    @Override
    public void reset() throws IOException {
        super.reset();
        this.tokens = null;
    }

    @Override
    protected void setNextSentence(int sentenceStart, int sentenceEnd) {
        String sentence = new String(this.buffer, sentenceStart, sentenceEnd - sentenceStart);
        this.tokens = this.segmenter.process(sentence, segMode, HMM).iterator();
    }

    @Override
    protected boolean incrementWord() {
        if (tokens != null && tokens.hasNext()) {
            SegToken token = tokens.next();
            this.clearAttributes();
            this.termAttr.append(token.word);
            this.offsetAttr.setOffset(token.startOffset, token.endOffset);
            return true;
        }
        return false;
    }

    static {
        sentenceProto = BreakIterator.getSentenceInstance(Locale.ROOT);
    }
}
