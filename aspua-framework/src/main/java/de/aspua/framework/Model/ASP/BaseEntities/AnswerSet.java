package de.aspua.framework.Model.ASP.BaseEntities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an answer set of an ASP-program.
 */
public class AnswerSet<T extends ASPRule<S>, S extends ASPLiteral<?>>  implements Serializable
{
    private static final long serialVersionUID = 3678182926317931435L;

    private static Logger LOGGER = LoggerFactory.getLogger(AnswerSet.class);
    
    private List<S> literals;
    private List<T> activeRules;

    public AnswerSet(List<S> literals)
    {
        this.setLiterals(literals);
        activeRules = new ArrayList<>();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(literals);
        sb.replace(0, 1, "{");
        sb.replace(sb.length()-1, sb.length(), "}");

        return sb.toString();
    }

    /**
     * Returns all literals of the answer set.
     * @return All literals contained in the answer set
     */
    public List<S> getLiterals() {
        return literals;
    }

    /**
     * Sets the literals within the answer set.
     * @param literals All literals contained in the answer set
     */
    public void setLiterals(List<S> literals) {
        if(literals == null)
        {
            LOGGER.warn("An answer set should contain at least one literal!");
            this.literals = new ArrayList<>();
        }
        else
            this.literals = literals;
    }

    /**
     * Returns all rules whose body is true in the answer set.
     * @return List of active rules
     */
    public List<T> getActiveRules() {
        return activeRules;
    }

    /**
     * Sets the active rules of this answer set.
     * @param activeRules List of active rules
     */
    public void setActiveRules(List<T> activeRules) {
        if(activeRules == null)
            this.activeRules = new ArrayList<>();
        else
            this.activeRules = activeRules;
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    @SuppressWarnings("unchecked")
    public AnswerSet<T, S> createNewInstance()
    {
        List<S> newLiterals = new ArrayList<>();
        for (S literal : literals)
            newLiterals.add((S) literal.createNewInstance());    

        AnswerSet<T, S> newAnswerSet = new AnswerSet<>(newLiterals);

        if(!activeRules.isEmpty())
        {
            List<T> newActiveRules = new ArrayList<>();
            for (T rule : activeRules)
                newActiveRules.add((T) rule.createNewInstance());    

            newAnswerSet.setActiveRules(newActiveRules);
        }
        return newAnswerSet;
    }
}
