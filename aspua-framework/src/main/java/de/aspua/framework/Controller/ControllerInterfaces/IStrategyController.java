package de.aspua.framework.Controller.ControllerInterfaces;

import java.util.List;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;

/**
 * Provides an Interface to compute possible solutions for a conflict.
 * Each implementation provides a different strategy for generating solutions.
 * Depending on the strategy, the implementation might need a reference to the used {@link ASPUAFrameworkAPI} execute necessary operations such as precomputing.
 */
public interface IStrategyController
{
	/**
	 * Applies the strategy to the given conflict. The created {@link Solution}-objects are added to the given {@link Conflict}-object.
	 * @param updateSequence Update sequence which contains the conflict.
	 * @param conflict Conflict which is supposed to be solved by the computed solutions
	 */
	public abstract void apply(List<ASPProgram<?, ?>> updateSequence, Conflict conflict);

	/**
	 * Specifies if the strategy should be automatically applied to detected conflicts.
	 * If the conflict relies on additional user input or is computationally complex, the strategy may only be applied if it is explicitly invoked.
	 * @return True if the strategy is complex (not immediately applied).
	 * False if the strategy is not complex and should be immediately applied if the conflict is detected.
	 * @see de.aspua.framework.Controller.ASPUAFrameworkAPI#detectConflicts()
	 * @see de.aspua.framework.Controller.ASPUAFrameworkAPI#solveConflict(de.aspua.framework.Model.Solution)
	 */
	public abstract boolean isComplex();
}
