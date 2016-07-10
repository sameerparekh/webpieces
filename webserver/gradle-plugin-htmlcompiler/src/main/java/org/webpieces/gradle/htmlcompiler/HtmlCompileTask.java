package org.webpieces.gradle.htmlcompiler;

import java.io.File;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.Closure;

public class HtmlCompileTask extends AbstractCompile {
	private static final Logger log = LoggerFactory.getLogger(HtmlCompileTask.class);

    private HtmlCompileOptions options = new HtmlCompileOptions();

    @Nested
    public HtmlCompileOptions getOptions() {
        return options;
    }

    public void options(Action<HtmlCompileOptions> action) {
        action.execute(getOptions());
    }

    public void options(Closure<?> closure) {
        getProject().configure(getOptions(), closure);
    }
    
	@TaskAction
	public void compile() {
        LogLevel logLevel = getProject().getGradle().getStartParameter().getLogLevel();
        
        File destinationDir = getDestinationDir();
        System.out.println("destDir="+destinationDir);
        FileCollection srcCollection = getSource();
        Set<File> files = srcCollection.getFiles();
        for(File f : files) {
        	System.out.println("file="+f);
        }


		setDidWork(true);
	}
}
