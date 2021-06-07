package de.aspua.framework.Controller.Measures;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ASPUAFrameworkAPI;
import de.aspua.framework.Controller.ControllerInterfaces.IMeasureController;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.SolutionMetaDataEnum;

/**
 * Provides a measure to determine how many literals within program-rules would be changed by a applied solution.
 * Different rules are compared by their 'rule-distance', which represents the symmetric difference between the literals in rule-parts (head, positive/negative body).
 * Added and deleted rules are compared to empty sets. The 'rule-distance' then equals the amount of literals in the added/deleted rules.
 */
public class RuleMeasure implements IMeasureController
{
    private static Logger LOGGER = LoggerFactory.getLogger(RuleMeasure.class);
    
    private ASPUAFrameworkAPI frameworkAPI;
    private List<ASPProgram<?, ?>> updateSequence;

    public RuleMeasure(ASPUAFrameworkAPI frameworkAPI)
    {
        this.frameworkAPI = frameworkAPI;
    }
    
    /**
     * Computes the rule measure for the given solution.
     * The computed measure is set in the metadata-Attribute of the given solution with the key {@link SolutionMetaDataEnum#MEASURE_RULECHANGES}.
     * The measure is only computed for the currently chosen operations. If a operation-variant is chosen, the measure has to be applied once again.
     */
    @Override
    public void computeMeasure(Solution solution)
    {
        this.updateSequence = frameworkAPI.getUpdateSequence();
        int result = 0;

        // rule-distance for added and deleted rules equals the amount of literals in the rule
        for (ASPRule<?> solutionRule : solution.getAddedRules())
            result += solutionRule.getAllLiterals().size();

        for (ASPRule<?> solutionRule : solution.getDeletedRules())
            result += solutionRule.getAllLiterals().size();

        for (ASPRule<?> solutionRule : solution.getModifiedRules())
        {
            // Search for the original rule from the update sequence which gets modified
            ASPRule<?> originalRule = null;
            for (ASPProgram<?,?> aspProgram : updateSequence)
            {
                originalRule = aspProgram.getRule(solutionRule.getID());

                if(originalRule != null)
                    break;
            }

            if(originalRule == null)
            {
                LOGGER.warn("The rule measure couldn't be computed because the modification of rule {} doesn't refer to an existing rule in the Update-Sequence. Solution: \\s {}",
                            solutionRule.getID(), solution.toString());
                result = -1;
            }
            else
                result += this.computeRuleDistance((ELPRule) originalRule,(ELPRule) solutionRule);
        }
        
        if(result >= 0)
            solution.addMetaData(SolutionMetaDataEnum.MEASURE_RULECHANGES, result);
    }

    /**
     * Computes the rule-distance between the given rules.
     * @param originalRule Original rule from the update sequence that gets modified
     * @param modifiedRule New, modified version of the original rule
     * @return The rule distance between the given rules
     */
    private int computeRuleDistance(ELPRule originalRule, ELPRule modifiedRule)
    {
        int difference = 0;
        // Symmetric difference in ruleheads
        difference += this.computeSymmetricDifference(originalRule.getHead(), modifiedRule.getHead());
        // Symmetric difference in positiv body
        difference += this.computeSymmetricDifference(originalRule.getBody(), modifiedRule.getBody());
        // Symmetric difference in negative body
        difference += this.computeSymmetricDifference(originalRule.getNegBody(), modifiedRule.getNegBody());

        return difference;
    }

    /**
     * Computes the symmetric difference between two lists of literals.
     * The parameter-lists use wildcards because the literal-lists of different {@link ASPRule}-objects might not use the same class-type.
     * This is only necessary for type-saefty! Only invoke the the method with lists of {@link ASPLiteral}-objects!
     * @param first List of {@link ASPLiteral}-objects, which represent the literals of the first rule.
     * @param second List of {@link ASPLiteral}-objects, which represent the literals of the second rule.
     * @return The symmetric difference between the given lists of literals.
     * @see AnswerSetMeasure
     */
    private int computeSymmetricDifference(List<?> first, List<?> second)
    {
        if(first.size() == 0)
            return second.size();

        if(second.size() == 0)
            return first.size();

        // Convert wildcard-type back to concrete Literal<?> type
        List<ASPLiteral<?>> castedFirst = new ArrayList<>();
        List<ASPLiteral<?>> castedSecond = new ArrayList<>();
        first.forEach(x -> castedFirst.add((ASPLiteral<?>) x));
        second.forEach(x -> castedSecond.add((ASPLiteral<?>) x));

        int distance = 0;
        //Compute difference between first and second list of literals in rule
        List<ASPLiteral<?>> firstLiteralList = new ArrayList<>(castedFirst);
        List<ASPLiteral<?>> secondLiteralList = new ArrayList<>(castedSecond);
        firstLiteralList.removeAll(secondLiteralList);
        distance = firstLiteralList.size();

        //Compute difference between second and first list of literals in rule
        firstLiteralList = new ArrayList<>(castedFirst);
        secondLiteralList.removeAll(firstLiteralList);
        distance += secondLiteralList.size();

        return distance;
    }
}
