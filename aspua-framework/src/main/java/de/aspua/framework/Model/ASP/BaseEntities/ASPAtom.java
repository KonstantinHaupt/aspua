package de.aspua.framework.Model.ASP.BaseEntities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an atom of a logic program. The entity can be extended by implementing subclasses.
 * Critical methods such as {@link #toString()}, {@link #equals(Object)},{@link #hashCode()} and {@link #createNewInstance()}
 * have to be refactored in subclasses, if new attributes are introduced.
 */
public class ASPAtom implements Serializable
{
    private static final long serialVersionUID = -3933154057202806227L;
    private static Logger LOGGER = LoggerFactory.getLogger(ASPAtom.class);

    private String predicate;
    private List<String> constants;
    private List<String> variables;

    /**
     * Creates a new ASP-Atom without any terms
     * @param predicate Predicate of the atom
     */
    public ASPAtom(String predicate)
    {
        this.setPredicate(predicate);
        constants = new ArrayList<String>();
        variables = new ArrayList<String>();
    }

    /**
     * Creates a new grounded ASP-Atom
     * @param predicate Predicate of the atom
     * @param constants Array of constants
     */
    public ASPAtom(String predicate, String... constants)
    {
        this.setPredicate(predicate);
        if(constants != null)
            this.setConstants(Arrays.asList(constants));
        else
            this.constants = new ArrayList<String>();

        variables = new ArrayList<String>();
    }

    /**
     * Creates a new ASP-Atom
     * @param predicate Predicate of the atom
     * @param constants Array of constants
     * @param variables Array of variables
     */
    public ASPAtom(String predicate, List<String> constants, List<String> variables)
    {
        this.setPredicate(predicate);
        this.setConstants(constants);
        this.setVariables(variables);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder()
            .append(predicate);

            if(!this.getTerms().isEmpty())
            {
                sb.append("(");
                sb.append(String.join(", ", this.getTerms()));
                sb.append(")");
            }

        return sb.toString(); 
    }

    @Override
    public int hashCode()
    {
        int result = 7;
        result = 31 * result + (predicate != null ? predicate.hashCode() : 0);
        result = 31 * result + (constants != null ? constants.hashCode() : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
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
        
        ASPAtom otherAtom = (ASPAtom) other;
        
        if(predicate.equals(otherAtom.getPredicate()))
        {   
            if(constants.isEmpty() && otherAtom.getTerms().isEmpty())
                return true;
            
            return constants.containsAll(otherAtom.getTerms())
                && otherAtom.getTerms().containsAll(constants);
        }

        return false;
    }

    /**
     * Returns all terms of the ASP-Atom, i.e. all constants and variables
     * @return List of all terms
     */
    public List<String> getTerms()
    {
        List<String> terms = new ArrayList<>();        
        terms.addAll(constants);
        terms.addAll(variables);

        return terms;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate)
    {
        if(predicate == null || predicate.trim() == "")
        {
            LOGGER.warn("An atom has to contain a valid predicate which is not null or an empty String!");
            this.predicate = "FAILEDATOM";
            return;
        }

        this.predicate = predicate;
    }

    public List<String> getConstants() {
        return constants;
    }

    public void setConstants(List<String> constants)
    {
        if(constants == null)
            this.constants = new ArrayList<String>();
        else
        {
            constants.removeAll(Collections.singletonList(null));
            this.constants = constants;
        }
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables)
    {
        if(variables == null)
            this.variables = new ArrayList<String>();
        else
        {
            variables.removeAll(Collections.singletonList(null));
            this.variables = variables;
        }
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    public ASPAtom createNewInstance()
    {
        ASPAtom newAtom = new ASPAtom(predicate);
        newAtom.setConstants(new ArrayList<>(constants));
        newAtom.setVariables(new ArrayList<>(variables));

        return newAtom;
    }
}
