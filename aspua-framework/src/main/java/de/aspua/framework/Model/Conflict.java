package de.aspua.framework.Model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;

/**
 * Represents a conflict between two ASP-rules which have complementary head-literals and whose bodies possibly be true in the same interpretation.
 * Objects of this class are created by implementations of the {@link de.aspua.framework.Controller.ControllerInterfaces.IConflictDetectionController}. 
 * Possible solutions for a conflict are modeled in {@link Solution}-objects.
 */
public class Conflict
{
    private static Logger LOGGER = LoggerFactory.getLogger(Conflict.class);
    
    /** List of the conflicting rules */
    private List<ASPRule<?>> conflictingRules;
    /** Contains all answer sets which would potentionally fulfill the bodies of all conflicting rules and therefore would be contradictory */
    private List<AnswerSet<?, ?>> involvedAnswerSets;
    /** List of all computed solutions for this conflict */
    private List<Solution> solutions;

    public Conflict(List<ASPRule<?>> conflictingRules, List<AnswerSet<?, ?>> involvedAnswerSets)
    {
        this.setConflictingRules(conflictingRules);
        this.setInvolvedAnwerSets(involvedAnswerSets);

        solutions = new ArrayList<>();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Conflict between rules:");
        sb.append(System.lineSeparator());
        
        for (ASPRule<?> currentRule : conflictingRules)
        {
            int ruleLabel = currentRule.getLabelID();
            sb.append("r" + ruleLabel + ": ");

            sb.append(currentRule.toString());
            sb.append(System.lineSeparator());
        }

        sb.append(System.lineSeparator());
        sb.append(String.format("The conflict appeared in %s answer set(s):", involvedAnswerSets.size()));
        sb.append(System.lineSeparator());

        for (AnswerSet<?, ?> currentAnswerSet : involvedAnswerSets)
        {
            sb.append(currentAnswerSet.toString());
            sb.append(System.lineSeparator());
        }

        sb.append(System.lineSeparator());

        for(int i = 0; i < solutions.size(); i++)
        {
            sb.append("Possible solution Nr. " + i);
            sb.append(System.lineSeparator());
            sb.append(solutions.get(i).toString());
        }

        return sb.toString();
    }

    public List<ASPRule<?>> getConflictingRules() {
        return conflictingRules;
    }

    public void setConflictingRules(List<ASPRule<?>> conflictingRules) {
        if(conflictingRules == null || conflictingRules.size() < 2)
        {
            LOGGER.warn("The involved rules of a conflict were set to less than 2 rules!" + 
                        "This could lead to possible errors or missbehaviour in the following conflict resolution!");
        }

        if(conflictingRules == null)
            this.conflictingRules = new ArrayList<>();
        else
            this.conflictingRules = conflictingRules;
    }

    public List<AnswerSet<?, ?>> getInvolvedAnwerSets() {
        return involvedAnswerSets;
    }

    public void setInvolvedAnwerSets(List<AnswerSet<?, ?>> involvedAnswerSets) {
        if(involvedAnswerSets == null || involvedAnswerSets.isEmpty())
        {
            LOGGER.warn("The conflicting answer sets of a conflict were set to null or an empty list!" + 
                        "This could lead to possible errors or missbehaviour in the following conflict resolution!");
        }

        if(involvedAnswerSets == null)
            this.involvedAnswerSets = new ArrayList<>();
        else
            this.involvedAnswerSets = involvedAnswerSets;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<Solution> solutions) {
        if(solutions == null)
            this.solutions = new ArrayList<>();
        else
            this.solutions = solutions;
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    public Conflict createNewInstance()
    {
        List<ASPRule<?>> newConflictingRules = new ArrayList<>();
        for (ASPRule<?> rule : conflictingRules)
            newConflictingRules.add(rule.createNewInstance());    

        List<AnswerSet<?, ?>> newAnswerSets = new ArrayList<>();
        for (AnswerSet<?,?> answerSet : involvedAnswerSets)
            newAnswerSets.add(answerSet.createNewInstance());    

        Conflict newConflict = new Conflict(newConflictingRules, newAnswerSets);

        List<Solution> newSolutions = new ArrayList<>();
        for (Solution solution : solutions)
            newSolutions.add(solution.createNewInstance(newConflict));  

        newConflict.setSolutions(newSolutions);
        return newConflict;
    }
}
