package de.aspua.framework.Model.ASP.BaseEntities;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a literal of a logic program. The entity can be extended by implementing subclasses.
 * Critical methods such as {@link #toString()}, {@link #equals(Object)},{@link #hashCode()} and {@link #createNewInstance()}
 * have to be refactored in subclasses, if new attributes are introduced.
 */
public class ASPLiteral<T extends ASPAtom> implements Serializable
{
    private static final long serialVersionUID = -24718173014043888L;
    private static Logger LOGGER = LoggerFactory.getLogger(ASPLiteral.class);
    
    private T atom;

    /**
     * Creates a new ASP-literal with the given ASP-atom.
     * @param atom Object of the atom which is represented by the ASP-literal
     */
    public ASPLiteral(T atom)
    {
        this.setAtom(atom);
    }

    @Override
    public String toString()
    {
        return atom.toString();
    }

    @Override
    public int hashCode()
    {
        int result = 7;
        result = 31 * result + (atom != null ? atom.hashCode() : 0);
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
        
        ASPLiteral<?> otherLiteral = (ASPLiteral<?>) other;
        
        if(atom.equals(otherLiteral.getAtom()))
            return true;

        return false;
    }

    /**
     * Returns the ASP-atom of the literal.
     * @return Object of the literal's ASP-Atom
     */
    public T getAtom()
    {
        return atom;
    }

    @SuppressWarnings("unchecked")
    public void setAtom(T atom)
    {
        if(atom == null)
        {
            LOGGER.warn("A Literal has do be defined over a atom which is not null!");
            this.atom = (T) new ASPAtom("FAILEDLITERAL");
            return;
        }
        
        this.atom = atom;
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    @SuppressWarnings("unchecked")
    public ASPLiteral<T> createNewInstance()
    {
        return new ASPLiteral<T>((T) atom.createNewInstance());
    }
}
