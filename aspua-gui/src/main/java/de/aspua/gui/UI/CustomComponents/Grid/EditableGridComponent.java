package de.aspua.gui.UI.CustomComponents.Grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.treegrid.TreeGrid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.gatanaso.MultiselectComboBox;

import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.OperationTypeEnum;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;

/**
 * Custom component to allow the modeling of new ASP-rules (ELPs) and show a new column for rule-operations.
 * Inherits all functionalities and and properties of the baseGrid.
 * @see BaseGridComponent
 */
public class EditableGridComponent extends BaseGridComponent
{
    private static Logger LOGGER = LoggerFactory.getLogger(EditableGridComponent.class);
    protected ASPUAFrameworkAdapter valueServieProvider;

    /** Components */
    // New column for showing the opration which would be caused by the row-entry (Add, modify or delete)
    protected TreeGrid.Column<ELPRule> operationColumn;
    // Footer row wraps components for modeling a new rule
    protected FooterRow footerRow;
    // Context menu for deleting modeled rules from grid
    protected GridContextMenu<ELPRule> contextMenu;

    // Components in Footer-Row
    protected ComboBox<Integer> ruleIDComboBox;
    protected MultiselectComboBox<ELPLiteral> conditionsMultiSelect;
    protected MultiselectComboBox<ELPLiteral> justificationsMultiSelect;
    protected ComboBox<ELPLiteral> conclusionComboBox;
    protected Select<String> operationSelect;
    protected Button applyButton;

    // FooterRow's visibility/enabled-state can't be controlled. Therefore, wrapper-Divs are used (Layout for combined operation-Column)
    protected Div ruleIDComboBoxWrapper;
    protected Div conditionsMultiSelectWrapper;
    protected Div justificationsMultiSelectWrapper;
    protected HorizontalLayout conclusionComboBoxWrapper;
    protected HorizontalLayout operationLayoutWrapper;
    
    /** Stored data */
    // Represents the program which contains all rules that can be deleted or modified (corresponds to the initial program of a update sequence)
    protected ELPProgram aspSource;
    // Saves all rules from the aspSource as well as all grid-rules with operation ADD. The updateSequence also provides all usable literals
    protected ELPProgram updateSequence;
    // Used to highlight conflicting rules in the operationColumn
    protected List<ELPRule> conflictingRules;
    // Saves all usable literals based on the updateSequence (convenient helper attribute)
    protected List<ELPLiteral> availableLiterals;
    // Used for displaying the operation-value in the operationColumn and style the rows with CSS-classed depending on mapping-type
    protected Map<Integer, OperationTypeEnum> ruleOperationMapping;
    // Maps the enum-values to actuals labels (from current locale). Can be used whenever the current value of the operationSelect has to be read or written
    protected Map<OperationTypeEnum, String> operationTranslations;

    public EditableGridComponent(ASPProgram<?, ?> aspProgram, ASPProgram<?, ?> aspSource, HashMap<Integer, OperationTypeEnum> operationMapping)
    {
        super(aspProgram);

        if(aspSource == null)
            this.aspSource = super.aspProgram.createNewInstance();
        else
            this.aspSource = (ELPProgram) aspSource;
            
        if(operationMapping == null)
            this.ruleOperationMapping = new HashMap<>();
        else
            this.ruleOperationMapping = operationMapping;

        // Initialize translations at runtime, as locale isn't static
        operationTranslations = new LinkedHashMap<>();
        operationTranslations.put(OperationTypeEnum.ADD, getTranslation("select.ruleOperation.add"));
        operationTranslations.put(OperationTypeEnum.MODIFY, getTranslation("select.ruleOperation.modify"));
        operationTranslations.put(OperationTypeEnum.DELETE, getTranslation("select.ruleOperation.delete"));
        
        // Add all initially displayed rules to the list of updateSequence, which have to checked for duplicates if new rules are added to aspProgram
        updateSequence = this.aspSource.createNewInstance();
        for (ASPRule<?> rule : this.aspProgram.getRuleSet())
        {
            if(!updateSequence.getRuleSet().contains(rule))
                updateSequence.addRule((ELPRule) rule);
        }

        conflictingRules = new ArrayList<>();
        availableLiterals = new ArrayList<>(updateSequence.getLiteralBase().keySet());
    }

    private void configureOperationColumnProperties()
    {
        this.removeThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        // Add CSS-classes from grid-styles.css for coloring rows depending on the operation
        this.setClassNameGenerator(elpRule -> 
        {
            OperationTypeEnum type = ruleOperationMapping.get(elpRule.getLabelID());
            if(type == null)
                return "";
            
            switch (type) {
                case ADD:
                    return "add";
                case MODIFY:
                    return "modify";
                case DELETE:
                    return "delete";
                default:
                    return "";
            }
        });
    }

    /**
     * Adds a new column at the most right position to display the operation of each row.
     * Has to be explicitly invoked and isn't performed automatically by the component.
     */
    public void addOperationColumn()
    {
        // Add classNameGenerator for CSS
        this.configureOperationColumnProperties();

        conclusionColumn.setFlexGrow(4);
        operationColumn = this.addComponentColumn(elpRule ->
            {
                HorizontalLayout columnLayout = new HorizontalLayout();
                columnLayout.setMargin(false);
                columnLayout.setWidthFull();
                columnLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
                
                OperationTypeEnum operation = ruleOperationMapping.get(elpRule.getLabelID());
                if(operation != null)
                {
                    Span operationSpan = new Span();
                    
                    if(OperationTypeEnum.ADD.equals(operation))
                        operationSpan = new Span(getTranslation("select.ruleOperation.add"));
                    else if(OperationTypeEnum.MODIFY.equals(operation))
                        operationSpan = new Span(getTranslation("select.ruleOperation.modify"));
                    else if(OperationTypeEnum.DELETE.equals(operation))
                        operationSpan = new Span(getTranslation("select.ruleOperation.delete"));
                    
                    columnLayout.add(operationSpan);
                }

                boolean conflicting = conflictingRules.contains(elpRule);
                if(conflicting)
                {
                    Span conflictSpan = new Span(getTranslation("select.ruleOperation.conflicting"));
                    conflictSpan.getStyle().set("font-weight", "bold");
                    conflictSpan.getStyle().set("margin-left", "auto");
                    columnLayout.add(conflictSpan);
                }

                return columnLayout;
            })
            .setHeader(getTranslation("label.ASP.operation"))
            .setKey("operation")
            .setSortable(true)
            .setFlexGrow(1)
            .setResizable(true);
    }

    /**
     * Adds a footer row to enable the modeling of new rules.
     * Has to be explicitly invoked and isn't performed automatically by the component.
     * @param operationColumnVisible Determines if the column-row should be enabled and therefore different rule operations can be chosen while modeling
     */
    public void addFooterRow(boolean operationColumnVisible)
    {        
        this.configureFooterComponents();
        
        // Initialize wrapper with coresponding components
        ruleIDComboBoxWrapper = new Div(ruleIDComboBox);
        conditionsMultiSelectWrapper = new Div(conditionsMultiSelect);
        justificationsMultiSelectWrapper = new Div(justificationsMultiSelect);
        conclusionComboBoxWrapper = new HorizontalLayout(conclusionComboBox);

        // Set wrapper to footer-cells
        footerRow = this.prependFooterRow();
        footerRow.getCell(ruleIDColumn).setComponent(ruleIDComboBoxWrapper);
        footerRow.getCell(conditionsColumn).setComponent(conditionsMultiSelectWrapper);
        footerRow.getCell(justificationsColumn).setComponent(justificationsMultiSelectWrapper);
        footerRow.getCell(conclusionColumn).setComponent(conclusionComboBoxWrapper);

        if(operationColumnVisible)
        {
            if(operationColumn == null)
                this.addOperationColumn();

            operationLayoutWrapper = new HorizontalLayout(operationSelect, applyButton);
            operationLayoutWrapper.setJustifyContentMode(JustifyContentMode.CENTER);
            footerRow.getCell(operationColumn).setComponent(operationLayoutWrapper);

            // Enlarge operationColumn to fit components in footerRow
            operationColumn.setFlexGrow(3);
        }
        else
        {
            // Set Add as only operation if no distinction between operations is made
            applyButton.setText(getTranslation("select.ruleOperation.add"));
            applyButton.setWidth("75px");
            conclusionComboBoxWrapper = new HorizontalLayout(conclusionComboBox, applyButton);
            conclusionComboBoxWrapper.setJustifyContentMode(JustifyContentMode.CENTER);
            footerRow.getCell(conclusionColumn).setComponent(conclusionComboBoxWrapper);
        }

        this.enableFooterRowComponents(true);
    }

    protected void configureFooterComponents()
    {
        this.configureOperationSelect();
        this.configureRuleIDComboBox();

        conditionsMultiSelect = new MultiselectComboBox<>();
        conditionsMultiSelect.setWidthFull();
        conditionsMultiSelect.setItems(availableLiterals);
        conditionsMultiSelect.setClearButtonVisible(true);
        conditionsMultiSelect.setPlaceholder(getTranslation("placeholder.ASP.literal"));
        conditionsMultiSelect.addValueChangeListener(e -> 
            {
                conditionsMultiSelect.setInvalid(false);
                justificationsMultiSelect.setInvalid(false);
                conclusionComboBox.setInvalid(false);

                if(e.getValue() == null || e.getValue().isEmpty())
                    conditionsMultiSelect.setPlaceholder(getTranslation("placeholder.ASP.literal"));
                else
                    conditionsMultiSelect.setPlaceholder(null);
            });

        justificationsMultiSelect = new MultiselectComboBox<>();
        justificationsMultiSelect.setWidthFull();
        justificationsMultiSelect.setItems(availableLiterals);
        justificationsMultiSelect.setClearButtonVisible(true);
        justificationsMultiSelect.setPlaceholder(getTranslation("placeholder.ASP.literal"));
        justificationsMultiSelect.addValueChangeListener(e ->
            {
                conditionsMultiSelect.setInvalid(false);
                justificationsMultiSelect.setInvalid(false);
                conclusionComboBox.setInvalid(false);

                if(e.getValue() == null || e.getValue().isEmpty())
                    justificationsMultiSelect.setPlaceholder(getTranslation("placeholder.ASP.literal"));
                else
                    justificationsMultiSelect.setPlaceholder(null);
            });

        conclusionComboBox = new ComboBox<>();
        conclusionComboBox.setWidthFull();
        conclusionComboBox.setItems(availableLiterals);
        conclusionComboBox.setClearButtonVisible(true);
        conclusionComboBox.setPlaceholder(getTranslation("placeholder.ASP.literal"));
        conclusionComboBox.addValueChangeListener(e ->
            {
                conditionsMultiSelect.setInvalid(false);
                justificationsMultiSelect.setInvalid(false);
                conclusionComboBox.setInvalid(false);
            });

        applyButton = new Button(getTranslation("button.apply"));
        applyButton.setWidth("50px");
        applyButton.addClickListener(e -> this.addGridRule());
    }

    protected void configureRuleIDComboBox()
    {
        ruleIDComboBox = new ComboBox<>();
        ruleIDComboBox.setWidthFull();
        ruleIDComboBox.setClearButtonVisible(true);

        List<Integer> ruleIDs = new ArrayList<>();
        for (ELPRule currentRule : aspProgram.getRuleSet())
            ruleIDs.add(currentRule.getLabelID());

        ruleIDComboBox.setItems(ruleIDs);
        ruleIDComboBox.addValueChangeListener(e ->
        {
            //Suggest the next availabe Integer as a possible rule ID (Starting at the last position of the initial program, i.e. aspSource)
            if(e.getValue() == null)
            {
                if(operationTranslations.get(OperationTypeEnum.ADD).equals(operationSelect.getValue()))
                {
                    int newValue = aspSource.getRuleSet().size() - 1;
                    while(updateSequence.getRuleByLabelID(newValue) != null)
                        newValue++;
                    
                    ruleIDComboBox.setValue(newValue);
                }
                else
                    ruleIDComboBox.setValue(updateSequence.getRuleSet().get(0).getLabelID());

                return;
            }
            
            // Check if entered ID already exists and cannot be used to model a new rule
            if(operationTranslations.get(OperationTypeEnum.ADD).equals(operationSelect.getValue()))
            {
                ELPRule chosenRule = updateSequence.getRuleByLabelID(e.getValue());
                if(chosenRule != null)
                {
                    ruleIDComboBox.setErrorMessage("The ID already exists");
                    ruleIDComboBox.setInvalid(true);
                }
                else
                    ruleIDComboBox.setInvalid(false);
            }
            else
            {
                // If the operation requires a existing rule (modify/delete): Check if a rule with the ID exists
                ELPRule chosenRule = aspSource.getRuleByLabelID(e.getValue());
                if(chosenRule == null)
                {
                    ruleIDComboBox.setErrorMessage("No rule was found for the entered ID");
                    ruleIDComboBox.setInvalid(true);
                }
                else
                    ruleIDComboBox.setInvalid(false);
                
                this.setFooterRowValues(chosenRule);
            }
        });

        // Listener to validate entered IDs as numebrs
        ruleIDComboBox.addCustomValueSetListener(event ->
        {
            int enteredValue = -1;
            try
            {
                enteredValue = Integer.parseInt(event.getDetail());
            } catch (NumberFormatException e) { }

            if(enteredValue > -1)
            {
                ruleIDComboBox.setValue(enteredValue);
                return;
            }

            ruleIDComboBox.setInvalid(true);
            ruleIDComboBox.setErrorMessage("ID has to be a positive number");
            ruleIDComboBox.setValue(ruleIDComboBox.getValue());
        });

        // Hack: Set random value and then change to null to set Default-Value from ChangeListener
        ruleIDComboBox.setValue(-1);
        ruleIDComboBox.setValue(null);
    }

    protected void configureOperationSelect()
    {
        operationSelect = new Select<>();
        operationSelect.setWidth("95px");

        operationSelect.setItems(operationTranslations.values());
        operationSelect.setValue(operationTranslations.get(OperationTypeEnum.ADD));
        operationSelect.setEmptySelectionAllowed(false);

        this.addOperationSelectListeners();
    }

    /** Defines the actions if a new operation is selected in the Footer-Row */
    protected void addOperationSelectListeners()
    {
        operationSelect.addValueChangeListener(e ->
        {
            // Can't use switch-Case because the array values of "operationOptions" is not constant
            // Set state for option "Add"
            if(operationTranslations.get(OperationTypeEnum.ADD).equals(e.getValue()))
            {
                conditionsMultiSelect.setEnabled(true);
                justificationsMultiSelect.setEnabled(true);
                conclusionComboBox.setEnabled(true);
                
                // Save current value because setItems() resets the selected value
                int newRuleIDValue = ruleIDComboBox.getValue();
                ruleIDComboBox.setItems(new ArrayList<>());
                ruleIDComboBox.setValue(newRuleIDValue);
            }
            // Set state for option "Modify"
            else if(operationTranslations.get(OperationTypeEnum.MODIFY).equals(e.getValue()))
            {
                conditionsMultiSelect.setEnabled(true);
                justificationsMultiSelect.setEnabled(true);
                conclusionComboBox.setEnabled(true);

                List<Integer> ruleIDs = new ArrayList<>();
                for (ELPRule currentRule : aspSource.getRuleSet())
                    ruleIDs.add(currentRule.getLabelID());

                // Save currently entered ID because it gets overwritten when the combo-box is filled with all possible IDs
                int newRuleIDValue;
                if(aspSource.getRuleByLabelID(ruleIDComboBox.getValue()) != null)
                    newRuleIDValue = ruleIDComboBox.getValue();
                else
                    newRuleIDValue = aspSource.getRuleSet().get(0).getLabelID();

                ruleIDComboBox.setItems(ruleIDs);
                ruleIDComboBox.setValue(newRuleIDValue);
            }
            // Set state for option "Delete"
            else
            {
                conditionsMultiSelect.setEnabled(false);
                justificationsMultiSelect.setEnabled(false);
                conclusionComboBox.setEnabled(false);

                List<Integer> ruleIDs = new ArrayList<>();
                for (ELPRule currentRule : aspSource.getRuleSet())
                    ruleIDs.add(currentRule.getLabelID());

                // Save currently entered ID because it gets overwritten when the combo-box is filled with all possible IDs
                int newRuleIDValue;
                if(aspSource.getRuleByLabelID(ruleIDComboBox.getValue()) != null)
                    newRuleIDValue = ruleIDComboBox.getValue();
                else
                    newRuleIDValue = aspSource.getRuleSet().get(0).getLabelID();
    
                ruleIDComboBox.setItems(ruleIDs);
                ruleIDComboBox.setValue(newRuleIDValue);
            }
        });
    }

    /** Adds a modeled rule to the grid and updates the attributes accordingly */
    protected void addGridRule()
    {
        ELPRule newRule = this.getEnteredRule();
        if(this.validateInput(newRule))
        {
            // If the Mapping contains the ruleID, the rule already exists in the aspProgram and is replaced by the new rule version
            OperationTypeEnum oldOperation = ruleOperationMapping.get(newRule.getLabelID());
            if(oldOperation != null)
                this.deleteRuleFromGrid(aspProgram.getRuleByLabelID(newRule.getLabelID()));
            
            // Iterate over all possible enum-Values until a match is found
            for (OperationTypeEnum currentKey : operationTranslations.keySet())
            {
                if(operationTranslations.get(currentKey).equals(operationSelect.getValue()))
                {
                    // Refresh ruleIDComboBox if new Rule was added and ID has to be set to the next available value
                    if(OperationTypeEnum.ADD.equals(currentKey))
                    {
                        updateSequence.addRule(newRule);
                        ruleIDComboBox.setValue(null);
                    }
                    ruleOperationMapping.put(newRule.getLabelID(), currentKey);
                    break;
                }
            }
            
            aspProgram.addRule(newRule);
            this.setItems(aspProgram.getRuleSet());
        }
    }

    /** Deletes a row/rule from the grid and updates the attributes accordingly */
    protected void deleteRuleFromGrid(ELPRule rule)
    {
        aspProgram.deleteRule(rule.getID());
        this.setItems(aspProgram.getRuleSet());
        
        if(OperationTypeEnum.ADD.equals(ruleOperationMapping.get(rule.getLabelID())))
        {
            updateSequence.deleteRule(rule.getID());
            ruleIDComboBox.setValue(null);
        }

        ruleOperationMapping.remove(rule.getLabelID());
    }

    /** Builds new rule from input of footer-row components */
    protected ELPRule getEnteredRule()
    {
        ELPRule generatedRule =  new ELPRule(
                conclusionComboBox.getValue(),
                new ArrayList<ELPLiteral>(conditionsMultiSelect.getSelectedItems()),
                new ArrayList<ELPLiteral>(justificationsMultiSelect.getSelectedItems()));
                
        int enteredID = ruleIDComboBox.getValue() == null ? -1 : ruleIDComboBox.getValue();
        generatedRule.setLabelID(enteredID);
        return generatedRule;
    }

    /** Sets the footer-row components according to the literals in the given rules (without ID) */
    public void setFooterRowValues(ELPRule chosenRule)
    {
        conditionsMultiSelect.deselectAll();
        justificationsMultiSelect.deselectAll();
        conclusionComboBox.clear();

        if(chosenRule != null)
        {
            conditionsMultiSelect.select(chosenRule.getBody());
            justificationsMultiSelect.select(chosenRule.getNegBody());

            if(!chosenRule.getHead().isEmpty())
                conclusionComboBox.setValue(chosenRule.getHead().get(0));
        }
    }

    /** Checks if the given rule is valid by trying to apply the currently selected operation in a dummy-entity */
    public boolean validateInput(ELPRule newRule)
    {
        if(ruleIDComboBox.isInvalid() || conditionsMultiSelect.isInvalid()
            || justificationsMultiSelect.isInvalid() || conclusionComboBox.isInvalid())
            return false;
        
        if(newRule.toString().contains("FAILED"))
        {
            conditionsMultiSelect.setErrorMessage("A rule must contain at least one literal");
            conditionsMultiSelect.setInvalid(true);
            justificationsMultiSelect.setInvalid(true);
            conclusionComboBox.setInvalid(true);
            return false;
        }

        if(newRule.getLabelID() < 0)
        {
            ruleIDComboBox.setErrorMessage("A rule must have a unique, non-empty ID");
            ruleIDComboBox.setInvalid(true);
            return false;
        }
        
        // Can't use switch-Case because the array values of "operationOptions" is not constant
        // Validate option "Add"
        if(operationTranslations.get(OperationTypeEnum.ADD).equals(operationSelect.getValue()))
        {
            ELPProgram testUpdateSequence = updateSequence.createNewInstance();
            if(testUpdateSequence.addRule(newRule))
            {
                conditionsMultiSelect.setInvalid(false);
                justificationsMultiSelect.setInvalid(false);
                conclusionComboBox.setInvalid(false);
                return true;
            }
            else
            {
                conditionsMultiSelect.setErrorMessage("A rule with the exact same literals already exists");
                conditionsMultiSelect.setInvalid(true);
                justificationsMultiSelect.setInvalid(true);
                conclusionComboBox.setInvalid(true);
                return false;
            }
        }
        // Validate option "Modify"
        else if(operationTranslations.get(OperationTypeEnum.MODIFY).equals(operationSelect.getValue()))
        {
            ELPProgram testSource = aspSource.createNewInstance();
            ELPRule oldRule = testSource.getRuleByLabelID(ruleIDComboBox.getValue());
            if(newRule.equals(oldRule))
            {
                conditionsMultiSelect.setErrorMessage("A rule-modification has to contain at least on changed literal");
                conditionsMultiSelect.setInvalid(true);
                justificationsMultiSelect.setInvalid(true);
                conclusionComboBox.setInvalid(true);
                return false;
            }

            newRule.setID(oldRule.getID());
            if(testSource.modifyRule(newRule))
            {
                conditionsMultiSelect.setInvalid(false);
                justificationsMultiSelect.setInvalid(false);
                conclusionComboBox.setInvalid(false);
                return true;
            }
            else
            {
                conditionsMultiSelect.setErrorMessage("The rule-modification is invalid");
                conditionsMultiSelect.setInvalid(true);
                justificationsMultiSelect.setInvalid(true);
                conclusionComboBox.setInvalid(true);
                return false;
            }
        }
        // Validate option "Delete"
        else
        {
            ELPProgram testSource = aspSource.createNewInstance();
            ELPRule oldRule = testSource.getRuleByLabelID(ruleIDComboBox.getValue());
            newRule.setID(oldRule.getID());

            if(testSource.deleteRule(oldRule.getID()))
            {
                conditionsMultiSelect.setInvalid(false);
                justificationsMultiSelect.setInvalid(false);
                conclusionComboBox.setInvalid(false);
                return true;
            }
            else
            {
                conditionsMultiSelect.setErrorMessage("The rule-deletion is invalid!");
                conditionsMultiSelect.setInvalid(true);
                justificationsMultiSelect.setInvalid(true);
                conclusionComboBox.setInvalid(true);
                return false;
            }
        }
    }

    /** Has to be called from the View, in which the component is used, as the parsing of new literals requires a Framework-Adapter */
    public void configureCustomValueListener(ASPUAFrameworkAdapter adapter)
    {
        // Parse new literal entered in conditionsMultiSelect
        conditionsMultiSelect.addCustomValuesSetListener(e ->
        {
            ASPLiteral<?> untypedLiteral = adapter.parseNewLiteral(e.getDetail());
            if(untypedLiteral != null)
            {
                conditionsMultiSelect.setInvalid(false);

                ELPLiteral newLiteral = (ELPLiteral) untypedLiteral;
                availableLiterals.add(newLiteral);
                conditionsMultiSelect.updateSelection(new HashSet<ELPLiteral>(Arrays.asList(newLiteral)) , new HashSet<>());

                justificationsMultiSelect.getDataProvider().refreshAll();
                conclusionComboBox.getDataProvider().refreshAll();
            }
            else
            {
                conditionsMultiSelect.setErrorMessage("Invalid Syntax! New literals require the format 'predicate(term1,term2,...)' or simply 'predicate'");
                conditionsMultiSelect.setInvalid(true);
            }
        });

        // Parse new literal entered in justificationsMultiSelect
        justificationsMultiSelect.addCustomValuesSetListener(e ->
        {
            ASPLiteral<?> untypedLiteral = adapter.parseNewLiteral(e.getDetail());
            if(untypedLiteral != null)
            {
                justificationsMultiSelect.setInvalid(false);

                ELPLiteral newLiteral = (ELPLiteral) untypedLiteral;
                availableLiterals.add(newLiteral);
                justificationsMultiSelect.updateSelection(new HashSet<ELPLiteral>(Arrays.asList(newLiteral)) , new HashSet<>());

                conditionsMultiSelect.getDataProvider().refreshAll();
                conclusionComboBox.getDataProvider().refreshAll();
            }
            else
            {
                justificationsMultiSelect.setErrorMessage("Invalid Syntax! New literals require the format 'predicate(term1,term2,...)' or simply 'predicate'");
                justificationsMultiSelect.setInvalid(true);
            }
        });

        // Parse new literal entered in conclusionComboBox
        conclusionComboBox.addCustomValueSetListener(e ->
        {
            ASPLiteral<?> untypedLiteral = adapter.parseNewLiteral(e.getDetail());
            if(untypedLiteral != null)
            {
                conclusionComboBox.setInvalid(false);
                ELPLiteral typedLiteral = (ELPLiteral) untypedLiteral;
                
                // The Listener may be fired twice. The condition avoids that the new Literal is added twice.
                if(!availableLiterals.contains(typedLiteral))
                    availableLiterals.add(typedLiteral);

                conclusionComboBox.setValue(typedLiteral);

                conditionsMultiSelect.getDataProvider().refreshAll();
                justificationsMultiSelect.getDataProvider().refreshAll();
            }
            else
            {
                conclusionComboBox.setErrorMessage("Invalid Syntax! New literals require the format 'predicate(term1,term2,...)' or simply 'predicate'");
                conclusionComboBox.setInvalid(true);
            }
        });
    }

    /**
     * Returns the GridContextMenu-component which has to be added to the caller-component to be displayed.
     * Enables the functionality to delete rows via rightclick -> 'remove'
     */
    public GridContextMenu<ELPRule> enableGridContextMenu(boolean enabled)
    {
        if(contextMenu == null)
            this.createGridContextMenu();

        if(enabled & contextMenu.getTarget() != this)
            contextMenu.setTarget(this);
        else if(!enabled & contextMenu.getTarget() != null)
            contextMenu.setTarget(null);

        return contextMenu;
    }

    private void createGridContextMenu()
    {
        contextMenu = new GridContextMenu<>();
        contextMenu.addItem(getTranslation("button.remove"), e ->
        {
            e.getItem().ifPresent(rule -> this.deleteRuleFromGrid(rule));
        });
    }

    /** 
     * Registers service which is used to automatically synchronise the grid rows with the current update sequence of the framework 
     * and assign the corresponding operations.
     * @see #refreshGridContent
     */
    public void registerValueProvider(ASPUAFrameworkAdapter valueServieProvider) {
        this.valueServieProvider = valueServieProvider;
    }

    /**
     * Synchronisse the grid rows with the current update sequence of the framework.
     * @param showOperationChanges Determines if rule operations should be displayed. If true, deleted rows are also displayed with the corresponding operation
     */
    public void refreshGridContent(boolean showOperationChanges)
    {
        if(valueServieProvider == null)
        {
            LOGGER.warn("The grid couldn't be refreshed because the value-Provider was null. Set the value-Provider by invoking the 'registerValueProvider()'-method.");
            return;
        }

        if(!showOperationChanges)
        {
            ruleOperationMapping = new HashMap<>();
            this.setASPProgram((ELPProgram) valueServieProvider.getMergedUpdateSequence());
            this.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            return;
        }

        // Add all operations which were made due to conflict resolution
        ruleOperationMapping = valueServieProvider.getAppliedSolutionOperations();

        // All rules of the second program in the update sequence are considered as added rules
        ELPProgram updateSequence = (ELPProgram) valueServieProvider.getInitialProgram().createNewInstance();
        for (ELPRule currentRule : ((ELPProgram) valueServieProvider.getNewProgram()).getRuleSet())
        {
            updateSequence.addRule(currentRule);
            ruleOperationMapping.put(currentRule.getLabelID(), OperationTypeEnum.ADD);
        }

        // Deleted rules will still be displayed, but with the operation-annotation 'Delete'
        ELPProgram unmodifedUpdateSequence = (ELPProgram) valueServieProvider.getUnmodifiedUpdateSequence();
        for (int ruleID : ruleOperationMapping.keySet())
        {
            if(OperationTypeEnum.DELETE.equals(ruleOperationMapping.get(ruleID)))
            {
                ASPRule<?> deletedRule = unmodifedUpdateSequence.getRuleByLabelID(ruleID);
                // Add rules to list, as the deleted rule should still be displayed
                updateSequence.addRule(deletedRule);
                // Manually insert the rule to its original position in the ruleSet
                updateSequence.getRuleSet().remove(deletedRule);
                updateSequence.getRuleSet().add(unmodifedUpdateSequence.getRuleSet().indexOf(deletedRule), (ELPRule) deletedRule);
            }
        }

        this.setASPProgram(updateSequence);
    }

    /** 
     * Returns all rows displayed in the grid which are assigned to the given operation.
     */
    public List<ELPRule> getGridRules(OperationTypeEnum operationType)
    {
        if(operationType == null)
            return aspProgram.getRuleSet();
        
        List<ELPRule> filteredRules = new ArrayList<>();

        for (ELPRule currentRule : aspProgram.getRuleSet())
        {
            OperationTypeEnum ruleOperation = ruleOperationMapping.get(currentRule.getLabelID());

            if(operationType.equals(ruleOperation))
                filteredRules.add(currentRule);
        }

        return filteredRules;
    }

    /** Determines the visibility of the footer-row components. */
    public void enableFooterRowComponents(boolean enabled)
    {
        ruleIDComboBoxWrapper.setVisible(enabled);
        conditionsMultiSelectWrapper.setVisible(enabled);
        justificationsMultiSelectWrapper.setVisible(enabled);
        conclusionComboBoxWrapper.setVisible(enabled);

        if(operationLayoutWrapper != null)
            operationLayoutWrapper.setVisible(enabled);
    }

    /** Conflicting rules are marked with the label 'Conflicting' in the operation-column */
    public void setConflictingRules(List<ELPRule> conflictingRules) {
        if(conflictingRules == null)
            this.conflictingRules = new ArrayList<>();
        else
            this.conflictingRules = conflictingRules;

        this.getDataProvider().refreshAll();
    }

    public ComboBox<Integer> getRuleIDComboBox() {
        return ruleIDComboBox;
    }

    public void setRuleIDComboBox(ComboBox<Integer> ruleIDComboBox) {
        this.ruleIDComboBox = ruleIDComboBox;
    }

    public Select<String> getOperationSelect() {
        return operationSelect;
    }

    public void setOperationSelect(Select<String> operationSelect) {
        this.operationSelect = operationSelect;
    }

    public Map<Integer, OperationTypeEnum> getRuleOperationMapping() {
        return ruleOperationMapping;
    }

    public void setRuleOperationMapping(Map<Integer, OperationTypeEnum> ruleOperationMapping) {
        if(ruleOperationMapping == null)
            this.ruleOperationMapping = new HashMap<>();
        else
            this.ruleOperationMapping = ruleOperationMapping;
    }

    public Button getApplyButton() {
        return applyButton;
    }

    public void setApplyButton(Button applyButton) {
        this.applyButton = applyButton;
    }

    public Map<OperationTypeEnum, String> getOperationTranslations() {
        return operationTranslations;
    }
}
