//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.manlier.analysis.filters;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.RollingBuffer;
import org.apache.lucene.util.RollingBuffer.Resettable;
import org.apache.lucene.util.fst.FST.Arc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class SynonymGraphFilter extends TokenFilter {
    public static final String TYPE_SYNONYM = "SYNONYM";
    private final CharTermAttribute termAtt = (CharTermAttribute) this.addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = (PositionIncrementAttribute) this.addAttribute(PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAtt = (PositionLengthAttribute) this.addAttribute(PositionLengthAttribute.class);
    private final TypeAttribute typeAtt = (TypeAttribute) this.addAttribute(TypeAttribute.class);
    private final OffsetAttribute offsetAtt = (OffsetAttribute) this.addAttribute(OffsetAttribute.class);
    private final SynonymGraphFilterFactory.SynonymMapGetter synonymMapGetter;
    private boolean ignoreCase;
    private Arc<BytesRef> scratchArc;
    private final ByteArrayDataInput bytesReader = new ByteArrayDataInput();
    private final BytesRef scratchBytes = new BytesRef();
    private final CharsRefBuilder scratchChars = new CharsRefBuilder();
    private final LinkedList<SynonymGraphFilter.BufferedOutputToken> outputBuffer = new LinkedList();
    private int nextNodeOut;
    private int lastNodeOut;
    private int maxLookaheadUsed;
    private int captureCount;
    private boolean liveToken;
    private int matchStartOffset;
    private int matchEndOffset;
    private boolean finished;
    private int lookaheadNextRead;
    private int lookaheadNextWrite;
    private RollingBuffer<SynonymGraphFilter.BufferedInputToken> lookahead = new RollingBuffer<SynonymGraphFilter.BufferedInputToken>() {
        protected SynonymGraphFilter.BufferedInputToken newInstance() {
            return new SynonymGraphFilter.BufferedInputToken();
        }
    };

    public SynonymGraphFilter(TokenStream input, SynonymGraphFilterFactory.SynonymMapGetter synonymMapGetter, boolean ignoreCase) {
        super(input);
        this.synonymMapGetter = synonymMapGetter;
        if (this.synonymMapGetter.getSynonymMap().fst == null) {
            throw new IllegalArgumentException("fst must be non-null");
        } else {
            this.scratchArc = new Arc();
            this.ignoreCase = ignoreCase;
        }
    }


    public boolean incrementToken() throws IOException {
        assert this.lastNodeOut <= this.nextNodeOut;

        if (!this.outputBuffer.isEmpty()) {
            this.releaseBufferedToken();

            assert !this.liveToken;

            return true;
        } else if (this.parse()) {
            this.releaseBufferedToken();

            assert !this.liveToken;

            return true;
        } else {
            if (this.lookaheadNextRead == this.lookaheadNextWrite) {
                if (this.finished) {
                    return false;
                }

                assert this.liveToken;

                this.liveToken = false;
            } else {
                assert this.lookaheadNextRead < this.lookaheadNextWrite : "read=" + this.lookaheadNextRead + " write=" + this.lookaheadNextWrite;

                SynonymGraphFilter.BufferedInputToken token = this.lookahead.get(this.lookaheadNextRead);
                ++this.lookaheadNextRead;
                this.restoreState(token.state);
                this.lookahead.freeBefore(this.lookaheadNextRead);

                assert !this.liveToken;
            }

            this.lastNodeOut += this.posIncrAtt.getPositionIncrement();
            this.nextNodeOut = this.lastNodeOut + this.posLenAtt.getPositionLength();
            return true;
        }
    }

    private void releaseBufferedToken() {
        SynonymGraphFilter.BufferedOutputToken token = this.outputBuffer.pollFirst();
        if (token.state != null) {
            this.restoreState(token.state);
        } else {
            this.clearAttributes();
            this.termAtt.append(token.term);

            assert this.matchStartOffset != -1;

            this.offsetAtt.setOffset(this.matchStartOffset, this.matchEndOffset);
            this.typeAtt.setType("SYNONYM");
        }

        this.posIncrAtt.setPositionIncrement(token.startNode - this.lastNodeOut);
        this.lastNodeOut = token.startNode;
        this.posLenAtt.setPositionLength(token.endNode - token.startNode);
    }

    private boolean parse() throws IOException {
        BytesRef matchOutput = null;
        int matchInputLength = 0;
        BytesRef pendingOutput = synonymMapGetter.getSynonymMap().fst.outputs.getNoOutput();
        synonymMapGetter.getSynonymMap().fst.getFirstArc(this.scratchArc);

        assert this.scratchArc.output == synonymMapGetter.getSynonymMap().fst.outputs.getNoOutput();

        int matchLength = 0;
        boolean doFinalCapture = false;
        int lookaheadUpto = this.lookaheadNextRead;
        this.matchStartOffset = -1;

        label93:
        while (true) {
            char[] buffer;
            int bufferLen;
            int inputEndOffset;
            if (lookaheadUpto <= this.lookahead.getMaxPos()) {
                SynonymGraphFilter.BufferedInputToken token = this.lookahead.get(lookaheadUpto);
                ++lookaheadUpto;
                buffer = token.term.chars();
                bufferLen = token.term.length();
                inputEndOffset = token.endOffset;
                if (this.matchStartOffset == -1) {
                    this.matchStartOffset = token.startOffset;
                }
            } else {
                assert this.finished || !this.liveToken;

                if (this.finished) {
                    break;
                }

                if (!this.input.incrementToken()) {
                    this.finished = true;
                    break;
                }

                this.liveToken = true;
                buffer = this.termAtt.buffer();
                bufferLen = this.termAtt.length();
                if (this.matchStartOffset == -1) {
                    this.matchStartOffset = this.offsetAtt.startOffset();
                }

                inputEndOffset = this.offsetAtt.endOffset();
                ++lookaheadUpto;
            }

            ++matchLength;

            int codePoint;
            int bufUpto;
            for (bufUpto = 0; bufUpto < bufferLen; bufUpto += Character.charCount(codePoint)) {
                codePoint = Character.codePointAt(buffer, bufUpto, bufferLen);
                if (synonymMapGetter.getSynonymMap().fst.findTargetArc(this.ignoreCase ? Character.toLowerCase(codePoint) : codePoint, this.scratchArc, this.scratchArc, synonymMapGetter.getSynonymMap().fst.getBytesReader()) == null) {
                    break label93;
                }

                pendingOutput = synonymMapGetter.getSynonymMap().fst.outputs.add(pendingOutput, this.scratchArc.output);
            }

            assert bufUpto == bufferLen;

            if (this.scratchArc.isFinal()) {
                matchOutput = synonymMapGetter.getSynonymMap().fst.outputs.add(pendingOutput, this.scratchArc.nextFinalOutput);
                matchInputLength = matchLength;
                this.matchEndOffset = inputEndOffset;
            }

            if (synonymMapGetter.getSynonymMap().fst.findTargetArc(0, this.scratchArc, this.scratchArc, synonymMapGetter.getSynonymMap().fst.getBytesReader()) == null) {
                break;
            }

            pendingOutput = synonymMapGetter.getSynonymMap().fst.outputs.add(pendingOutput, this.scratchArc.output);
            doFinalCapture = true;
            if (this.liveToken) {
                this.capture();
            }
        }

        if (doFinalCapture && this.liveToken && !this.finished) {
            this.capture();
        }

        if (matchOutput != null) {
            if (this.liveToken) {
                this.capture();
            }

            this.bufferOutputTokens(matchOutput, matchInputLength);
            this.lookaheadNextRead += matchInputLength;
            this.lookahead.freeBefore(this.lookaheadNextRead);
            return true;
        } else {
            return false;
        }
    }


    private void bufferOutputTokens(BytesRef bytes, int matchInputLength) {
        this.bytesReader.reset(bytes.bytes, bytes.offset, bytes.length);
        int code = this.bytesReader.readVInt();
        boolean keepOrig = (code & 1) == 0;
        int totalPathNodes;
        if (keepOrig) {
            assert matchInputLength > 0;

            totalPathNodes = matchInputLength - 1;
        } else {
            totalPathNodes = 0;
        }

        int count = code >>> 1;
        List<List<String>> paths = new ArrayList();

        int startNode;
        int endNode;
        int newNodeCount;
        int i;

        int lastNode;
        for (startNode = 0; startNode < count; ++startNode) {
            endNode = this.bytesReader.readVInt();
            synonymMapGetter.getSynonymMap().words.get(endNode, this.scratchBytes);
            this.scratchChars.copyUTF8Bytes(this.scratchBytes);
            newNodeCount = 0;
            List<String> path = new ArrayList();
            paths.add(path);
            i = this.scratchChars.length();

            for (lastNode = 0; lastNode <= i; ++lastNode) {
                if (lastNode == i || this.scratchChars.charAt(lastNode) == 0) {
                    path.add(new String(this.scratchChars.chars(), newNodeCount, lastNode - newNodeCount));
                    newNodeCount = 1 + lastNode;
                }
            }

            assert path.size() > 0;

            totalPathNodes += path.size() - 1;
        }

        startNode = this.nextNodeOut;
        endNode = startNode + totalPathNodes + 1;
        newNodeCount = 0;

        List path;
        for (Iterator var15 = paths.iterator(); var15.hasNext(); this.outputBuffer.add(new SynonymGraphFilter.BufferedOutputToken(null, (String) path.get(0), startNode, lastNode))) {
            path = (List) var15.next();
            if (path.size() == 1) {
                lastNode = endNode;
            } else {
                lastNode = this.nextNodeOut + newNodeCount + 1;
                newNodeCount += path.size() - 1;
            }
        }

        if (keepOrig) {
            SynonymGraphFilter.BufferedInputToken token = this.lookahead.get(this.lookaheadNextRead);
            if (matchInputLength == 1) {
                i = endNode;
            } else {
                i = this.nextNodeOut + newNodeCount + 1;
            }

            this.outputBuffer.add(new SynonymGraphFilter.BufferedOutputToken(token.state, token.term.toString(), startNode, i));
        }

        this.nextNodeOut = endNode;

        for (lastNode = 0; lastNode < paths.size(); ++lastNode) {
            path = paths.get(lastNode);
            if (path.size() > 1) {
                lastNode = this.outputBuffer.get(lastNode).endNode;

                for (i = 1; i < path.size() - 1; ++i) {
                    this.outputBuffer.add(new SynonymGraphFilter.BufferedOutputToken(null, (String) path.get(i), lastNode, lastNode + 1));
                    ++lastNode;
                }

                this.outputBuffer.add(new SynonymGraphFilter.BufferedOutputToken(null, (String) path.get(path.size() - 1), lastNode, endNode));
            }
        }

        if (keepOrig && matchInputLength > 1) {
            lastNode = this.outputBuffer.get(paths.size()).endNode;

            for (i = 1; i < matchInputLength - 1; ++i) {
                SynonymGraphFilter.BufferedInputToken token = this.lookahead.get(this.lookaheadNextRead + i);
                this.outputBuffer.add(new SynonymGraphFilter.BufferedOutputToken(token.state, token.term.toString(), lastNode, lastNode + 1));
                ++lastNode;
            }

            SynonymGraphFilter.BufferedInputToken token = this.lookahead.get(this.lookaheadNextRead + matchInputLength - 1);
            this.outputBuffer.add(new SynonymGraphFilter.BufferedOutputToken(token.state, token.term.toString(), lastNode, endNode));
        }

    }

    private void capture() {
        assert this.liveToken;

        this.liveToken = false;
        SynonymGraphFilter.BufferedInputToken token = this.lookahead.get(this.lookaheadNextWrite);
        ++this.lookaheadNextWrite;
        token.state = this.captureState();
        token.startOffset = this.offsetAtt.startOffset();
        token.endOffset = this.offsetAtt.endOffset();

        assert token.term.length() == 0;

        token.term.append(this.termAtt);
        ++this.captureCount;
        this.maxLookaheadUsed = Math.max(this.maxLookaheadUsed, this.lookahead.getBufferSize());
    }

    public void reset() throws IOException {
        super.reset();
        this.lookahead.reset();
        this.lookaheadNextWrite = 0;
        this.lookaheadNextRead = 0;
        this.captureCount = 0;
        this.lastNodeOut = -1;
        this.nextNodeOut = 0;
        this.matchStartOffset = -1;
        this.matchEndOffset = -1;
        this.finished = false;
        this.liveToken = false;
        this.outputBuffer.clear();
        this.maxLookaheadUsed = 0;
    }

    int getCaptureCount() {
        return this.captureCount;
    }

    int getMaxLookaheadUsed() {
        return this.maxLookaheadUsed;
    }

    static class BufferedOutputToken {
        final String term;
        final State state;
        final int startNode;
        final int endNode;

        public BufferedOutputToken(State state, String term, int startNode, int endNode) {
            this.state = state;
            this.term = term;
            this.startNode = startNode;
            this.endNode = endNode;
        }
    }

    static class BufferedInputToken implements Resettable {
        final CharsRefBuilder term = new CharsRefBuilder();
        State state;
        int startOffset = -1;
        int endOffset = -1;

        BufferedInputToken() {
        }

        public void reset() {
            this.state = null;
            this.term.clear();
            this.startOffset = -1;
            this.endOffset = -1;
        }
    }


}
