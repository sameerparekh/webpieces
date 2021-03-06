package org.webpieces.templating.api;

public interface Token {

	/**
	 * When you throw an exception, you can add the location in the source by reading from the token it's location
	 */
	public String getSourceLocation(boolean dueToError);

	/**
	 * Get's the entire String between #{ and }# including arguments
	 */
	public String getCleanValue();

	public boolean isEndTag();

}
