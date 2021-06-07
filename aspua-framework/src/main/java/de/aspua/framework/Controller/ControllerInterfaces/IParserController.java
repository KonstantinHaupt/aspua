package de.aspua.framework.Controller.ControllerInterfaces;

import java.util.List;

import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;

/**
 * Provides an Interface for parsing ASP-Programs and map Strings to model-entities.
 * Different implementations may be used for various types of ASP, such as normal logic programs (NLPs), extended logic programs (ELPs), etc.
 */
public interface IParserController
{
	/**
	 * Parses a given String to an {@link ASPProgram}-object (or subclass).
	 * @param programString String containing all rules which are supposed to be parsed.
	 * @param programName Program name which is set in the created {@link ASPProgram}-object.
	 * @return An (subclass-)object of the {@link ASPProgram}-class which corresponds to the given string.
	 * Returns null if the given program string doesn't follow the expected syntax.
	 */
	public abstract ASPProgram<?, ?> parseProgram(String programString, String programName);

	/**
	 * Parses each String in the given list to an {@link AnswerSet}-object.
	 * @param answerSets List of Strings in which each String represents a parsable answer set.
	 * @return A list of {@link AnswerSet}-objects, where each object corresponds to one string of the given list.
	 * Returns null if no answer sets could be parsed.
	 */
	public abstract List<AnswerSet<?, ?>> parseAnswerSets(List<String> answerSets);
}
