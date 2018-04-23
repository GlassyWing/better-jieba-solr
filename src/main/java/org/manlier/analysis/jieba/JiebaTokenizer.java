package org.manlier.analysis.jieba;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.SegmentingTokenizerBase;
import org.apache.lucene.util.AttributeFactory;
import org.manlier.analysis.jieba.dao.DictSource;
import org.manlier.analysis.syn.DictSyn;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.manlier.analysis.jieba.JiebaSegmenter.*;

/**
 * 自定义jieba分词器
 */
public final class JiebaTokenizer extends SegmentingTokenizerBase {
    private static final BreakIterator sentenceProto;
    private final JiebaSegmenter segmenter;
    private final SegMode segMode;
    private final boolean HMM;
    private Iterator<SegToken> tokens;
    private final CharTermAttribute termAttr;
    private final OffsetAttribute offsetAttr;

    public JiebaTokenizer(JiebaSegmenter segmenter
            , SegMode segMode
            , boolean HMM
            , AttributeFactory factory) {
        super(factory, (BreakIterator) sentenceProto.clone());

        this.segmenter = segmenter;
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
