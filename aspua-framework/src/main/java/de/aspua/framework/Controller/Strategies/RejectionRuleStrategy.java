package de.aspua.framework.Controller.Strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.aspua.framework.Controller.ControllerInterfaces.IStrategyController;
import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.SolutionMetaDataEnum;

/**
 * Implements conflict solving by adding so called rejection rules to the update sequence.
 * A rejection rule ensures that a negative body literal of a conflict rule is true whenever the body of the other conflict rule is true in an answer set.
 */
public class RejectionRuleStrategy implements IStrategyController
{
    private Conflict conflict;
    private ELPProgram updateSequence;

    @Override
    public void apply(List<ASPProgram<?, ?>> updateSequence, Conflict conflict)
    {
        this.conflict = conflict;

        if(updateSequence == null || updateSequence.isEmpty())
            this.updateSequence = new ELPProgram();
        else
        {
            this.updateSequence = (ELPProgram) updateSequence.get(0).createNewInstance();
            for(int i = 1; i < updateSequence.size(); i++)
            {
                for (ASPRule<?> currentRule : updateSequence.get(i).getRuleSet())
                {
                    this.updateSequence.addRule(currentRule.createNewInstance());
                }
            }
        }

        List<Solution> solutionCandidates = new ArrayList<>();
        ELPRule oldRule = (ELPRule) conflict.getConflictingRules().get(0);
        ELPRule newRule = (ELPRule) conflict.getConflictingRules().get(1);

        // Compute all solutions by deactivating the older rule
        solutionCandidates.addAll(this.computeSolutions(oldRule, newRule));

        // Compute all solutions by deactivating the newer rule
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
     * Computes all solutions for the given rules using rejection rules.
     * @param rejectedRule Focused rule which will be inactive if the solution is applied
     * @param referenceRule The rule which is used to build the rejection rule
     * @return List of all computed {@link Solution}-objects
     */
    private List<Solution> computeSolutions(ELPRule rejectedRule, ELPRule referenceRule)
    {
        List<Solution> computedSolutions = new ArrayList<>();

        rejectedRule = rejectedRule.createNewInstance();
        referenceRule = referenceRule.createNewInstance();

        // Compute the difference between the negative bodies of the referenceRule and rejectedRule to get all literals which may be used for generating solutions
        rejectedRule.getNegBody().removeAll(referenceRule.getNegBody());

        this.filterForConflictPotential(referenceRule, rejectedRule.getNegBody());
        for (ELPLiteral dependencyLiteral : rejectedRule.getNegBody())
        {
            // Compute all indirect rule modifications by adding rejection rules
            Solution solution = this.computeSolutionForFocusedLiteral(referenceRule, dependencyLiteral);
            solution.addMetaData(SolutionMetaDataEnum.TARGETLITERAL, dependencyLiteral);
            solution.addMetaData(SolutionMetaDataEnum.ROOTRULE, rejectedRule.getID());
            computedSolutions.add(solution);
        }

        return computedSolutions;
    }

    /**
     * Computes a solution by generating a rejection rule from the given paramters.
     * @param referenceRule Rule whose body is used to construct the rejection rule's body
     * @param focusedLiteral Head literal of the rejection rule
     * @return A {@link Solution} containing the generated rejection rule
     */
    private Solution computeSolutionForFocusedLiteral(ELPRule referenceRule, ELPLiteral focusedLiteral)
    {
        // Set the literal from negative body as the head of the rejection rule
        ELPLiteral rejectionHead = focusedLiteral.createNewInstance();

        // Use the positive body of the referenceRule as the positive body of the new rejectionRule
        List<ELPLiteral> posRejectionBody = new ArrayList<>();
        for (ELPLiteral elpLiteral : referenceRule.getBody())
            posRejectionBody.add(elpLiteral.createNewInstance());

        // Use the negative body of the referenceRule as the negative body of the new rejectionRule
        List<ELPLiteral> negRejectionBody = new ArrayList<>();
        for (ELPLiteral elpLiteral : referenceRule.getNegBody())
            negRejectionBody.add(elpLiteral.createNewInstance());

        List<ASPRule<?>> addRules = new ArrayList<>();
        ELPRule addedRule = new ELPRule(rejectionHead, posRejectionBody, negRejectionBody);

        int placeholderLabel = updateSequence.getRuleSet().size();
        while(updateSequence.getRuleByLabelID(placeholderLabel) != null)
            placeholderLabel++;

        addedRule.setLabelID(placeholderLabel);
        addRules.add(addedRule);
        return new Solution(conflict, addRules, null, null);
    }

    /**
     * Removes all potential head literals of rejection rules which could cause new conflicts.
     * @param referenceRule Rule whose body will be used to construct the rejection rule
     * @param possibleLiterals List of potential head literals for rejection rules
     */
    private void filterForConflictPotential(ELPRule referenceRule, List<ELPLiteral> possibleLiterals)
    {
        List<ELPLiteral> iterableLiterals = new ArrayList<>();
        iterableLiterals.addAll(possibleLiterals);
        for (ELPLiteral literalCandidate : iterableLiterals) 
        {
            for (ELPRule currentRule : updateSequence.getRuleSet())
            {
                // Constraints don't have head literals, and therefore cannot cause new conflicts
                if(currentRule.isContraint())
                    continue;

                // If the literalCandidate and the head of the current rule aren't complementary, there cannot arise a new conflict
                ELPLiteral currentRuleHead = currentRule.getHead().get(0);
                boolean equalHeadAtom = currentRuleHead.getAtom().equals(literalCandidate.getAtom());
                if(!equalHeadAtom || literalCandidate.isNegated() == currentRuleHead.isNegated())
                    continue;

                // If the positive body of the currentRule and the negative body of the referenceRule contain literals, the bodies cannot be true at the same time
                boolean contraryLiterals = currentRule.getBody().stream()
                    .anyMatch(referenceRule.getNegBody()::contains);
                if(contraryLiterals)
                    continue;

                // If the negative body of the currentRule and the positive body of the referenceRule contain literals, the bodies cannot be true at the same time
                contraryLiterals = currentRule.getNegBody().stream()
                    .anyMatch(referenceRule.getBody()::contains);
                if(contraryLiterals)
                    continue;

                // If no criteria is applicable, a rejection rule and the currentRule might create a conflict
                possibleLiterals.remove(literalCandidate);
            }
        }
    }
}
