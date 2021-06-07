package de.aspua.framework.Controller.ControllerInterfaces;

import java.util.List;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;

/**
 * Provides an interface to detect Conflicts between different ASP-Programs of an update sequence.
 */
public interface IConflictDetectionController
{
	/**
	 * Computes an {@link ASPProgram} based on the given update sequence, whose answer sets allow to detect conflicts between the ASP-Programs of the update sequence.
	 * @param updateSequence The update sequence which is supposed to be checked for conflicts
	 * @return ASP-Program which is able to detect conflicts within the update sequence by inspecting its answer sets (use {@link #detectConflicts(List, List)} for that purpose)
	 */
	public abstract ASPProgram<?, ?> computeConflictDetectionProgram(List<ASPProgram<?, ?>> updateSequence);

	/**
	 * Detects conflicts based on the answer sets of the conflict-detection {@link ASPProgram}, which is generated in {@link #computeConflictDetectionProgram(List)}.
	 * @param updateSequence Update sequence which provides a ruleset to generate {@link Conflict}-objects
	 * @param answerSets Answer sets of the conflict-detection {@link ASPProgram}
	 * @return A List of {@link Conflict}-Objects which describes all detected conflicts within the update sequence
	 */
	public abstract List<Conflict> detectConflicts(List<ASPProgram<?, ?>> updateSequence, List<AnswerSet<?, ?>> answerSets);
}
