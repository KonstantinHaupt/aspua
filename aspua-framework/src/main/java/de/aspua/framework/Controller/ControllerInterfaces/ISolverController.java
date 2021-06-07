package de.aspua.framework.Controller.ControllerInterfaces;

import java.util.List;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;

/**
 * Provides an Interface to generate the models/answer sets of an ASP-Programs.
 */
public interface ISolverController
{
	/**
	 * Computes all models/answer sets for the given ASP-program.
	 * @param program {@link ASPProgram}-object whose answer sets are computed
	 * @return List of strings, where each string represents an answer sets for the given ASP-program.
	 * Returns null if the given ASP-program is unsatisfiable or an error occured while computing the models/answer sets.
	 */
	public abstract List<String> computeModels(ASPProgram<?, ?> program);
}
