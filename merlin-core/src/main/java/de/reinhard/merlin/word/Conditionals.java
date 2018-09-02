package de.reinhard.merlin.word;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;

public class Conditionals {
    private Logger log = LoggerFactory.getLogger(Conditionals.class);
    private SortedSet<Conditional> conditionals;
    WordDocument document;

    private Map<Integer, IBodyElement> bodyElementsMap;

    Conditionals(WordDocument document) {
        this.document = document;
    }

    /**
     * Parses all conditionals and build also a conditional tree.
     */
    void read() {
        List<IBodyElement> elements = document.getDocument().getBodyElements();
        conditionals = new TreeSet<>();
        bodyElementsMap = new HashMap<>();
        SortedSet<DocumentRange> allControls = new TreeSet<>();
        Map<DocumentRange, Conditional> conditionalMap = new HashMap<>();
        int bodyElementCounter = 0;
        for (IBodyElement element : elements) {
            bodyElementsMap.put(bodyElementCounter, element);
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                List<XWPFRun> runs = paragraph.getRuns();
                if (runs != null) {
                    read(runs, bodyElementCounter, allControls, conditionalMap);
                }
            }
            ++bodyElementCounter;
        }
        Conditional current = null;
        for (DocumentRange range : allControls) {
            Conditional conditional = conditionalMap.get(range);
            if (conditional != null) {
                log.debug("Processing conditional: " + conditional);
                // If-expression:
                if (current != null) {
                    // This is a child if-expression of current.
                    conditional.setParent(current);
                }
                current = conditional; // Set child as current.
            } else {
                log.debug("Processing endif: " + range);
                // endif-expression:
                if (current == null) {
                    log.error("endif without if-expression found. Ignoring it.");
                } else {
                    current.setEndif(range);
                    current = current.getParent(); // May-be null.
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Conditionals:");
            for (Conditional conditional : conditionals) {
                log.debug("Conditional: " + conditional);
            }
        }
    }

    void process(Map<String, ?> variables) {
        for (Conditional conditional : conditionals) {
            if (conditional.getParent() != null) {
                // Process only top level conditionals. The childs will be processed by its parent.
                continue;
            }
            process(conditional, variables);
        }
        document.removeMarkedParagraphs();
    }

    void process(Conditional conditional, Map<String, ?> variables) {
        if (conditional.matches(variables) == false) {
            // Remove all content covered by this conditional.
            removeRange(conditional.getRange());
        } else {
            removeRange(conditional.getEndifExpressionRange());
            if (conditional.getChildConditionals() != null) {
                for (Conditional child : conditional.getChildConditionals()) {
                    process(child, variables);
                }
            }
            removeRange(conditional.getIfExpressionRange());
        }
    }

    private void removeRange(DocumentRange removeRange) {
        int fromNo = removeRange.getStartPosition().getBodyElementNumber();
        int toNo = removeRange.getEndPosition().getBodyElementNumber();
        for (int elementNo = fromNo; elementNo <= toNo; elementNo++) {
            IBodyElement element = bodyElementsMap.get(elementNo);
            if (element instanceof XWPFParagraph) {
                if (elementNo == fromNo) {
                    RunsProcessor processor = new RunsProcessor(((XWPFParagraph) element).getRuns());
                    if (elementNo == toNo) {
                        processor.replaceText(removeRange.getStartPosition(), removeRange.getEndPosition(), "");
                    } else {
                        processor.replaceText(removeRange.getStartPosition(), processor.getEnd(elementNo), "");
                    }
                } else if (elementNo == toNo) {
                    RunsProcessor processor = new RunsProcessor(((XWPFParagraph) element).getRuns());
                    processor.replaceText(new DocumentPosition(elementNo, 0, 0), removeRange.getEndPosition(), "");
                } else {
                    document.markParagraphToRemove(((XWPFParagraph) element));
                }
            } else {
                // Not yet supported.
            }
        }
    }

    private void read(List<XWPFRun> runs, int bodyElementNumber, SortedSet<DocumentRange> allControls,
                      Map<DocumentRange, Conditional> conditionalMap) {
        RunsProcessor processor = new RunsProcessor(runs);
        String text = processor.getText();
        Matcher beginMatcher = Conditional.beginIfPattern.matcher(text);
        while (beginMatcher.find()) {
            Conditional conditional = new Conditional(beginMatcher, bodyElementNumber, processor);
            conditionals.add(conditional);
            allControls.add(conditional.getIfExpressionRange());
            conditionalMap.put(conditional.getIfExpressionRange(), conditional);
        }
        Matcher endMatcher = Conditional.endIfPattern.matcher(text);
        while (endMatcher.find()) {
            DocumentPosition endifStart = processor.getRunIdxAndPosition(bodyElementNumber, endMatcher.start());
            DocumentPosition endifEnd = processor.getRunIdxAndPosition(bodyElementNumber, endMatcher.end() - 1);
            DocumentRange range = new DocumentRange(endifStart, endifEnd);
            allControls.add(range);
        }
    }

    private void process(List<XWPFRun> runs, int bodyElementNumber) {
        RunsProcessor processor = new RunsProcessor(runs);
        String text = processor.getText();

    }

    public SortedSet<Conditional> getConditionals() {
        return conditionals;
    }
}