package de.aspua.framework.Model.ASP.ELP;

import de.aspua.framework.Model.ASP.BaseEntities.ASPAtom;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;

/**
 * Represents an ASP-literal with strict negation for extended logic programs (ELPs).
 */
public class ELPLiteral extends ASPLiteral<ASPAtom>
{
    private static final long serialVersionUID = 3430995377317855975L;
    private boolean isNegated;

    /**
     * Creates a new ASP-literal with the given ASP-atom.
     * @param isNegated True if the literal is strictly negated, false otherwise
     * @param atom Object of the atom which is represented by the ASP-literal
     */
    public ELPLiteral(boolean isNegated, ASPAtom atom)
    {
        super(atom);
        this.isNegated = isNegated;
    }

    /**
     * Creates a new grounded ASP-literal by creating a {@link ASPAtom}-object using the given predicate and constants.
     * @param isNegated True if the literal is strictly negated, false otherwise
     * @param predicate Predicate of the atom, which is represented by the literal
     * @param constants Constants of the atom, which is represented by the literal
     */
    public ELPLiteral(boolean isNegated, String predicate, String... constants)
    {
        super(new ASPAtom(predicate, constants));
        this.isNegated = isNegated;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(isNegated)
            sb.append("-");
        
        sb.append(super.toString());
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (isNegated ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object other)
    {
        if(!super.equals(other))
            return false;
        
        ELPLiteral otherLiteral = (ELPLiteral) other;
        
        if( isNegated && otherLiteral.isNegated()
        || !isNegated && !otherLiteral.isNegated())
            return true;

        return false;
    }

    /**
     * Returns whether the literal is strictly negated.
     * @return True if the literal is strictly negated, false otherwise
     */
    public boolean isNegated() {
        return isNegated;
    }

    /**
     * Sets the negation value for this literal.
     * @param isNegated Decides if the literal is strictly negated or not
     */
    public void setNegation(boolean isNegated) {
        this.isNegated = isNegated;
    }

   /**
    * Creates a deep copy of the current object.
    * @return The created deep copy
    */
    @Override
    public ELPLiteral createNewInstance()
    {
        return new ELPLiteral(isNegated, this.getAtom().createNewInstance());
    }
}
