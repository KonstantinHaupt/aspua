package de.aspua.framework.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Utils.OperationTypeEnum;
import de.aspua.framework.Utils.SolutionMetaDataEnum;

/**
 * Represents a solution for an detected conflict between two ASP-rules. Objects of this class are linked to an {@link Conflict}-object,
 * and can be used to solve the conflict by invoking the API-method {@link ASPUAFrameworkAPI#solveConflict(solution)}.
 * Objects of this class distinguish between actually applied operations (chosen rules) and possible variants for operations.
 * Variants of rule operations are NOT considered if the solution is actually applied. Only the operations in the lists<p>
 * {@link #getAddedRules()}<p>
 * {@link #getModifiedRules()}<p>
 * {@link #getDeletedRules()}<p>
 * are considered.
 * Objects of this class are created by implementations of the {@link de.aspua.framework.Controller.ControllerInterfaces.IStrategyController}. 
 * Detailed properties (meta-data) can be computed by implementations of the {@link de.aspua.framework.Controller.ControllerInterfaces.IMeasureController}
 */
public class Solution
{
    private static Logger LOGGER = LoggerFactory.getLogger(Solution.class);

    /** Contains all operations which will be applied to the update sequence */
    private HashMap<OperationTypeEnum, List<ASPRule<?>>> chosenRuleMapping;

    /** 
     * Variants for operations of {@link de.aspua.framework.Utils.OperationTypeEnum}
     * Key: ID of rule, Value: List of all variants for the rule with the ID
     */
    private HashMap<String, List<ASPRule<?>>> addVariants;
    private HashMap<String, List<ASPRule<?>>> modifyVariants;
    private HashMap<String, List<ASPRule<?>>> deleteVariants;

    /** Conflict which is supposed to be solved by the solution-object */
    private final Conflict cause;

    /** Container for meta-data computed by implementations of {@link de.aspua.framework.Controller.ControllerInterfaces.IMeasureController} */
    private HashMap<SolutionMetaDataEnum, Object> metaData;

    public Solution(Conflict cause, List<ASPRule<?>> add, List<ASPRule<?>> modify, List<ASPRule<?>> delete)
    {
        if(cause == null)
            LOGGER.warn("A solution should be linked to its conflict, but wasn't set in the instantiation! This might lead to problems in the further execution!");
        this.cause = cause;

        this.setChosenRuleMapping(null);
        this.setAddedRules(add);
        this.setModifiedRules(modify);
        this.setDeletedRules(delete);

        addVariants = new HashMap<>();
        modifyVariants = new HashMap<>();
        deleteVariants = new HashMap<>();
        metaData = new HashMap<>();
            
        if(chosenRuleMapping.get(OperationTypeEnum.ADD).size() == 0
        && chosenRuleMapping.get(OperationTypeEnum.MODIFY).size() == 0
        && chosenRuleMapping.get(OperationTypeEnum.DELETE).size() == 0)
            LOGGER.warn("A solution was instantiated without any rule operations! This might lead to problems in the further execution!");
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Metadata");
        sb.append(System.lineSeparator());
        for (SolutionMetaDataEnum metadataKey : metaData.keySet())
        {
            sb.append(metadataKey + ": ");
            sb.append(metaData.get(metadataKey));
            sb.append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        if(!chosenRuleMapping.get(OperationTypeEnum.ADD).isEmpty())
        {
            sb.append("Add the following rule(s):");
            sb.append(System.lineSeparator());
            
            for (ASPRule<?> currentRule : chosenRuleMapping.get(OperationTypeEnum.ADD))
            {
                sb.append(currentRule.toString());
                sb.append(System.lineSeparator());

                if(addVariants.get(currentRule.getID()) != null)
                {
                    List<ASPRule<?>> variantList = addVariants.get(currentRule.getID());
                    for (int i = 0; i < variantList.size(); i++)
                    {
                        sb.append("\t" + i + ".Variant: ");
                        sb.append(variantList.get(i).toString());
                        sb.append(System.lineSeparator());
                    }
                }
            }
            sb.append(System.lineSeparator());
        }

        if(!chosenRuleMapping.get(OperationTypeEnum.MODIFY).isEmpty())
        {
            sb.append("Modify the following rule(s):");
            sb.append(System.lineSeparator());
            
            for (ASPRule<?> currentRule : chosenRuleMapping.get(OperationTypeEnum.MODIFY))
            {
                sb.append(String.format("Change rule r%s to ", currentRule.getLabelID()));
                sb.append(currentRule.toString());
                sb.append(System.lineSeparator());

                List<ASPRule<?>> variantList = modifyVariants.get(currentRule.getID());
                if(modifyVariants.get(currentRule.getID()) != null)
                {
                    for (int i = 0; i < variantList.size(); i++)
                    {
                        sb.append("\t" + i + ".Variant: ");
                        sb.append(variantList.get(i).toString());
                        sb.append(System.lineSeparator());
                    }
                }
            }
            sb.append(System.lineSeparator());
        }

        if(!chosenRuleMapping.get(OperationTypeEnum.DELETE).isEmpty())
        {
            sb.append("Delete the following rule(s):");
            sb.append(System.lineSeparator());
            
            for (ASPRule<?> currentRule : chosenRuleMapping.get(OperationTypeEnum.DELETE))
            {
                sb.append(currentRule.toString());
                sb.append(System.lineSeparator());

                if(deleteVariants.get(currentRule.getID()) != null)
                {
                    List<ASPRule<?>> variantList = deleteVariants.get(currentRule.getID());
                    for (int i = 0; i < variantList.size(); i++)
                    {
                        sb.append("\t" + i + ".Variant: ");
                        sb.append(variantList.get(i).toString());
                        sb.append(System.lineSeparator());
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Adds the given rule with the given operation to the solution. If the rule (a rule with the same ID) is already marked as chosen,
     * this older chosen rule is moved to the variants. Therefore, the given rule is always included in the chosen operations and will be applied.
     * @param chosenRule Rule which is added to the solution
     * @param operation Operation which is performed by the given rule
     * @return True, if the given rule was successfully added to the chosen operations. False otherwise
     */
    public boolean addChosenRule(ASPRule<?> chosenRule, OperationTypeEnum operation)
    {
        boolean success = false;
        ASPRule<?> oldRule = chosenRuleMapping.get(operation).stream()
            .filter(x -> x.getID().equals(chosenRule.getID()))
            .findFirst()
            .orElse(null);

        if(oldRule == null)
        {
            success = chosenRuleMapping.get(operation).add(chosenRule);
        }
        else
        {
            int previousPosition;
            switch (operation)
            {
                case ADD:
                    if(addVariants.containsKey(oldRule.getID()))
                    {
                        addVariants.get(oldRule.getID()).add(0, oldRule);
                    }
                    else
                    {
                        addVariants.put(oldRule.getID(), new ArrayList<ASPRule<?>>(){{ add(oldRule); }});
                    }
                    previousPosition = chosenRuleMapping.get(operation).indexOf(oldRule);
                    chosenRuleMapping.get(operation).remove(oldRule);
                    chosenRuleMapping.get(operation).add(previousPosition, chosenRule);

                    success = true;
                    LOGGER.info("The rule 'r{}: {}' replaced the previously chosen rule '{}', which was added to the add-variants.",
                        chosenRule.getLabelID(), chosenRule.toString(), oldRule.toString());
                    break;
                case MODIFY:
                    if(modifyVariants.containsKey(oldRule.getID()))
                    {
                        modifyVariants.get(oldRule.getID()).add(0, oldRule);
                    }
                    else
                    {
                        modifyVariants.put(oldRule.getID(), new ArrayList<ASPRule<?>>(){{ add(oldRule); }});
                    }
                    previousPosition = chosenRuleMapping.get(operation).indexOf(oldRule);
                    chosenRuleMapping.get(operation).remove(oldRule);
                    chosenRuleMapping.get(operation).add(previousPosition, chosenRule);

                    success = true;
                    LOGGER.info("The rule 'r{}: {}' replaced the previously chosen rule '{}', which was added to the modify-variants.",
                        chosenRule.getLabelID(), chosenRule.toString(), oldRule.toString());
                    break;
                case DELETE:
                    if(deleteVariants.containsKey(oldRule.getID()))
                    {
                        deleteVariants.get(oldRule.getID()).add(0, oldRule);
                    }
                    else
                    {
                        deleteVariants.put(oldRule.getID(), new ArrayList<ASPRule<?>>(){{ add(oldRule); }});
                    }
                    previousPosition = chosenRuleMapping.get(operation).indexOf(oldRule);
                    chosenRuleMapping.get(operation).remove(oldRule);
                    chosenRuleMapping.get(operation).add(previousPosition, chosenRule);

                    success = true;
                    LOGGER.info("The rule 'r{}: {}' replaced the previously chosen rule '{}', which was added to the delete-variants.",
                        chosenRule.getLabelID(), chosenRule.toString(), oldRule.toString());
                    break;
                default:
                    success = false;
                    break;
            }
        }
        return success;
    }

    /**
     * Selects the given rule as a chosen rule. The currently chosen operation of the rule will be switched with the chosen variant.
     * The given rule has to already exist as a rule-variant of this object. If you want to add a completely new operation to this solution,
     * the method {@link #addChosenRule(ASPRule, OperationTypeEnum)} has to be used.
     * @param chosenRule Variant which already exists in this solution-object and is supposed to be selected as chosen
     * @return True if the given rule was successfully selected as a chosen rule.
     * False otherwise, e.g. if the given rule doesn't already exists as a variant in the object.
     */
    public boolean chooseVariant(ASPRule<?> chosenRule)
    {
        if(chosenRule == null)
            return false;

        if(chosenRuleMapping.get(OperationTypeEnum.ADD).contains(chosenRule)
        || chosenRuleMapping.get(OperationTypeEnum.MODIFY).contains(chosenRule)
        || chosenRuleMapping.get(OperationTypeEnum.DELETE).contains(chosenRule))
        {
            LOGGER.info("The chosen Variant is already chosen. Therefore, the solution-change request for rule {} has no effect.",
                        chosenRule.getLabelID() + chosenRule.toString());
            return true;
        }

        boolean success = switchRules(chosenRuleMapping.get(OperationTypeEnum.ADD) , addVariants, chosenRule);

        if(!success)
            success = switchRules(chosenRuleMapping.get(OperationTypeEnum.MODIFY) , modifyVariants, chosenRule);
        else
        {
            LOGGER.info("The rule-variant 'r{}: {}' was chosen and will be added when the solution is applied.", chosenRule.getLabelID(), chosenRule.toString());
            return true;
        }
        
        if(!success)
            success = switchRules(chosenRuleMapping.get(OperationTypeEnum.DELETE) , deleteVariants, chosenRule);
        else
        {
            LOGGER.info("The rule-variant 'r{}: {}' was chosen and will be modified when the solution is applied.", chosenRule.getLabelID(), chosenRule.toString());
            return true;
        }

        if(success)
        {
            LOGGER.info("The rule-variant 'r{}: {}' was chosen and will be deleted when the solution is applied.", chosenRule.getLabelID(), chosenRule.toString());
            return true;
        }
        else
        {
            LOGGER.warn("The rule-variant 'r{}: {}' couldn't be added to the chosen rules of the solution.", chosenRule.getLabelID(), chosenRule.toString());
            return false;
        }
    }

    /** 
     * Internal method to perform the switching of a chosen rule and a rule variant
     * @return True, if the given rule could successfully be switched with a corresponding chosen rule.
     * False, if the given list of chosen rules doesn't contain a rule with the same ID as the given rule
     */
    private boolean switchRules(List<ASPRule<?>> chosenList, HashMap<String, List<ASPRule<?>>> variant, ASPRule<?> chosenRule)
    {
        ASPRule<?> previous = chosenList.stream()
            .filter(x -> x.getID().equals(chosenRule.getID()))
            .findFirst()
            .orElse(null);

        if(previous == null)
            return false;

        int previousPosition = chosenList.indexOf(previous);
        chosenList.remove(previous);
        chosenList.add(previousPosition, chosenRule);

        if(variant.get(previous.getID()) != null)
        {
            variant.get(previous.getID()).remove(chosenRule);
            variant.get(previous.getID()).add(0, previous);
        }
        else
        {
            List<ASPRule<?>> previousRuleList = new ArrayList<>();
            previousRuleList.add(previous);
            variant.put(previous.getID(), previousRuleList);
        }

        return true;
    }

    /**
     * Convenient helper-method to add a new entry to the meta-data.
     * It is highly recommended to only safe values according to the class-type suggested in {@link de.aspua.framework.Utils.OperationTypeEnum}!
     * If a class-type other than the suggested is used, it is possible that critical runtime-exceptions occure!
     * @param key Key for the meta-data map
     * @param value Value for the meta-data map
     * @return The previous value associated with the key
     */
    public Object addMetaData(SolutionMetaDataEnum key, Object value) {
        return this.metaData.put(key, value);
    }

    /**
     * Creates a deep copy of the current object. <p>
     * The connected conflict of the solution-object isn't cloned because
     * this method may be invoked by the {@link Conflict#createNewInstance()}. Therefore, the copied solution-object will be connected
     * to the given conflict-object in order to prevent infinite loops during runtime. <p>
     * The method has to be refactored as soon as new {@link de.aspua.framework.Utils.OperationTypeEnum}-values are introduced! As the classtypes of the
     * enum-values have to copied manually due to type-saefty, the method has to be extended for each new introduced 
     * {@link de.aspua.framework.Utils.OperationTypeEnum}-key in the meta-data attribute.
     * @param rootConflict Conflict-object which the copied solution-object will be connected to
     * @return The created deep copy
     */
    public Solution createNewInstance(Conflict rootConflict)
    {
        List<ASPRule<?>> newAdd = new ArrayList<>();
        for (ASPRule<?> rule : chosenRuleMapping.get(OperationTypeEnum.ADD))
            newAdd.add(rule.createNewInstance());    

        List<ASPRule<?>> newModify = new ArrayList<>();
        for (ASPRule<?> rule : chosenRuleMapping.get(OperationTypeEnum.MODIFY))
            newModify.add(rule.createNewInstance());    

        List<ASPRule<?>> newDelete = new ArrayList<>();
        for (ASPRule<?> rule : chosenRuleMapping.get(OperationTypeEnum.DELETE))
            newDelete.add(rule.createNewInstance());    

        Solution newSolution = new Solution(rootConflict, newAdd, newModify, newDelete);

        // Create a Deep Copy of all key-value pairs and set the new HashMap in newSolution
        List<HashMap<String, List<ASPRule<?>>>> variants = new ArrayList<>();
        variants.add(addVariants);
        variants.add(modifyVariants);
        variants.add(deleteVariants);

        for(int i = 0; i < variants.size(); i++)
        {
            if(variants.get(i).isEmpty())
                continue;

            HashMap<String,List<ASPRule<?>>> currentVariant = variants.get(i);
            HashMap<String, List<ASPRule<?>>> newVariants = new HashMap<>();

            for (String ruleID : currentVariant.keySet())
            {
                List<ASPRule<?>> newRuleVariantList = new ArrayList<>();
                for (ASPRule<?> ruleVariant : currentVariant.get(ruleID))
                    newRuleVariantList.add(ruleVariant.createNewInstance());

                newVariants.put(ruleID, newRuleVariantList);
            }

            // Switch-cases depend on the order, in which the variants were added in the variants-List
            switch (i)
            {
                case 0:
                    newSolution.setAddVariants(newVariants);
                    break;
                case 1:
                    newSolution.setModifyVariants(newVariants);
                    break;
                case 2:
                    newSolution.setDeleteVariants(newVariants);
                    break;
                default:
                    break;
            }
        }

        if(!metaData.isEmpty())
        {
            HashMap<SolutionMetaDataEnum, Object> newMetaData = new HashMap<>();
    
            for (SolutionMetaDataEnum metaDataKey : metaData.keySet())
            {
                switch (metaDataKey) {
                    // Maps to String. Because primitive-parameters are copied anyway, the data can be copied directly.
                    case ROOTRULE:
                        newMetaData.put(metaDataKey, metaData.get(metaDataKey));
                        break;
                    // Maps to a Literal<?>. Therefore, the Literal has to be copied by its copy-Method.
                    case TARGETLITERAL:
                        ASPLiteral<?> targetLiteral = (ASPLiteral<?>) metaData.get(metaDataKey);
                        newMetaData.put(metaDataKey, targetLiteral.createNewInstance());
                        break;
                    case MEASURE_ANSWERSETCHANGES:
                        newMetaData.put(metaDataKey, metaData.get(metaDataKey));
                        break;
                    case MEASURE_RULECHANGES:
                        newMetaData.put(metaDataKey, metaData.get(metaDataKey));
                        break;
                    default:
                        LOGGER.warn("An unknown Metadata-Key was detected while attempting to create a new Solution-Instance! " +
                                    "Unknown metadata won't be copied to the new instance!");
                        break;
                }
            }

            newSolution.setMetaData(newMetaData);
        }

        return newSolution;
    }

    public List<ASPRule<?>> getAddedRules() {
        return chosenRuleMapping.get(OperationTypeEnum.ADD);
    }

    public void setAddedRules(List<ASPRule<?>> addedRules)
    {
        if(addedRules == null)
            chosenRuleMapping.put(OperationTypeEnum.ADD, new ArrayList<>());
        else
            chosenRuleMapping.put(OperationTypeEnum.ADD, addedRules);
    }

    public List<ASPRule<?>> getModifiedRules() {
        return chosenRuleMapping.get(OperationTypeEnum.MODIFY);
    }

    public void setModifiedRules(List<ASPRule<?>> modifiedRules)
    {
        if(modifiedRules == null)
            chosenRuleMapping.put(OperationTypeEnum.MODIFY, new ArrayList<>());
        else
            chosenRuleMapping.put(OperationTypeEnum.MODIFY, modifiedRules);
    }

    public List<ASPRule<?>> getDeletedRules() {
        return chosenRuleMapping.get(OperationTypeEnum.DELETE);
    }

    public void setDeletedRules(List<ASPRule<?>> deletedRules)
    {
        if(deletedRules == null)
            chosenRuleMapping.put(OperationTypeEnum.DELETE, new ArrayList<>());
        else
            chosenRuleMapping.put(OperationTypeEnum.DELETE, deletedRules);
    }

    public Conflict getCause() {
        return cause;
    }

    public HashMap<String, List<ASPRule<?>>> getAddVariants() {
        return addVariants;
    }

    public void setAddVariants(HashMap<String, List<ASPRule<?>>> addVariants)
    {
        if(addVariants == null)
            this.addVariants = new HashMap<>();
        else
            this.addVariants = addVariants;
    }

    public HashMap<String, List<ASPRule<?>>> getModifyVariants() {
        return modifyVariants;
    }

    public void setModifyVariants(HashMap<String, List<ASPRule<?>>> modifyVariants)
    {
        if(modifyVariants == null)
            this.modifyVariants = new HashMap<>();
        else
            this.modifyVariants = modifyVariants;
    }

    public HashMap<String, List<ASPRule<?>>> getDeleteVariants() {
        return deleteVariants;
    }

    public void setDeleteVariants(HashMap<String, List<ASPRule<?>>> deleteVariants)
    {
        if(deleteVariants == null)
            this.deleteVariants = new HashMap<>();
        else
            this.deleteVariants = deleteVariants;
    }

    public HashMap<SolutionMetaDataEnum, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(HashMap<SolutionMetaDataEnum, Object> metaData) {
        this.metaData = metaData;
    }

    public HashMap<OperationTypeEnum, List<ASPRule<?>>> getChosenRuleMapping() {
        return chosenRuleMapping;
    }

    public void setChosenRuleMapping(HashMap<OperationTypeEnum, List<ASPRule<?>>> chosenRuleMapping)
    {
        if(chosenRuleMapping == null)
        {
            this.chosenRuleMapping = new HashMap<>();
            this.chosenRuleMapping.put(OperationTypeEnum.ADD, new ArrayList<>());
            this.chosenRuleMapping.put(OperationTypeEnum.MODIFY, new ArrayList<>());
            this.chosenRuleMapping.put(OperationTypeEnum.DELETE, new ArrayList<>());
        }
        else
        {
            this.chosenRuleMapping = chosenRuleMapping;
            if(this.chosenRuleMapping.get(OperationTypeEnum.ADD) == null)
                this.chosenRuleMapping.put(OperationTypeEnum.ADD, new ArrayList<>());

            if(this.chosenRuleMapping.get(OperationTypeEnum.MODIFY) == null)
                this.chosenRuleMapping.put(OperationTypeEnum.MODIFY, new ArrayList<>());

            if(this.chosenRuleMapping.get(OperationTypeEnum.DELETE) == null)
                this.chosenRuleMapping.put(OperationTypeEnum.DELETE, new ArrayList<>());
        }
    }
}
