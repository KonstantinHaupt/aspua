package de.aspua.gui.UI.CustomComponents.Grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;

/**
 * Custom component to show ASP-Programs (ELPs) in a grid. This component can be extended by sub-components with further extensions & features.
 * @see EditableGridComponent
 * @see SolutionGridComponent
 */
public class BaseGridComponent extends TreeGrid<ELPRule>
{
    /** Components */
    protected HeaderRow filterHeaderRow;
    protected TextField filterTextField;
    protected TreeGrid.Column<ELPRule> ruleIDColumn;
    protected TreeGrid.Column<ELPRule> conditionsColumn;
    protected TreeGrid.Column<ELPRule> justificationsColumn;
    protected TreeGrid.Column<ELPRule> conclusionColumn;

    /** Stored data */
    protected ELPProgram aspProgram;

    public BaseGridComponent(ASPProgram<?, ?> aspProgram)
    {
        if(aspProgram == null)
            this.aspProgram = new ELPProgram();
        else
            this.aspProgram = (ELPProgram) aspProgram;

        this.configureProperties();
        this.addGridContent();
        
        this.setItems(this.aspProgram.getRuleSet());
    }

    private void configureProperties()
    {
        this.setSizeFull();
        this.setMultiSort(true);
        // this.addThemeVariants(GridVariant.LUMO_COMPACT);
        this.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        this.addThemeVariants(GridVariant.MATERIAL_COLUMN_DIVIDERS);
    }

    protected void addGridContent()
    {
        // Column for showing the ID of an ELP-rule (or ASP-rule in general)
        ruleIDColumn = this.addHierarchyColumn(ELPRule::getLabelID)
            .setHeader(getTranslation("label.ASP.ruleID"))
            .setKey("ID")
            .setSortable(true)
            .setFlexGrow(1)
            .setResizable(true);

        // Column for showing the positive body of an ELP-rule
        conditionsColumn = this.addColumn(elpRule ->
            { 
                String literalString = "";
                if(elpRule.getBody() != null && !elpRule.getBody().isEmpty())
                {
                    literalString = elpRule.getBody().toString();
                    literalString = literalString.substring(1, literalString.length() - 1);
                }
                return literalString;
            }, "literalString")
            .setHeader(getTranslation("label.ASP.condition"))
            .setKey("condition")
            .setSortable(true)
            .setFlexGrow(6)
            .setResizable(true);

        // Column for showing the negative body of an ELP-rule
        justificationsColumn = this.addColumn(elpRule ->
            { 
                String literalString = "";
                if(elpRule.getNegBody() != null && !elpRule.getNegBody().isEmpty())
                {
                    literalString = elpRule.getNegBody().toString();
                    literalString = literalString.substring(1, literalString.length() - 1);
                }
                return literalString;
            })
            .setHeader(getTranslation("label.ASP.justification"))
            .setKey("justification")
            .setSortable(true)
            .setFlexGrow(6)
            .setResizable(true);

        // Column for showing the head of an ELP-rule
        conclusionColumn = this.addColumn(elpRule ->
            { 
                String literalString = "";
                if(elpRule.getHead() != null && !elpRule.getHead().isEmpty())
                {
                    literalString = elpRule.getHead().toString();
                    literalString = literalString.substring(1, literalString.length() - 1);
                }
                return literalString;
            })
            .setHeader(getTranslation("label.ASP.conclusion"))
            .setKey("conclusion")
            .setSortable(true)
            .setFlexGrow(4)
            .setResizable(true);
    }

    /**
     * Method for adding a filter textfield above the ID-Column.
     * Has to be explictly invoked because sub-components may not support filtering (complex for hierarchical rules)
     */
    public HeaderRow addFilterHeaderRow()
    {
        filterHeaderRow = this.prependHeaderRow();

        filterTextField = new TextField();
        filterTextField.setPlaceholder(getTranslation("placeholder.grid.filter"));
        filterTextField.setClearButtonVisible(true);
        filterTextField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        filterTextField.addValueChangeListener(e -> this.applyFilter(e.getValue().trim()));
        filterHeaderRow.join(ruleIDColumn, conditionsColumn).setComponent(filterTextField);

        return filterHeaderRow;
    }

    protected void applyFilter(String filterString)
    {
        List<ELPRule> filteredRules = new ArrayList<>();

        if(filterString == null || "".equals(filterString))
        {
            filteredRules = aspProgram.getRuleSet();
        }
        else
        {
            // Support multiple search terms by separating them with spaces
            List<String> searchTerms = Arrays.asList(filterString.split("\\s"));
            for (String currentSearchTerm : searchTerms)
            {
                currentSearchTerm = currentSearchTerm.trim();
                if("".equals(currentSearchTerm))
                    searchTerms.remove(currentSearchTerm);
            }

            for (ELPRule rule : aspProgram.getRuleSet())
            {
                for (String currentSearchTerm : searchTerms)
                {
                    // Filter for IDs of rules
                    if(Integer.toString(rule.getLabelID()).equals(currentSearchTerm))
                    {
                        filteredRules.add(rule);
                        break;
                    }

                    //Filter for literals in rules
                    boolean ruleContained = rule.getAllLiterals().stream()
                                            .anyMatch(x -> currentSearchTerm.contains(x.toString())
                                                    || x.toString().contains(currentSearchTerm));
                    
                    if(ruleContained)
                    {
                        filteredRules.add(rule);
                        break;
                    }
                }
            }
        }
        this.setItems(filteredRules);
    }

    public void setASPProgram(ASPProgram<?, ?> aspProgram) {
        if(aspProgram == null)
            this.aspProgram = new ELPProgram();
        else
            this.aspProgram = (ELPProgram) aspProgram;

        this.setItems(this.aspProgram.getRuleSet());
    }

    public TextField getFilterTextField() {
        return filterTextField;
    }

    public void setFilterTextField(TextField filterTextField) {
        this.filterTextField = filterTextField;
    }
}
