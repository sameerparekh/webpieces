package org.webpieces.gradle.htmlcompiler;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public class HtmlCompileOptions {
    private String encoding = "UTF-8";
    
    @Optional @Input
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
