package de.aspua.framework.Model.ASP.BaseEntities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an ASP-program. The entity can be extended by implementing subclasses.
 * Critical methods such as {@link #toString()}, {@link #equals(Object)},{@link #hashCode()} and {@link #createNewInstance()}
 * have to be refactored in subclasses, if new attributes are introduced.
 */
public class ASPProgram<T extends ASPRule<S>, S extends ASPLiteral<?>> implements Serializable
{
    private static final long serialVersionUID = -8640672656594547080L;
    private static Logger LOGGER = LoggerFactory.getLogger(ASPProgram.class);

    private String programName;
    
    private List<String> predicates;
    private List<String> constants;
    private List<String> variables;

    private HashMap<S, List<String>> usedLiterals;
    private List<T> ruleSet;

    /**
     * Creates an empty ASP-program with no rules and no program name.
     */
    public ASPProgram()
    {
        programName = "";

        this.predicates = new ArrayList<>();
        this.constants = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.usedLiterals = new HashMap<>();
        this.ruleSet = new ArrayList<>();
    }

    /**
     * Creates a new ASP-program with the given rule set.
     * @param ruleSet Rules which are contained in the program.
     */
    public ASPProgram(List<T> ruleSet)
    {
        programName = "";

        this.predicates = new ArrayList<>();
        this.constants = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.usedLiterals = new HashMap<>();
        this.ruleSet = new ArrayList<>();

        for (T rule : ruleSet)
            this.addRule(rule);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(programName);
        sb.append(System.lineSeparator());

        for (int i = 0; i < ruleSet.size(); i++)
        {
            int ruleLabel = ruleSet.get(i).getLabelID();
            sb.append("r" + ruleLabel + ": ");

            sb.append(ruleSet.get(i).toString());
            sb.append(System.lineSeparator());
        }
        
        return sb.toString();
    }

    /**
     * Adds a new rule to the program and adds the contained literals to the signature.
     * The given rule has to match the generic {@link ASPRule}-class type which is used by the current object.
     * @param rule Rule which is supposed to be added to the program
     * @return True if the rule could successfully added to the program, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean addRule(ASPRule<?> rule)
    {
        T castedRule = (T) rule;
        if(castedRule == null)
            return false;
        
        if(castedRule.getHead().isEmpty() && castedRule.getBody().isEmpty())
            return false;

        boolean alreadyExist =  ruleSet.stream()
            .anyMatch(currentRule -> currentRule.equals(castedRule));
        
        if(alreadyExist)
        {
            LOGGER.info("Did not add Rule '{}' because a Rule with the exact same literals already exists.", castedRule);
            return false;
        }
        
        if(this.getRule(castedRule.getID()) != null)
            castedRule.setID(null); 

        ruleSet.add(castedRule);
        this.updateUsedLiterals(castedRule, true);

        return true;
    }

    /**
     * Modifies an existing rule of the program by replacing the old rule with the given rule while preserving the original position within the rule set.
     * The rule which is supposed to be replaced has to match the ID of the given rule ({@link #getRule(String)}).
     * The given rule has to match the generic {@link ASPRule}-class type which is used by the current object.
     * @param rule Rule which is supposed to replace a exisiting rule
     * @return True if old rule could successfully be replaced by the given rule, false otherwise
     * @see #deleteRule(String)
     * @see #addRule(ASPRule)
     */
    public boolean modifyRule(ASPRule<?> rule)
    {
        if(rule == null)
            return false;

        T oldRule = this.getRule(rule.getID());

        if(oldRule == null)
            return false;

        int oldPosition = ruleSet.indexOf(oldRule);

        boolean success;
        success = this.deleteRule(rule.getID());

        if(success)
            success = this.addRule(rule);

        // Rearrange ruleSet, so the 'added' (i.e. modified) rule is assigned to its original position in the ruleSet
        if(success)
        {
            T modifiedRule = this.getRule(rule.getID());
            ruleSet.remove(modifiedRule);
            ruleSet.add(oldPosition,modifiedRule);
        }

        return success;
    }

    /**
     * Deletes an existing rule from the program's rule set and updates the signature accordingly.
     * @param id ID of the rule which is supposed to be deleted
     * @return True if rule could successfully be deleted, false otherwise
     */
    public boolean deleteRule(String id)
    {
        T ruleToDelete = this.getRule(id);
        
        if(ruleToDelete != null)
        {
            boolean success = this.getRuleSet().removeIf(x -> id == x.getID());

            if(success)
            {
                this.updateUsedLiterals(ruleToDelete, false);
                return true;
            }
        }

        return false;
    }

    /**
     * Returns all predicates which occure in at least one literal of the program
     * @return List of all predicates
     */
	public List<String> getPredicates() {
		return predicates;
	}

     /**
     * Returns all constants which occure in at least one literal of the program
     * @return List of all constants
     */
	public List<String> getConstants() {
		return constants;
	}

    /**
     * Returns all variables which occure in at least one literal of the program
     * @return List of all variables
     */
	public List<String> getVariables() {
		return variables;
	}

    /**
     * Returns a mapping from all occuring literals within the program to a list of the rule IDs, in which the literal occures.
     * @return Mapping from each occuring literal to the IDs of rules
     */
	public HashMap<S, List<String>> getLiteralBase() {
		return usedLiterals;
	}

    /**
     * Returns all rules which are contained in the program. The returned list should not be used to modify the rules of the program!
     * Use the provided methods of this class instead, as the signature of the program only gets updated if those methods are used.
     * In addition, convenient validation is applied by the provided methods, which is not given if the returned list itself is modified.
     * @return A list with all rules of the program. Should not be modified.
     * @see #addRule(ASPRule)
     * @see #modifyRule(ASPRule)
     * @see #deleteRule(String)
     */
	public List<T> getRuleSet() {
		return ruleSet;
	}
    
    /**
     * Returns the assigned name of the program. Is not ensured to be unique.
     * @return Name of the program
     */
    public String getProgramName() {
        return programName;
    }
    
    /**
     * Sets the name of the program.
     * @param programName New name of the program
     */
    public void setProgramName(String programName) {
        if(programName == null)
            this.programName = "";
        else
            this.programName = programName;
    }
    
    /**
     * Returns the first rule with the given ID.
     * @param id Rule ID of the searched rule
     * @return The rule with the given ID.
     * Returns null if the program doesn't contain a rule with the given ID
     * @see #getRuleByLabelID(int)
     */
    public T getRule(String id)
    {
        if(ruleSet.isEmpty() || id == null || id.isEmpty())
            return null;
        else
        {
            return ruleSet.stream()
                .filter(currentRule -> id.equals(currentRule.getID()))
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Returns the first rule with the given label ID.
     * @param id Label ID of the searched rule
     * @return The rule with the given label ID.
     * Returns null if the program doesn't contain a rule with the given label ID
     * @see #getRule(String)
     */
    public T getRuleByLabelID(int labelID)
    {
        if(ruleSet.isEmpty() || labelID < 0)
            return null;
        else
        {
            return ruleSet.stream()
                .filter(currentRule -> labelID == currentRule.getLabelID())
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Inspects all literals of the given rule and adds/removes them to the mapping of all used literals.
     * If an entry for a literal already exists and the rule is supposed to be added, the rule is registered in the value-list of the mapping.
     * Likewise, the whole entry is removed if the given rule was the only rule in the value-list and is supposed to be removed.
     * @param rule Rule which is inspected
     * @param add True if the literals should be added to the mapping, false otherwise.
     */
    protected void updateUsedLiterals(T rule, boolean add)
    {
        List<S> allRuleLiterals = new ArrayList<>(rule.getAllLiterals());

        for (S currentLiteral : allRuleLiterals)
        {
            if(add)
            {
                if(!usedLiterals.containsKey(currentLiteral))
                    usedLiterals.put(currentLiteral, new ArrayList<String>(Arrays.asList(rule.getID())));
                else
                {
                    List<String> updatedRuleValues = new ArrayList<>(usedLiterals.get(currentLiteral));
                    updatedRuleValues.add(rule.getID());
                    usedLiterals.put(currentLiteral, updatedRuleValues);
                }
            }
            else
            {
                List<String> updatedRuleValues = new ArrayList<>(usedLiterals.get(currentLiteral));

                if(updatedRuleValues.size() == 1)
                    usedLiterals.remove(currentLiteral);
                else
                {
                    updatedRuleValues.remove(rule.getID());
                    usedLiterals.replace(currentLiteral, updatedRuleValues);
                }
            }

            this.updateSignature(currentLiteral, add);
        }
    }

    /**
     * Updates the predicate-, constants- and variables-list of this class for a given literal.
     * The lists are only updated if an added symbol doesn't already exist in the corresponding list.
     * If the symbols are supposed to be removed, a symbol is only removed if there is no rule which still contains the symbol.
     * Therefore, a rule has to be deleted from the rule set before calling this method ({@link #deleteRule(String)}).
     * @param literal Literal, whose symbols are supposed to be added/removed
     * @param add True, if the symbols of the literal are supposed to be added to the lists.
     * False if the symbols are supposed to be removed from the lists.
     */
    private void updateSignature(S literal, boolean add)
    {
        String updatePredicate = literal.getAtom().getPredicate();
        List<String> updateConstants = literal.getAtom().getConstants();
        List<String> updateVariables = literal.getAtom().getVariables();

        if(add)
        {
            if(!predicates.contains(updatePredicate))
                predicates.add(updatePredicate);

            for (String currentConstant : updateConstants)
            {
                if(!constants.contains(currentConstant))
                    constants.add(currentConstant);
            }

            for (String currentVariable : updateVariables)
            {
                if(!variables.contains(currentVariable))
                    variables.add(currentVariable);
            }
        }
        else
        {
            boolean contained = usedLiterals.keySet().stream()
                .anyMatch(x -> updatePredicate.equals(x.getAtom().getPredicate()));

            if(!contained)
                predicates.remove(updatePredicate);

            for (String currentConstant : updateConstants)
            {
                contained = usedLiterals.keySet().stream()
                    .anyMatch(x -> x.getAtom().getConstants().contains(currentConstant));
                
                if(!contained)
                    constants.remove(currentConstant);
            }

            for (String currentVariable : updateVariables)
            {
                contained = usedLiterals.keySet().stream()
                    .anyMatch(x -> x.getAtom().getVariables().contains(currentVariable));
                
                if(!contained)
                    constants.remove(currentVariable);
            }
        }
    }

    /**
     * Creates a deep copy of the current object.
     * @return The created deep copy
     */
    public ASPProgram<T,S> createNewInstance()
    {
        ASPProgram<T,S> newProgram = new ASPProgram<>();
        newProgram.setProgramName(programName);

        for (T currentRule : ruleSet)
            newProgram.addRule(currentRule.createNewInstance());

        return newProgram;
    }
}