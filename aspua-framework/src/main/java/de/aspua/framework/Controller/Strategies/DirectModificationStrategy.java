package de.aspua.framework.Controller.Strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.aspua.framework.Controller.ControllerInterfaces.IStrategyController;
import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.SolutionMetaDataEnum;

/**
 * Implements the rule modification as seen in 'Towards Interactive Conflict Resolution in ASP Programs' by Thevapalan and Kern-Isberner.
 * A rule modification adds default-negated literals from one conflict rule to the other conflict rule.
 * The modifications cause the modified conflict rule to be rejected if the body of the other conflict rule is true in an answer set.
 */
public class DirectModificationStrategy implements IStrategyController
{
    private Conflict conflict;
    
    @Override
    public void apply(List<ASPProgram<?, ?>> updateSequence, Conflict conflict)
    {
        this.conflict = conflict;

        List<Solution> solutions = new ArrayList<>();
        ELPRule oldRule = (ELPRule) conflict.getConflictingRules().get(0);
        ELPRule newRule = (ELPRule) conflict.getConflictingRules().get(1);

        // Compute all solutions by modifying the older rule
        solutions.add(this.computeSolution(oldRule, newRule));

        // Compute all solutions by modifying the new rule
        solutions.add(this.computeSolution(newRule, oldRule));

        // Remove all null entries from solutions which couldn't be generated
        solutions.removeAll(Collections.singletonList(null));
        conflict.getSolutions().addAll(solutions);
    }

    /**
     * @return false
     */
    @Override
	public boolean isComplex()
    {
        return false;
    }

    /**
     * Computes the solution.
     * @param rejectedRule Rule which is rejected if the generated solution is applied
     * @param referenceRule Rule which causes the rejection if the solution is applied
     * @return A {@link Solution}-object which describes a rule modification.
     * Returns null if no rule modifications are possible.
     */
    private Solution computeSolution(ELPRule rejectedRule, ELPRule referenceRule)
    {
        rejectedRule = rejectedRule.createNewInstance();
        referenceRule = referenceRule.createNewInstance();
        
        List<ASPRule<?>> possibleModifications = this.computeRuleModifications(rejectedRule, referenceRule);

        // If the rejected rule cannot be modified (the body of the other rule is a subset of the body the rejected rule), no solutions can be generated
        if(possibleModifications.isEmpty())
            return null;

        // Sort modifications such that the amount of literals in the variants is descending
        Comparator<ASPRule<?>> comparator = (ASPRule<?> first, ASPRule<?> second) -> 
        {
            return second.getAllLiterals().size() - first.getAllLiterals().size();
        };
        possibleModifications.sort(comparator);
        
        // Contains the rule modification which will be preselected in the generated solution (typed as list because the solution-constructor demands a list)
        // As default, the rule-modifications which use all literals from C should be preselected for the solution
        List<ASPRule<?>> selectedModification = new ArrayList<>();
        selectedModification.add(possibleModifications.get(0));
        possibleModifications.remove(possibleModifications.get(0));

        // Maps all possible modification-variants (typed as list because the solution-constructor demands a list; the only key for this strategy is the rejected rule)
        HashMap<String, List<ASPRule<?>>> modificationVariants = new HashMap<>();
        modificationVariants.put(rejectedRule.getID(), possibleModifications);

        // Create final solution and set the possible variants for modifications
        Solution solution = new Solution(conflict, null, selectedModification, null);
        solution.addMetaData(SolutionMetaDataEnum.ROOTRULE, rejectedRule.getID());
        solution.setModifyVariants(modificationVariants);

        return solution;
    }

    /**
     * Computes all possible rule modifications.
     * @param rejectedRule Rule which is modified in order to get rejected
     * @param referenceRule Rule which causes the rejection if the solution is applied
     * @return List of all possible modified rules, which could replace the rejected rule
     * @see IndirectModificationStrategy
     */
    private List<ASPRule<?>> computeRuleModifications(ELPRule rejectedRule, ELPRule referenceRule)
    {
        // Build C: Choose all literals from the referenceRule which are not already included in the body of the rejectedRule
        referenceRule.getBody().removeAll(rejectedRule.getBody());
        referenceRule.getNegBody().removeAll(rejectedRule.getNegBody());
        Set<Set<ELPLiteral>> powerSet = this.computePowerSet(new HashSet<ELPLiteral>(referenceRule.getCompleteBody()));

        List<ASPRule<?>> ruleModificationList = new ArrayList<>();
        for (Set<ELPLiteral> currentC : powerSet)
        {
            // At least one literal has to be added to the rejectedRule in order to deactivate it
            if(currentC.isEmpty())
                continue;
            
            ELPRule ruleModification = rejectedRule.createNewInstance();

            // Add C_not: The complementary literals that are supposed to deactivate the rejectedRule if the new rule is active
            for (ELPLiteral currentLiteral : currentC)
            {
                // Invert negation-as-failure (not not c = c)
                if(referenceRule.getBody().contains(currentLiteral))
                    ruleModification.getNegBody().add(currentLiteral);
                else
                    ruleModification.getBody().add(currentLiteral);
            }

            // Add solution with current C_not to the possible solutions for the conflict
            ruleModificationList.add(ruleModification);
        }

        return ruleModificationList;
    }

    /**
     * Computes all possible permutations of literals using recursion.
     * @param originalSet Set of literals, whose permutations should be computed
     * @return Set of literal-sets, where each set represents one permutation
     * @see IndirectModificationStrategy
     */
    private Set<Set<ELPLiteral>> computePowerSet(Set<ELPLiteral> originalSet)
    {
        Set<Set<ELPLiteral>> powerSet = new HashSet<Set<ELPLiteral>>();

        // base case: Return an empty set as the only valid permutation
        if (originalSet.isEmpty())
        {
            powerSet.add(new HashSet<ELPLiteral>());
            return powerSet;
        }

        // Fix one literal which is not used for further recursive invokations
        List<ELPLiteral> originalList = new ArrayList<ELPLiteral>(originalSet);
        ELPLiteral fixedLiteral = originalList.get(0);

        // For all literals except the fixed literal: use recursion to find all permutations
        Set<ELPLiteral> rest = new HashSet<ELPLiteral>(originalList.subList(1, originalList.size()));
        for (Set<ELPLiteral> currentRecursiveSet : this.computePowerSet(rest))
        {
            Set<ELPLiteral> newSet = new HashSet<ELPLiteral>();
            newSet.add(fixedLiteral);
            newSet.addAll(currentRecursiveSet);

            // Add set of {fixed literal + recursive permutation} to powerset
            powerSet.add(newSet);
            // Add set of {recursive permutation} to powerset (without fixed literal)
            powerSet.add(currentRecursiveSet);
        }

        return powerSet;
    }
}
