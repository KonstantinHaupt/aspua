package de.aspua.framework.Model.ASP.ELP;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;

/**
 * Represents an extended logic program (ELP).
 */
public class ELPProgram extends ASPProgram<ELPRule, ELPLiteral>
{
    private static final long serialVersionUID = -2731325228740423979L;
    private static Logger LOGGER = LoggerFactory.getLogger(ELPProgram.class);
    
    /**
     * Creates an empty ELP-program with no rules and no program name.
     */
    public ELPProgram()
    {
        super();
    }
    
    /**
     * Creates a new ELP-program with the given rule set.
     * @param ruleSet Rules which are contained in the program.
     */
    public ELPProgram(List<ELPRule> ruleSet)
    {
        super(ruleSet);
    }

    @Override
    public boolean addRule(ASPRule<?> rule)
    {
        ELPRule elpRule = (ELPRule) rule;
        if(elpRule == null)
            return false;
        
        if(elpRule.getHead().isEmpty() && elpRule.getCompleteBody().isEmpty())
            return false;

        boolean alreadyExist =  super.getRuleSet().contains(elpRule);
        
        if(alreadyExist)
        {
            LOGGER.info("Did not add Rule '{}' because a Rule with the exact same literals already exists.", elpRule);
            return false;
        }
        
        if(super.getRule(elpRule.getID()) != null)
            elpRule.setID(null);

        super.getRuleSet().add(elpRule);
        super.updateUsedLiterals(elpRule, true);

        return true;
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    @Override
    public ELPProgram createNewInstance()
    {
        ELPProgram newELPProgram = new ELPProgram();
        newELPProgram.setProgramName(super.getProgramName());

        for (ELPRule currentRule : super.getRuleSet())
        {
            newELPProgram.addRule(currentRule.createNewInstance());
        }

        return newELPProgram;
    }
}
