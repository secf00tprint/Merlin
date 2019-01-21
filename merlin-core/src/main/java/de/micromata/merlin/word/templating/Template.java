package de.micromata.merlin.word.templating;

import de.micromata.merlin.persistency.FileDescriptor;
import de.micromata.merlin.persistency.FileDescriptorInterface;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A template refers a template file, optional a template definition file and contains some meta data (such as statistics
 * about variables and their usage).
 */
public class Template implements Cloneable, FileDescriptorInterface {
    private Logger log = LoggerFactory.getLogger(Template.class);
    private String id;
    private TemplateStatistics statistics;
    private TemplateDefinition templateDefinition;
    private String templateDefinitionId;
    private FileDescriptor fileDescriptor;
    private String templateDefinitionReferenceId;

    public Template() {
        statistics = new TemplateStatistics(this);
    }

    /**
     * Template id definined inside Word document (if given): {@code {id = “Employment contract template“}}.
     * This id is useful for referencing templates in serial template definitions.
     * @return id if specified inside the Word template.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return id if specified in Word template or filename if given or primary key.
     * @see #getId()
     * @see FileDescriptor#getFilename()
     * @see #getPrimaryKey()
     */
    public String getDisplayName() {
        if (StringUtils.isNotBlank(id)) return id;
        if (fileDescriptor != null) return fileDescriptor.getFilename();
        return getPrimaryKey();
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
     *
     * @param templateDefinition The template definition to set.
     */
    public void setTemplateDefinition(TemplateDefinition templateDefinition) {
        this.templateDefinition = templateDefinition;
    }

    public void assignTemplateDefinition(TemplateDefinition templateDefinition) {
        setTemplateDefinition(templateDefinition);
        updateStatistics();
    }

    /**
     *
     * @return The template definition reference if defined in Word document like {@code ${templateDefinition.refid = "..."}},
     * otherwise null.
     */
    public String getTemplateDefinitionReferenceId() {
        return templateDefinitionReferenceId;
    }

    public void setTemplateDefinitionReferenceId(String templateDefinitionReferenceId) {
        this.templateDefinitionReferenceId = templateDefinitionReferenceId;
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
        if (templateDefinition != null) {
            return templateDefinition.getId();
        }
        return templateDefinitionId;
    }

    public void setTemplateDefinitionId(String templateDefinitionId) {
        if (this.templateDefinition != null) {
            throw new IllegalArgumentException("You shouldn't try to set a template definition id if a template definition object is already assigned.");
        }
        this.templateDefinitionId = templateDefinitionId;
    }

    @Override
    public String toString() {
        ToStringBuilder tos = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        tos.append("fileDescriptor", fileDescriptor);
        tos.append("templateDefinitionId", getTemplateDefinitionId());
        tos.append("statistics", statistics);
        return tos.toString();
    }

    /**
     * Creates a template definition from all used variables. This may be used, if not template definition is
     * explicitly set.
     *
     * @return The created TemplateDefinition.
     */
    public TemplateDefinition createAutoTemplateDefinition() {
        TemplateDefinition autoTemplateDefinition = new TemplateDefinition();
        autoTemplateDefinition.setId(this.getFileDescriptor().getFilename());
        autoTemplateDefinition.setAutoGenerated(true);
        if (CollectionUtils.isNotEmpty(statistics.getUsedVariables())) {
            for (String variable : statistics.getUsedVariables()) {
                if (autoTemplateDefinition.getVariableDefinition(variable, false) == null) {
                    // Not yet registered.
                    autoTemplateDefinition.add(new VariableDefinition(VariableType.STRING, variable));
                }
            }
        }
        return autoTemplateDefinition;
    }

    @Override
    public Object clone() {
        Template template = null;
        try {
            template = (Template) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " isn't cloneable: " + ex.getMessage(), ex);
        }
        template.fileDescriptor = (FileDescriptor) this.fileDescriptor.clone();
        template.statistics = (TemplateStatistics) this.statistics.clone();
        return template;
    }

    /**
     *
     * @return The primary key served by the file descriptor.
     * @see FileDescriptor#getPrimaryKey()
     */
    public String getPrimaryKey() {
        return fileDescriptor != null ? fileDescriptor.getPrimaryKey() : null;
    }
}