package org.webpieces.templating.impl.tags;

import org.webpieces.templating.api.AbstractTag;
import org.webpieces.templating.api.CompileCallback;
import org.webpieces.templating.api.ScriptOutput;
import org.webpieces.templating.api.Token;

public class IfGen extends AbstractTag {

	@Override
	public String getName() {
		return "if";
	}
	@Override
	public void generateStart(ScriptOutput sourceCode, Token token, int uniqueId, CompileCallback callback) {
		String cleanValue = token.getCleanValue();
		int indexOf = cleanValue.indexOf(" ");
		if(indexOf < 0)
			throw new IllegalArgumentException("if statement is missing expression.  "
					+ "It must be #{if expression}# to work. "+token.getSourceLocation(true));
		String expression = cleanValue.substring(indexOf+1);
		sourceCode.println("if ("+expression+") {", token);
		sourceCode.println();
	}

	@Override
	public void generateEnd(ScriptOutput sourceCode, Token token, int uniqueId) {
		sourceCode.println("}", token);
		sourceCode.println();
	}

}
