package de.aspua.framework.Utils;

/**
 * Enum for all meta-data, which can be computed and safed in the {@link Solution}-class.
 * The class-type, which should be used for saving a specific meta-data value is described below.
 * @see de.aspua.framework.Model.Solution
 * @see de.aspua.framework.Controller.ControllerInterfaces.IMeasureController
 */
public enum SolutionMetaDataEnum
{
    /** 
     * ClassType: String
     * Defines the ID of the conflicting rule which will be deactivated by the given solution.
     */
    ROOTRULE,

    /**
     * ClassType: Literal<?>
     * Defines the literal which is targeted by the solution for the conflict (only applicable for indirect conflict resolution).
     */
    TARGETLITERAL,

    /**
     * ClassType: int
     * Defines how many changes will be caused in the answer sets if a given solution is applied.
     * @see de.aspua.framework.Controller.Measures.AnswerSetMeasure
     */
    MEASURE_ANSWERSETCHANGES,

    /**
     * ClassType: int
     * Defines how many changes will be caused in the rule set if a given solution is applied.
     * @see de.aspua.framework.Controller.Measures.RuleMeasure
     */
    MEASURE_RULECHANGES
}
