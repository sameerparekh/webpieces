package org.webpieces.templating.impl.source;

import java.util.HashMap;
import java.util.Map;

import org.webpieces.templating.api.GroovyGen;
import org.webpieces.templating.impl.tags.ElseIfGen;
import org.webpieces.templating.impl.tags.ElseGen;
import org.webpieces.templating.impl.tags.IfGen;
import org.webpieces.templating.impl.tags.ListGen;
import org.webpieces.templating.impl.tags.VerbatimGen;

public class GenLookup {

	private Map<String, GroovyGen> generators = new HashMap<>();
	
	public GenLookup() {
		put(new VerbatimGen());
		put(new IfGen());
		put(new ElseIfGen());
		put(new ElseGen());
		put(new ListGen());
	}
	
	protected void put(GroovyGen generator) {
		generators.put(generator.getName(), generator);
	}

	public GroovyGen lookup(String genName, TokenImpl token) {
		GroovyGen gen = generators.get(genName);
		return gen;
	}
}
