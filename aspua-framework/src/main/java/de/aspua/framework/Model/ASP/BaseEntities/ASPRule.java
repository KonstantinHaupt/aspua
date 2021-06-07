package de.aspua.framework.Model.ASP.BaseEntities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents an ASP-rule. The entity can be extended by implementing subclasses.
 * Critical methods such as {@link #toString()}, {@link #equals(Object)},{@link #hashCode()} and {@link #createNewInstance()}
 * have to be refactored in subclasses, if new attributes are introduced.
 */
public class ASPRule<T extends ASPLiteral<?>> implements Serializable
{
    private static final long serialVersionUID = -8614624829731457500L;

    /** Unique ID for internal computations. Not suitable for displaying in Views! */
    private String id;

    /**  Optional ID whose purpose is to distinguish rules in the View. Not suitable for checking the ID during internal computations! */
    private int labelID;

    private List<T> head;
    private List<T> body;
    
    /**
     * Creates a new ASP-rule with the given head and body literals.
     * @param head List with all head literals
     * @param body List with all body literals
     */
    public ASPRule(List<T> head, List<T> body)
    {
        this.setHead(head);
        this.setBody(body);
        
        // Hardcode the first character as a 'char' to avoid possible problems with parsers/solvers etc. which cannot handle strings that start with a number
        id = "r" + UUID.randomUUID().toString().substring(0, 7);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        List<String> headStrings = new ArrayList<>();
        for (T currentLiteral : head)
        {
            headStrings.add(currentLiteral.toString());
        }

        sb.append(String.join(", ", headStrings));
        
        if(!body.isEmpty())
        {
            sb.append(" :- ");

            List<String> bodyStrings = new ArrayList<>();
            for (T currentLiteral : body)
            {
                bodyStrings.add(currentLiteral.toString());
            }

            sb.append(String.join(", ", bodyStrings));
        }

        sb.append(".");
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        int result = 7;
        result = 31 * result + (head != null ? head.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object other)
    {
        if(this == other)
            return true;
        if(other == null)
            return false;
        if(this.getClass() != other.getClass())
            return false;
        
        ASPRule<?> otherRule = (ASPRule<?>) other;

        if((head == null && otherRule.getHead() != null) 
        || (head != null && otherRule.getHead() == null))
        {
            return false;
        }
        else
        {
            if(!head.containsAll(otherRule.getHead())
            || !otherRule.getHead().containsAll(head))
            {
                return false;
            }
        }

        if((body == null && otherRule.getBody() != null) 
        || (body != null && otherRule.getBody() == null))
        {
            return false;
        }
        else
        {
            if(!body.containsAll(otherRule.getBody())
            || !otherRule.getBody().containsAll(body))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a list of all literals which are contained in the rule. Combines all head and body literals.
     * @return List of all literals within the rule
     */
    public List<T> getAllLiterals()
    {   
        List<T> allLiterals = new ArrayList<>();
        allLiterals.addAll(head);
        allLiterals.addAll(body);

        return allLiterals;
    }

    /**
     * Returns the internal, unique ID of the rule which can be used for computations.
     * If not changed manually, the ID contains a string starting with the character 'r', followed by 7 randomly generated characters.
     * @return The unique rule ID
     * @see #getLabelID()
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the internal, unique ID of the rule which can be used for computations.
     * The rule ID should only be changed if it is clear that the ID will stay unique.
     * @param id New ID of the rule. If null, a new, random ID is computed
     * @see #setLabelID(int)
     */
    public void setID(String id)
    {
        if(id == null)
            id = "r" + UUID.randomUUID().toString().substring(0, 7);
        else
            this.id = id;
    }

    /**
     * Returns the label ID of the rule which is usually the position of the rule within the corresponding {@link ASPProgram}.
     * The returned ID doesn't guarantees to be unique, and therefore shouldn't be used for computations or comparisons between rules.
     * @return The label ID of the rule
     * @see #getID()
     */
    public int getLabelID() {
		return labelID;
	}

    /**
     * Sets the label ID of the rule.
     * @param labelID New label ID of the rule
     * @return The label ID of the rule
     * @see #setID(String)
     */
	public void setLabelID(int labelID) {
		this.labelID = labelID;
	}

    /**
     * Returns all head literals of the rule.
     * @return List of all head literals
     */
    public List<T> getHead() {
        return head;
    }

    /**
     * Sets the head literals of the rule.
     * @param head List of new head literals
     */
    public void setHead(List<T> head)
    {
        if(head == null)
            this.head = new ArrayList<>();
        else
        {
            head.removeAll(Collections.singletonList(null));
            this.head = head;
        }
    }

    /**
     * Returns the body literals of the rule.
     * May be redefined in sublasses with different behaviour depending on the ASP-variant.
     * @return List of all body literals
     */
    public List<T> getBody() {
        return body;
    }

    /**
     * Sets the body literals of the rule.
     * May be redefined in sublasses with different behaviour depending on the ASP-variant.
     * @param body List of new body literals
     */
    public void setBody(List<T> body)
    {
        if(body == null)
            this.body = new ArrayList<>();
        else
        {
            body.removeAll(Collections.singletonList(null));
            this.body = body;
        }
    }

    /**
     * Checks whether the rule contains any body literals. If so, the rule isn't considered as a fact.
     * @return True, if the rule is a fact, false otherwise.
     */
    public boolean isFact() {
        return body.isEmpty();
    }

    /**
     * Checks whether the rule contains any head literals. If so, the rule isn't considered as a constraint.
     * @return True, if the rule is a constraint, false otherwise.
     */
    public boolean isContraint() {
        return head.isEmpty();
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    @SuppressWarnings("unchecked")
    public ASPRule<T> createNewInstance()
    {
        List<T> newHead = new ArrayList<>();
        List<T> newBody = new ArrayList<>();

        for (T currentLiteral : head)
            newHead.add((T) currentLiteral.createNewInstance());

        for (T currentLiteral : body)
            newBody.add((T) currentLiteral.createNewInstance());

        ASPRule<T> newRule = new ASPRule<>(newHead, newBody);
        newRule.setID(id);
        newRule.setLabelID(labelID);

        return newRule;
    }
}
