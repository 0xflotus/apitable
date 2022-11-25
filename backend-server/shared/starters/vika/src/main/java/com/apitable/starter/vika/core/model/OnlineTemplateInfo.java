package com.apitable.starter.vika.core.model;

/**
 * <p>
 * Online template configuration information
 * </p>
 *
 */
public class OnlineTemplateInfo {
    private String templateName;

    private String[] templateCategoryName;

    private String[] templateTagName;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String[] getTemplateCategoryName() {
        return templateCategoryName;
    }

    public void setTemplateCategoryName(String[] templateCategoryName) {
        this.templateCategoryName = templateCategoryName;
    }

    public String[] getTemplateTagName() {
        return templateTagName;
    }

    public void setTemplateTagName(String[] templateTagName) {
        this.templateTagName = templateTagName;
    }
}
