package de.aspua.framework.Model.ASP.ELP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;

/**
 * Represents an ASP-rule of an extended logic program (ELP). It allows default-negation and therefore contains a positive and negative body.
 */
public class ELPRule extends ASPRule<ELPLiteral>
{
    private static final long serialVersionUID = 8487894665163811690L;
    private static Logger LOGGER = LoggerFactory.getLogger(ELPRule.class);

    private List<ELPLiteral> negBody;

    /**
     * Creates a new ASP-rule for an ELP.
     * @param head List with all head literals
     * @param body List with all positive body literals
     */
    public ELPRule(List<ELPLiteral> head, List<ELPLiteral> body)
    {
        super(head, body);
    }

    /**
     * Creates a new ASP-rule for an ELP.
     * @param head List with all head literals
     * @param body List with all positive body literals
     * @param negBody List with all negative body literals
     */
    public ELPRule(ELPLiteral head, List<ELPLiteral> body, List<ELPLiteral> negBody)
    {
        super(new ArrayList<ELPLiteral>(){{add(head);}}, body);
        this.setNegBody(negBody);
        
        if(super.getHead().isEmpty() & super.getBody().isEmpty() & this.getNegBody().isEmpty())
        {
            LOGGER.warn("A Rule cannot be completely empty. It has to contain at least a head or body literal!");

            List<ELPLiteral> failedHead = new ArrayList<>();
            failedHead.add(new ELPLiteral(false, "FAILEDELPRULE"));
            this.setHead(failedHead);
        }

        super.setID(null);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (negBody != null ? negBody.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        String ruleString = super.toString();
        
        if(!negBody.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            ruleString = ruleString.substring(0, ruleString.length()-1);
            sb.append(ruleString);

            if(!sb.toString().contains(":-"))
                sb.append(" :- ");
            else
                sb.append(", ");

            List<String> negBodyStrings = new ArrayList<>();
            for (ELPLiteral currentLiteral : negBody)
            {
                negBodyStrings.add("not " + currentLiteral.toString());
            }

            sb.append(String.join(", ", negBodyStrings));
            sb.append(".");

            return sb.toString();
        }
        else
        {
            return ruleString;
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if(!super.equals(other))
            return false;
        
        ELPRule otherRule = (ELPRule) other;

        if((negBody == null && otherRule.getNegBody() != null) 
        || (negBody != null && otherRule.getNegBody() == null))
        {
            return false;
        }
        else
        {
            if(!negBody.containsAll(otherRule.getNegBody())
            || !otherRule.getNegBody().containsAll(negBody))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<ELPLiteral> getAllLiterals()
    {
        List<ELPLiteral> allLiterals = super.getAllLiterals();
        allLiterals.addAll(negBody);

        return allLiterals;
    }

    /**
     * Sets the head literal of the rule. As rules of an ELP are only allowed to have a single literal,
     * the first literal of the list is chosen if the given list contains multiple valuees.
     * @param head List of the new head literal
     */
    @Override
    public void setHead(List<ELPLiteral> head)
    {
        if(head != null)
        {
            head.removeAll(Collections.singletonList(null));
    
            if(head.size() > 1)
            {
                LOGGER.warn("The Rule of an extended logical programm (ELP) only support at max one head literal!", System.lineSeparator(),
                            "Choose first Literal of list as head literal.");
                super.setHead(head.subList(0, 1));
                return;
            }
        }

        super.setHead(head);
    }

    @Override
    public boolean isFact() {
        return super.isFact() && negBody.isEmpty();
    }

    @Override
    public boolean isContraint() {
        return this.getHead().isEmpty() && !this.getCompleteBody().isEmpty();
    }

    /**
     * Returns all body literals including the negative body.
     * @return List of positive and negative body literals
     * @see #getBody()
     * @see #getNegBody()
     */
    public List<ELPLiteral> getCompleteBody()
    {
        List<ELPLiteral> completeBody = new ArrayList<>();
        completeBody.addAll(super.getBody());
        completeBody.addAll(negBody);

        return completeBody;
    }

    /**
     * Returns all negative body literals, i.e. body literals which are default-.
     * @return List of all negative body literals
     */
    public List<ELPLiteral> getNegBody() {
        return negBody;
    }

    /**
     * Sets the negative body literals, i.e. body literals which are default-negated.
     * @negBody List of new negative body literals
     */
    public void setNegBody(List<ELPLiteral> negBody)
    {
        if(negBody == null)
            this.negBody = new ArrayList<ELPLiteral>();
        else
        {
            negBody.removeAll(Collections.singletonList(null));
            this.negBody = negBody;
        }
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    @Override
    public ELPRule createNewInstance()
    {
        ELPLiteral newHead;
        List<ELPLiteral> newBody = new ArrayList<>();
        List<ELPLiteral> newNegBody = new ArrayList<>();

        if(super.getHead().size() > 0)
            newHead = super.getHead().get(0).createNewInstance();
        else
            newHead = null;

        for (ELPLiteral currentLiteral : super.getBody())
            newBody.add(currentLiteral.createNewInstance());

        for (ELPLiteral currentLiteral : negBody)
            newNegBody.add(currentLiteral.createNewInstance());

        ELPRule newELPRule = new ELPRule(newHead, newBody, newNegBody);
        newELPRule.setID(super.getID());
        newELPRule.setLabelID(super.getLabelID());

        return newELPRule;
    }
}