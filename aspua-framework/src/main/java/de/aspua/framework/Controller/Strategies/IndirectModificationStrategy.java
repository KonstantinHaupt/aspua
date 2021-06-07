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
 * Implements an indirect rule modification for a subset of positiv-dependent rules of a conflict rule.
 * Only positiv-dependent rules of first order are considered.
 */
public class IndirectModificationStrategy implements IStrategyController
{
    private Conflict conflict;
    private ASPProgram<?,?> updateSequence;
    
    public void apply(List<ASPProgram<?, ?>> updateSequence, Conflict conflict)
    {
        this.conflict = conflict;

        this.updateSequence = updateSequence.get(0).createNewInstance();
		for (int i = 1; i < updateSequence.size(); i++)
		{
			for (ASPRule<?> currentRule : updateSequence.get(i).getRuleSet())
			{
				this.updateSequence.addRule(currentRule.createNewInstance());
			}
		}

        List<Solution> solutionCandidates = new ArrayList<>();
        ELPRule oldRule = (ELPRule) conflict.getConflictingRules().get(0);
        ELPRule newRule = (ELPRule) conflict.getConflictingRules().get(1);

        // Compute all solutions by rejecting the older rule
        solutionCandidates.addAll(this.computeSolutions(oldRule, newRule));

        // Compute all solutions by rejecting the new rule
        solutionCandidates.addAll(this.computeSolutions(newRule, oldRule));

        // Remove all null entries from solutions which couldn't be generated
        solutionCandidates.removeAll(Collections.singletonList(null));

        conflict.getSolutions().addAll(solutionCandidates);
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
     * Computes the solutions.
     * @param rejectedRule Rule which is rejected if a generated solution is applied
     * @param referenceRule Rule whose body is used to generate default-complementary literals to reject dependency rules
     * @return List of all computed {@link Solution}-objects
     */
    private List<Solution> computeSolutions(ELPRule rejectedRule, ELPRule referenceRule)
    {
        List<Solution> computedSolutions = new ArrayList<>();

        rejectedRule = rejectedRule.createNewInstance();
        referenceRule = referenceRule.createNewInstance();

        // Compute the difference between the positive bodies of the referenceRule and rejectedRule to get all literals which may be used for generating solutions
        rejectedRule.getBody().removeAll(referenceRule.getBody());

        // Compute all indirect rule modifications for each possible positive body literal of the rejectedRule
        for (ELPLiteral dependencyLiteral : rejectedRule.getBody())
        {
            Solution solution = this.computeSolutionForDependyLiteral(referenceRule, dependencyLiteral);

            // Check if a solution could be generated for the current dependency literal
            if(solution != null)
            {
                solution.addMetaData(SolutionMetaDataEnum.TARGETLITERAL, dependencyLiteral);
                solution.addMetaData(SolutionMetaDataEnum.ROOTRULE, rejectedRule.getID());
                computedSolutions.add(solution);
            }
        }
        return computedSolutions;
    }

    /**
     * Computes a solution by rejecting all dependency rules with the given head through (indirect) rule modifications.
     * @param referenceRule Rule whose body is used to generate default-complementary literals to reject dependency rules
     * @param dependencyLiteral head of dependency rules who should be modified
     * @return A {@link Solution}-object which contains indirect rule modifications for all dependency rules with the given head literal.
     * Returns null if not all dependency rules can be rejected by indirect rule modifications.
     */
    public Solution computeSolutionForDependyLiteral(ELPRule referenceRule, ELPLiteral dependencyLiteral)
    {
        // Contains the rules which will be preselected in the generated solution
        List<ASPRule<?>> selectedModifications = new ArrayList<>();

        // Maps all possible modifications for each dependencyRule
        HashMap<String, List<ASPRule<?>>> allDependecyModifications = new HashMap<>();

        // Compute all possible rule-modifications for each dependencyRule
        List<ELPRule> dependencyRules = this.computeRelevantDependencyRules(dependencyLiteral);
        for (ELPRule dependencyRule : dependencyRules)
        {
            List<ASPRule<?>> possibleModifications = this.computeRuleModifications(dependencyRule.createNewInstance(), referenceRule.createNewInstance());

            // If at least one rule cannot be modified (e.g. the body of the referenceRule is a subset of the body the dependecyRule),
            //      the literal cannot be disabled. Therefore, not solution can be generated.
            if(possibleModifications.isEmpty())
                return null;

            // Sort modifications such that the amount of literals in the variants is descending
            Comparator<ASPRule<?>> comparator = (ASPRule<?> first, ASPRule<?> second) -> 
            {
                return second.getAllLiterals().size() - first.getAllLiterals().size();
            };
            possibleModifications.sort(comparator);

            // As default, the rule-modifications which use all literals from C should be preselected for the solution
            selectedModifications.add(possibleModifications.get(0));
            possibleModifications.remove(possibleModifications.get(0));

            allDependecyModifications.put(dependencyRule.getID(), possibleModifications);
        }

        // Create final solution and set the possible variants for modifications
        Solution solution = new Solution(conflict, null, selectedModifications, null);
        solution.setModifyVariants(allDependecyModifications);

        return solution;
    }

    /**
     * Filters the update sequence for all rules with the given head literal, which can possibly be active with the conflicting rules.
     * @param dependencyLiteral Head literal of the filtered rules
     * @return List of all relevant rules of the update sequence
     */
    private List<ELPRule> computeRelevantDependencyRules(ELPLiteral dependencyLiteral)
    {
        List<ELPRule> dependencyRules = new ArrayList<>();
        for (ASPRule<?> untypedRule : updateSequence.getRuleSet())
        {
            ELPRule candidateRule = (ELPRule) untypedRule;
            boolean isRelevant = !candidateRule.isContraint() && candidateRule.getHead().get(0).equals(dependencyLiteral);

            for (ASPRule<?> untypedConflictRule : conflict.getConflictingRules())
            {
                if(!isRelevant)
                    break;

                ELPRule conflictRule = (ELPRule) untypedConflictRule;

                // Search for complementary literals in positive body
                for (ELPLiteral referenceLiteral : conflictRule.getBody())
                {
                    for (ELPLiteral candidateLiteral : candidateRule.getBody())
                    {
                        if(referenceLiteral.getAtom().equals(candidateLiteral.getAtom())
                        && (referenceLiteral.isNegated() != candidateLiteral.isNegated()))
                        {
                            isRelevant = false;
                            break;
                        }  

                    }

                    if(!isRelevant)
                        break;
                }

                // Check if a positive body literal of the conflictRule already exists in the negative body of the candidateRule
                if(isRelevant)
                    isRelevant = conflictRule.getBody().stream()
                    .noneMatch(x -> candidateRule.getNegBody().contains(x));

                // Check if a negative body literal of the conflictRule already exists in the positive body of the candidateRule
                if(isRelevant)
                    isRelevant = conflictRule.getNegBody().stream()
                    .noneMatch(x -> candidateRule.getBody().contains(x));
            }

            if(isRelevant)
                dependencyRules.add(candidateRule.createNewInstance());
        }

        return dependencyRules;
    }

    /**
     * Computes all possible rule modifications for rejecting the rule of the first parameter if the rule of the second parameter is active in an answer set.
     * @param rejectedRule Rule which is modified in order to get rejected
     * @param referenceRule Rule which causes the rejection if the solution is applied
     * @return List of all possible modified rules, which could replace the rejected rule
     * @see DirectModificationStrategy
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
     * @see DirectModificationStrategy
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
    }}
