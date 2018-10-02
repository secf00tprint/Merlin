package de.reinhard.merlin.word.templating;

import de.reinhard.merlin.word.WordDocument;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Runs a template.
 */
public class WordTemplateRunner {
    private Logger log = LoggerFactory.getLogger(WordTemplateRunner.class);
    private TemplateDefinition templateDefinition;
    private WordDocument srcDocument;

    /**
     * @param templateDefinition Bind this Template definition to this Word document. Any template definition read inside the
     *                           Word document will be ignored.
     * @param document
     */
    public WordTemplateRunner(TemplateDefinition templateDefinition, WordDocument document) {
        this.templateDefinition = templateDefinition;
        this.srcDocument = document;
    }

    /**
     * Don't forget to bind the Word template before running {@link #run(Map)}.
     *
     * @param document
     */
    public WordTemplateRunner(WordDocument document) {
        this.srcDocument = document;
    }

    public void setTemplateDefinition(TemplateDefinition templateDefinition) {
        this.templateDefinition = templateDefinition;
    }

    /**
     * Scans the Word file for template definition, such as:
     * <ul>
     * <li>{template.id="of84r3orn3w0jo"} or</li>
     * <li>{template.name="Employee Contract"} or</li>
     * </ul>
     *
     * @return TemplateDefinition only with fields id and or name.
     */
    public TemplateDefinitionReference scanForTemplateDefinitionReference() {
        return srcDocument.scanForTemplateDefinitionReference();
    }

    public WordDocument run(Map<String, Object> variables) {
        ByteArrayOutputStream bos = srcDocument.getAsByteArrayOutputStream();
        WordDocument newDocument = new WordDocument(bos.toInputStream());
        newDocument.setFilename(srcDocument.getFilename());
        if (templateDefinition != null && templateDefinition.getDependentVariableDefinitions() != null) {
            for (DependentVariableDefinition depVar : templateDefinition.getDependentVariableDefinitions()) {
                variables.put(depVar.getName(), depVar.getMappedValue(variables));
            }
        }
        newDocument.process(variables);
        return newDocument;
    }
}
