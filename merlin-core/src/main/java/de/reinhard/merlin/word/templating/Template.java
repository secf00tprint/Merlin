package de.reinhard.merlin.word.templating;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A template refers a template file, optional a template definition file and contains some meta data (such as statistics
 * about variables and their usage).
 * TODO: If no template definition is given, serve a pseudo one (for the rest clients).
 */
public class Template {
    private Logger log = LoggerFactory.getLogger(Template.class);
    private TemplateStatistics statistics;
    private TemplateDefinition templateDefinition;
    private FileDescriptor fileDescriptor;

    public Template() {
        statistics = new TemplateStatistics(this);
    }

    public TemplateDefinition getTemplateDefinition() {
        return templateDefinition;
    }

    public TemplateStatistics getStatistics() {
        return statistics;
    }

    /**
     * Please use {@link #assignTemplateDefinition(TemplateDefinition)} for updating statistics (unused variables etc.) or
     * don't forget to call {@link #updateStatistics()}.
     * @param templateDefinition
     */
    public void setTemplateDefinition(TemplateDefinition templateDefinition) {
        this.templateDefinition = templateDefinition;
    }

    public void assignTemplateDefinition(TemplateDefinition templateDefinition) {
        setTemplateDefinition(templateDefinition);
        updateStatistics();
    }

    /**
     * Analyzes used variables by this template and compares it to the defined variables in the given templateDefinition.
     */
    public void updateStatistics() {
        statistics.updateStatistics();
    }

    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public void setFileDescriptor(FileDescriptor fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }

    public String getTemplateDefinitionId() {
        return templateDefinition != null ? templateDefinition.getId() : null;
    }

    @Override
    public String toString() {
        ToStringBuilder tos = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        tos.append("fileDescriptor", fileDescriptor);
        tos.append("templateDefinitionId", getTemplateDefinitionId());
        tos.append("statistics", statistics);
        return tos.toString();
    }
}
