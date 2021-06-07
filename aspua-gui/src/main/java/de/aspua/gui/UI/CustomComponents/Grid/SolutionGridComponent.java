package de.aspua.gui.UI.CustomComponents.Grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.OperationTypeEnum;
import de.aspua.gui.UI.CustomComponents.ValidationNotification;

/**
 * Custom component to display and model solutions.
 * Inherits all functionalities and and properties of the editableGrid, but some functionalitites such as filtering are not supported.
 * @see EditableGridComponent
 */
public class SolutionGridComponent extends EditableGridComponent
{
    private static Logger LOGGER = LoggerFactory.getLogger(SolutionGridComponent.class);
    
    /** Stored data */
    protected Solution solution;
    protected TreeData<ELPRule> treeData;
    protected List<ELPRule> rootRules;

    public SolutionGridComponent(Solution solution, ASPProgram<?, ?> aspSource)
    {
        super(null, aspSource, null);
        super.addFooterRow(true);

        treeData = this.getTreeData();
        this.displaySolution(solution);
        this.configureClickListener();
    }

    /** Displays the given Solution-object with the corresponding operations */
    public void displaySolution(Solution displayedSolution)
    {
        this.setSolution(displayedSolution);
        treeData.clear();

        this.displayRuleTree(solution.getAddedRules(), solution.getAddVariants(), OperationTypeEnum.ADD);
        this.displayRuleTree(solution.getModifiedRules(), solution.getModifyVariants(), OperationTypeEnum.MODIFY);
        this.displayRuleTree(solution.getDeletedRules(), solution.getDeleteVariants(), OperationTypeEnum.DELETE);
    }

    /** Separates the solution into root-rules for the currently selected rule-changes and sets all variants as child-rows */
    private void displayRuleTree(List<ASPRule<?>> untypedRules, HashMap<String, List<ASPRule<?>>> untypedVariants, OperationTypeEnum operation)
    {
        // Get root-rules
        rootRules = new ArrayList<>();
        for (ASPRule<?> currentRule : untypedRules)
        {
            ELPRule typedRule = (ELPRule) currentRule;
            rootRules.add(typedRule);
            ruleOperationMapping.put(currentRule.getLabelID(), operation);
        }

        // Collect modifications
        HashMap<String, List<ELPRule>> ruleVariants = new HashMap<>();
        for (String ruleID : untypedVariants.keySet())
        {
            List<ELPRule> typedVariants = new ArrayList<>();
            for (ASPRule<?> currentRule : untypedVariants.get(ruleID))
            {
                typedVariants.add((ELPRule) currentRule);
                ruleOperationMapping.put(currentRule.getLabelID(), operation);
            }
            ruleVariants.put(ruleID, typedVariants);
        }

        treeData.addRootItems(rootRules);
        for (ELPRule rootRule : rootRules)
        {
            List<ELPRule> variants = ruleVariants.get(rootRule.getID());
            
            if(variants != null)
                treeData.addItems(rootRule, variants);
        }
        this.getDataProvider().refreshAll();
    }

    private void configureClickListener()
    {
        // Select variant as root-rule if it is doubleclicked
        this.addItemDoubleClickListener(e ->
        {
            ELPRule clickedRule = e.getItem();
            if(treeData.getRootItems().contains(clickedRule))
                return;

            boolean success = solution.chooseVariant(clickedRule);
            if(!success)
            {
                new ValidationNotification("The clicked rule couldn't be chosen as a primary rule. Please try again or enter the rule manually.", true)
                .open();
            }

            this.displaySolution(solution);
            this.expand(clickedRule);
        });

        // Copy literals of entered rule to footer-row
        this.addSelectionListener(e -> 
        {
            ELPRule selectedRule = e.getFirstSelectedItem().orElse(null);
            if(selectedRule != null)
            {
                ruleIDComboBox.setValue(selectedRule.getLabelID());
                super.setFooterRowValues(selectedRule);
            }
            else
                ruleIDComboBox.setValue(null);
        });
    }


    @Override
    protected void deleteRuleFromGrid(ELPRule rule)
    { 
        boolean removed = false;

        // Check if deleted rule is a root rule which was supposed to be added
        if(solution.getAddedRules().contains(rule)
        || solution.getAddVariants().containsKey(rule.getID()))
        {
            removed = solution.getAddedRules().remove(rule);
            if(removed)
            {
                aspProgram.deleteRule(rule.getID());
                updateSequence.deleteRule(rule.getID());
                ruleIDComboBox.setValue(null);
            }

            List<ASPRule<?>> variants = solution.getAddVariants().get(rule.getID());
            if(variants != null && !variants.isEmpty())
            {
                // If the root rule was removed and variants exist: Remove all variants
                if(removed)
                    solution.getAddVariants().remove(rule.getID());
                // If the root rule wasn't the rule which should be removed: Try to remove the rule from the variants
                else
                    removed = variants.remove(rule);
            }
        }
        // Check if deleted rule is a root rule which was supposed to be modified
        if(!removed && solution.getModifiedRules().contains(rule)
        || solution.getModifyVariants().containsKey(rule.getID()))
        {
            removed = solution.getModifiedRules().remove(rule);

            List<ASPRule<?>> variants = solution.getModifyVariants().get(rule.getID());
            if(variants != null && !variants.isEmpty())
            {
                // If the root rule was removed and variants exist: Remove all variants
                if(removed)
                    solution.getModifyVariants().remove(rule.getID());
                // If the root rule wasn't the rule which should be removed: Try to remove the rule from the variants
                else
                    removed = variants.remove(rule);
            }
        }
        // Check if deleted rule is a root rule which was supposed to be deleted
        if(!removed && solution.getDeletedRules().contains(rule)
        || solution.getDeleteVariants().containsKey(rule.getID()))
        {
            removed = solution.getDeletedRules().remove(rule);

            List<ASPRule<?>> variants = solution.getDeleteVariants().get(rule.getID());
            if(variants != null && !variants.isEmpty())
            {
                // If the root rule was removed and variants exist: Remove all variants
                if(removed)
                    solution.getDeleteVariants().remove(rule.getID());
                // If the root rule wasn't the rule which should be removed: Try to remove the rule from the variants
                else
                    removed = variants.remove(rule);
            }
        }

        if(!removed)
            LOGGER.warn("The rule {} wasn't found in the displayed solution and therefore wasn't removed from the solution-grid.", rule.toString());
        else
            ruleOperationMapping.remove(rule.getLabelID());
        
        this.displaySolution(solution);
    }

    @Override
    protected void addGridRule()
    {
        ELPRule newRule = this.getEnteredRule();
        if(!this.validateInput(newRule))
            return;

        List<ELPRule> rootItems = treeData.getRootItems();
        ELPRule oldRule = rootItems.stream()
        .filter(x -> x.getLabelID() == newRule.getLabelID())
        .findFirst()
        .orElse(null);
        
        if(oldRule != null)
        {
            OperationTypeEnum oldOperation = ruleOperationMapping.get(oldRule.getLabelID());
            boolean sameOperation = operationTranslations.get(oldOperation).equals(operationSelect.getValue());

            // If the operation, the labelID and the literals (checked in contains) are equal, nothing is changed
            if(sameOperation && treeData.contains(newRule))
                return;

            // If rules with same labelID & operation exist, just set the added rule as the new root rule instead of overwriting all variants
            if(sameOperation)
            {
                newRule.setID(oldRule.getID());
                solution.addChosenRule(newRule, oldOperation);
            }
            // Only happens if MODIFY -> DELETE or DELETE -> MODIFY, as ADD gets validated (no old rule with the same labelID can be created)
            else
            {
                // Delete existing rules & variants, as they are useless if another operation is used
                this.deleteRuleFromGrid(oldRule);

                if(operationTranslations.get(OperationTypeEnum.MODIFY).equals(operationSelect.getValue()))
                {
                    solution.getModifiedRules().add(newRule);
                    ruleOperationMapping.put(newRule.getLabelID(), OperationTypeEnum.MODIFY);
                }
                else if(operationTranslations.get(OperationTypeEnum.DELETE).equals(operationSelect.getValue()))
                {
                    solution.getDeletedRules().add(newRule);
                    ruleOperationMapping.put(newRule.getLabelID(), OperationTypeEnum.DELETE);
                }
            }
        }
        else
        {
            if(operationTranslations.get(OperationTypeEnum.ADD).equals(operationSelect.getValue()))
            {
                solution.getAddedRules().add(newRule);
                ruleOperationMapping.put(newRule.getLabelID(), OperationTypeEnum.ADD);
                updateSequence.addRule(newRule);
                ruleIDComboBox.setValue(null);
            }
            else if(operationTranslations.get(OperationTypeEnum.MODIFY).equals(operationSelect.getValue()))
            {
                solution.getModifiedRules().add(newRule);
                ruleOperationMapping.put(newRule.getLabelID(), OperationTypeEnum.MODIFY);
            }
            else if(operationTranslations.get(OperationTypeEnum.DELETE).equals(operationSelect.getValue()))
            {
                solution.getDeletedRules().add(newRule);
                ruleOperationMapping.put(newRule.getLabelID(), OperationTypeEnum.DELETE);
            }
        }
        
        aspProgram.addRule(newRule);
        this.displaySolution(this.solution);
    }

    public Solution getSolution() {
        return solution;
    }

    public void setSolution(Solution solution) {
        if(solution == null)
            this.solution = new Solution(null, null, null, null);
        else
            this.solution = solution;
    }
}
