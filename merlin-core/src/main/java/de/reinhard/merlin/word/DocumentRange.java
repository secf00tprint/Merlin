package de.reinhard.merlin.word;

import org.apache.commons.lang.builder.CompareToBuilder;

/**
 * Localizes an object inside a word document.
 */
public class DocumentRange implements Comparable<DocumentRange> {
    private DocumentPosition startPosition;
    private DocumentPosition endPosition;

    DocumentRange(DocumentPosition startPosition, DocumentPosition endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        if (this.endPosition != null && startPosition.compareTo(endPosition) > 0) {
            throw new IllegalStateException("End position should be after start position: start=" + startPosition + ", end=" + endPosition);
        }
    }

    @Override
    public String toString() {
        return "[range=[start=" + startPosition + ", end=" + endPosition + "]";
    }

    public DocumentPosition getStartPosition() {
        return startPosition;
    }

    public DocumentPosition getEndPosition() {
        return endPosition;
    }

    @Override
    public int compareTo(DocumentRange o) {
        return this.startPosition.compareTo(o.startPosition);
    }
}
