package org.webpieces.templating.impl.source;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.templating.api.CompileCallback;

public class GroovyScriptGenerator {

	private static final Logger log = LoggerFactory.getLogger(GroovyScriptGenerator.class);
	private TemplateTokenizer tokenizer;
	private ScriptWriter creator;

	@Inject
	public GroovyScriptGenerator(TemplateTokenizer tokenizer, ScriptWriter creator) {
		this.tokenizer = tokenizer;
		this.creator = creator;
	}
	
	public ScriptOutputImpl generate(String filePath, String source, String fullClassName, CompileCallback callbacks) {
		long start = System.currentTimeMillis();
		source = source.replace("\r", "");
		
		List<TokenImpl> tokens = tokenizer.tokenize(filePath, source);

		String className = fullClassName;
		String packageStr = null;
		//split class name if it has package
		int index = fullClassName.lastIndexOf(".");
		if(index > 0) {
			className = fullClassName.substring(index+1);
			packageStr = fullClassName.substring(0, index);
		}

		ScriptOutputImpl sourceCode = new ScriptOutputImpl(packageStr, className);

		try {
			// Class header
			creator.printHead(sourceCode, packageStr, className);
	
			generateBody(sourceCode, tokens, callbacks);
	
			// Class end
			creator.printEnd(sourceCode);
		} finally {
			creator.cleanup();
		}
		
		TokenImpl token = tokens.get(tokens.size()-1);
		int lastLine = token.endLineNumber;
		long total = System.currentTimeMillis() - start;
		log.info(total+"ms (dev mode only) source generation. class="+className+" from "+lastLine+" html lines of code to "+sourceCode.getLineNumber()+" lines of groovy code");
		
		return sourceCode;
	}

	private void generateBody(ScriptOutputImpl sourceCode, List<TokenImpl> tokens, CompileCallback callbacks) {
		for(int i = 0; i < tokens.size(); i++) {
			TokenImpl token = tokens.get(i);
			TokenImpl previousToken = null;
			if(i > 0)
				previousToken = tokens.get(i-1);
			
			TemplateToken state = token.state;
			
			switch (state) {
			case EOF:
				return;
			case PLAIN:
				creator.printPlain(token, sourceCode);
				break;
			case SCRIPT:
				creator.printScript(token, sourceCode);
				break;
			case EXPR:
				creator.printExpression(token, sourceCode);
				break;
			case MESSAGE:
				creator.printMessage(token, sourceCode);
				break;
			case ACTION:
				creator.printAction(false);
				break;
			case ABSOLUTE_ACTION:
				creator.printAction(true);
				break;
			case COMMENT:
				creator.unprintUpToLastNewLine();
				break;
			case START_END_TAG:
				creator.printStartEndTag(token, sourceCode, callbacks);
				break;
			case START_TAG:
				creator.printStartTag(token, previousToken, sourceCode, callbacks);
				break;
			case END_TAG:
				creator.printEndTag(token, sourceCode);
				break;
			}
		}
	}
}
