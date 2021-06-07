package de.aspua.gui.UI.CustomComponents.Accordion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipAlignment;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.SolutionMetaDataEnum;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.CustomComponents.Dialog.SolutionPreviewDialog;
import de.aspua.gui.UI.CustomComponents.Dialog.ValidationDialog;
import de.aspua.gui.UI.CustomComponents.Grid.SolutionGridComponent;

/**
 * Custom component for displaying solutions and provide functionalities for customizing/applying them.
 * Provides a compact version for only displaying the affected answer sets of conflicts.
 */
public class ConflictPanelComponent extends AccordionPanel
{
    private ASPUAFrameworkAdapter aspuaAdapterService;

    /** Components */
    // Overall layout
    private HorizontalLayout panelLayout;
    // Contains the left sidebar with answer sets & solutions
    private VerticalLayout controlLayout;
    private VerticalLayout conflictInfoLayout;
    private VerticalLayout solutionListLayout;

    // Wraps the solutionGrid on the right side of the panel
    private VerticalLayout gridLayout;
    // Contains the row below the solutionGrid to align all buttons
    private HorizontalLayout buttonLayout;

    private SolutionGridComponent solutionGrid;
    private MultiSelectListBox<Solution> solutionListBox;
    private Button customizeButton;
    private Button previewButton;
    private Button solveButton;
    
    /** Stored data */
    private Conflict conflict;
    // Individual object for a new, customized solution which wasn't provided by the service/frameworkAPI
    private Solution customSolution;
    // Indicates whether the solutions should be displayed (if so, false). Otherwise, only the answer sets are displayed
    private boolean compact;
    // Indicates whether the CONTROL or ALT key is clicked to enable the selection of multiple solutions (see configureListBoxListener())
    private boolean keyIsPressed;
    // Maps the enum-values to actuals labels (from current locale).
    protected Map<SolutionMetaDataEnum, String> measureTranslations;

    public ConflictPanelComponent(ASPUAFrameworkAdapter aspuaAdapterService, Conflict conflict, boolean compact)
    {
        if(conflict != null)
            this.conflict = conflict.createNewInstance();
        else
            this.conflict = new Conflict(null, null);
            
        this.aspuaAdapterService = aspuaAdapterService;
        this.customSolution = new Solution(this.conflict, null, null, null);
        this.conflict.getSolutions().add(customSolution);
        this.compact = compact;
        keyIsPressed = false;

        measureTranslations = new LinkedHashMap<>();
        measureTranslations.put(SolutionMetaDataEnum.MEASURE_ANSWERSETCHANGES, getTranslation("label.measure.answerSetChanges"));
        measureTranslations.put(SolutionMetaDataEnum.MEASURE_RULECHANGES, getTranslation("label.measure.ruleChanges"));

        ELPRule oldConflictRule = (ELPRule) conflict.getConflictingRules().get(0);
        ELPRule newConflictRule = (ELPRule) conflict.getConflictingRules().get(1);
        String summaryText = String.format("Conflict between Rule %s & %s (Literal: %s)",
            oldConflictRule.getLabelID(), newConflictRule.getLabelID(), oldConflictRule.getHead().get(0).getAtom());
        this.setSummary(new H5(summaryText));

        this.configureProperties();
        this.configureControlLayout();
        this.configurePanelLayout();
    }

    private void configureProperties()
    {
        this.addThemeVariants(DetailsVariant.FILLED);
        this.addThemeVariants(DetailsVariant.REVERSE);
    }

    private void configureControlLayout()
    {
        // Header and answer sets are displayed in compact and extended version
        H5 header = new H5(getTranslation("label.ConflictView.AnswerSetHeader"));
        header.getStyle().set("margin-bottom", "0px");
        header.getStyle().set("margin-left", "0px");

        conflictInfoLayout = new VerticalLayout(header);
        this.createAnswerSetOutput();
        
        controlLayout = new VerticalLayout(conflictInfoLayout);

        if(!compact)
        {
            // Adjust sizes to fit more components
            controlLayout.setWidth("25%");
            controlLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
            conflictInfoLayout.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
            conflictInfoLayout.setHeight("50%");

            customizeButton = new Button(getTranslation("button.conflict.customize"));
            previewButton = new Button(getTranslation("button.conflict.preview"));
            solveButton = new Button("Solve");
            solveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            this.configureButtonListener();
    
            Div filler = new Div();
            buttonLayout = new HorizontalLayout(customizeButton, filler, previewButton, solveButton);
            buttonLayout.expand(filler);
            buttonLayout.setWidthFull();
            buttonLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        }
    }

    private void configureButtonListener()
    {
        customizeButton.addClickListener(e -> 
        {
            boolean emptyCustom = customSolution.getAddedRules().isEmpty()
                && customSolution.getModifiedRules().isEmpty()
                && customSolution.getDeletedRules().isEmpty();

            if(!solutionListBox.getSelectedItems().contains(customSolution) && !emptyCustom)
                this.openSolutionOverwriteDialog();
            else
                this.fillCustomSolution();
        });

        previewButton.addClickListener(e ->
        {
            // If custom solution was selected, the measures have to be computed
            if(customSolution == solutionGrid.getSolution())
                aspuaAdapterService.computeMeasures(customSolution);

            List<Conflict> newConflicts = aspuaAdapterService.previewSolutionConflicts(solutionGrid.getSolution());
            // If custom solution was selected, the measures were already computed in the last line
            List<AnswerSet<?, ?>> newAnswerSets = aspuaAdapterService.previewSolutionUpdateAnswerSets(solutionGrid.getSolution());
            SolutionPreviewDialog previewDialog = new SolutionPreviewDialog(solutionGrid.getSolution(), newConflicts, aspuaAdapterService.getCurrentUpdateAnswerSets(), newAnswerSets);
            previewDialog.open();
        });

        solveButton.addClickListener(e ->
        {
            // Validate that the update sequence after applying the solution is consistent
            List<AnswerSet<?, ?>> newAnswerSets = aspuaAdapterService.previewSolutionUpdateAnswerSets(solutionGrid.getSolution());
            if(newAnswerSets == null || newAnswerSets.isEmpty())
            {
                this.createIncompleteSolutionDialog(true);
                return;
            }

            // Validate that the conflict is actually solved by the chosen solution
            boolean solved = true;
            List<Conflict> remainingConflicts = aspuaAdapterService.previewSolutionConflicts(solutionGrid.getSolution());
            for (Conflict currentConflict : remainingConflicts)
            {
                if(currentConflict.getConflictingRules().containsAll(conflict.getConflictingRules()))
                {
                    solved = false;
                    break;
                }
            }

            if(solved)
            {
                aspuaAdapterService.solveConflict(solutionGrid.getSolution());
                ((ConflictAccordionComponent) this.getParent().get()).handleAppliedSolution();
            }
            else
                this.createIncompleteSolutionDialog(false);
        });
    }

    private void openSolutionOverwriteDialog()
    {
        ValidationDialog dialog = new ValidationDialog(null);
        dialog.setHeaderText("Overwrite custom solution");
        dialog.setMessageText("The custom solution will be overwritten with your selected solution(s). Are you sure you want to continue?");
        dialog.getNextButton().setText(getTranslation("button.overwrite"));
        dialog.getNextButton().removeThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.setSaveButtonClickListener(e -> 
        {
            this.fillCustomSolution();
            dialog.close();
        });
    }

    /** May outsource to framework */
    private void fillCustomSolution()
    {
        // Save current solution if it is one of the selected solutions
        Set<Solution> selectedSolutions = solutionListBox.getSelectedItems();
        Solution cachedCustom = null;
        if(selectedSolutions.contains(customSolution))
            cachedCustom = customSolution.createNewInstance(conflict);

        // Reset custom solution
        customSolution.setAddedRules(null);
        customSolution.setModifiedRules(null);
        customSolution.setDeletedRules(null);
        customSolution.setAddVariants(null);
        customSolution.setModifyVariants(null);
        customSolution.setDeleteVariants(null);
        
        for (Solution solution : selectedSolutions)
        {
            Solution clonedSolution;
            if(solution.equals(customSolution))
                clonedSolution = cachedCustom;
            else
                clonedSolution = solution.createNewInstance(this.conflict);

            if(clonedSolution == null)
                continue;

            // Merge all operations of current solution to custom solution
            // Add chosen 'Add' rules
            for (ASPRule<?> rule : clonedSolution.getAddedRules())
            {
                if(!this.isCustomDuplicate(rule))
                    customSolution.getAddedRules().add(rule);
            }

            // Add chosen 'Modify' rules
            for (ASPRule<?> rule : clonedSolution.getModifiedRules())
            {
                if(!this.isCustomDuplicate(rule))
                    customSolution.getModifiedRules().add(rule);
            }

            // Iterate over all addVariant-lists of the clonedSolution
            for (List<ASPRule<?>> variants : clonedSolution.getAddVariants().values())
            {
                // Iterate over each rule within each variant-list
                for (ASPRule<?> rule : variants)
                {
                    if(!this.isCustomDuplicate(rule))
                    {
                        // If the rule doesnt't exist, but there are already other variants: add to variants
                        if(customSolution.getAddVariants().get(rule.getID()) != null)
                            customSolution.getAddVariants().get(rule.getID()).add(rule);
                        else
                        {
                            // Create a new list for variants if it doesn't exist
                            List<ASPRule<?>> newVariant = new ArrayList<>();
                            customSolution.getAddVariants().put(rule.getID(), newVariant);
                        }
                    }
                }
            }

            // Iterate over all modifyVariant-lists of the clonedSolution
            for (List<ASPRule<?>> variants : clonedSolution.getModifyVariants().values())
            {
                // Iterate over each rule within each variant-list
                for (ASPRule<?> rule : variants)
                {
                    if(!this.isCustomDuplicate(rule))
                    {
                        // If the rule doesnt't exist, but there are already other variants: add to variants
                        if(customSolution.getModifyVariants().get(rule.getID()) != null)
                            customSolution.getModifyVariants().get(rule.getID()).add(rule);
                        else
                        {
                            // Create a new list for variants if it doesn't exist
                            List<ASPRule<?>> newVariant = new ArrayList<>();
                            newVariant.add(rule);
                            customSolution.getModifyVariants().put(rule.getID(), newVariant);
                        }
                    }
                }
            }

            customSolution.getDeletedRules().addAll(clonedSolution.getDeletedRules());
            customSolution.getDeleteVariants().putAll(clonedSolution.getDeleteVariants());
        }

        solutionListBox.select(customSolution);
    }

    private boolean isCustomDuplicate(ASPRule<?> rule)
    {
        // Check if current rule exists in chosen rules
        if(customSolution.getAddedRules().contains(rule)
        || customSolution.getModifiedRules().contains(rule))
            return true;

        // Check if current rule exists in add variants
        boolean alreadyExists = customSolution.getAddVariants()
            .values()
            .stream()
            .anyMatch(list -> list.contains(rule));

        if(alreadyExists)
            return true;

        // Check if current rule exists in modify variants
        return customSolution.getModifyVariants()
            .values()
            .stream()
            .anyMatch(list -> list.contains(rule));
    }

    private void createIncompleteSolutionDialog(boolean inconsistent)
    {
        ValidationDialog dialog = new ValidationDialog(null);
        dialog.setModal(true);
        dialog.setDraggable(false);

        if(inconsistent)
            dialog.setHeaderText("Inconsistent knowledge base");
        else
            dialog.setHeaderText("Ineffective Solution");

        ASPRule<?> oldRule = conflict.getConflictingRules().get(0);
        ASPRule<?> newRule = conflict.getConflictingRules().get(1);
        StringBuilder sb = new StringBuilder();

        if(inconsistent)
        {
            sb.append("Caution! If the selected solution is applied, the knowledge base becomes inconsistent, i.e. doesn't contain any results.");
            sb.append(" It is recommended that a knowledge base stays consistent at all times, especially because it is not possible to detect conflicts in inconsistent knowledge bases.");
        }
        else
        {
            sb.append(String.format("Caution! The conflict between rule %s and %s won't be solved by the selected solution!",
                oldRule.getLabelID(), newRule.getLabelID()));
            sb.append(" It is highly recommended to only apply solutions which will actually solve the conflict.");
        }
        dialog.setMessageText(sb.toString());

        dialog.getCancelButton().addClickListener(e -> dialog.close());
        dialog.getNextButton().setText(getTranslation("button.apply"));
        dialog.getNextButton().removeThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.setSaveButtonClickListener(e -> 
        {
            aspuaAdapterService.solveConflict(solutionGrid.getSolution());
            ((ConflictAccordionComponent) this.getParent().get()).handleAppliedSolution();
            dialog.close();
        });
        dialog.open();
    }
    
    private void configurePanelLayout()
    {
        // Configure overall layout
        panelLayout = new HorizontalLayout();
        panelLayout.setWidthFull();
        panelLayout.setSpacing(false);
        panelLayout.setDefaultVerticalComponentAlignment(Alignment.START);
        panelLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // The compact version doesn't contain the grid etc.
        if(compact)
        {
            panelLayout.add(controlLayout);
            this.setContent(panelLayout);
        }
        else
        {
            panelLayout.setHeight("500px");

            gridLayout = new VerticalLayout();
            gridLayout.setSizeFull();
            gridLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    
            // Configure solution grid
            solutionGrid = new SolutionGridComponent(conflict.getSolutions().get(0), aspuaAdapterService.getUnmodifiedUpdateSequence());
            solutionGrid.configureCustomValueListener(aspuaAdapterService);
            solutionGrid.addItemDoubleClickListener(e ->
            {
                if(solutionGrid.getSolution() != customSolution)
                {
                    aspuaAdapterService.computeMeasures(solutionGrid.getSolution());
                    solutionListBox.getDataProvider().refreshAll();
                }
            });
            
            // Add grid to layout
            gridLayout.add(solutionGrid, buttonLayout);
            gridLayout.expand(solutionGrid);
            solutionGrid.setHeight(gridLayout.getHeight());
            panelLayout.add(solutionGrid.enableGridContextMenu(false));
    
            solutionListBox = new MultiSelectListBox<>();
            // Initial sorting is answerset-changes
            solutionListBox.setItems(this.sortSolutions(SolutionMetaDataEnum.MEASURE_ANSWERSETCHANGES));
            solutionListBox.setRenderer(new ComponentRenderer<HorizontalLayout ,Solution>(solution -> this.createListBoxRenderer(solution)));
            this.configureListBoxListener();
            
            // Initial configuration if panel is opened
            solutionListBox.select(new HashSet<>(Arrays.asList(conflict.getSolutions().get(0))));
            solutionListBox.setValue(new HashSet<>(Arrays.asList(conflict.getSolutions().get(0))));
            solutionGrid.enableFooterRowComponents(false);

            H5 header = new H5(getTranslation("label.ConflictView.SolutionHeader"));
            header.getStyle().set("margin-bottom", "0px");
            header.getStyle().set("margin-left", "0px");

            // Configure select-component for measures
            Select<SolutionMetaDataEnum> solutionSortSelect = new Select<>();
            solutionSortSelect.setWidth("160px");
            solutionSortSelect.getElement().setAttribute("theme", "small");
            solutionSortSelect.getElement().getStyle().set("margin", "0px");
            solutionSortSelect.setLabel(getTranslation("label.solution.sort"));
            solutionSortSelect.setItemLabelGenerator(measure -> measureTranslations.get(measure));
            
            solutionSortSelect.setItems(measureTranslations.keySet());
            solutionSortSelect.setValue(SolutionMetaDataEnum.MEASURE_ANSWERSETCHANGES);
            solutionSortSelect.addValueChangeListener(e -> this.sortSolutionListBoxItems(e.getValue()));

            // Wrap in scroller-component to prevent overflow if many solutions exist
            Scroller scrollLayout = new Scroller(solutionListBox);
            scrollLayout.setWidthFull();
            scrollLayout.setHeight("260px");

            solutionListLayout = new VerticalLayout(header, solutionSortSelect, scrollLayout);
            solutionListLayout.setJustifyContentMode(JustifyContentMode.START);
            solutionListLayout.getStyle().set("background-color", "var(--lumo-contrast-10pct)");

            controlLayout.add(solutionListLayout);
            panelLayout.add(controlLayout, gridLayout);
            this.setContent(panelLayout);
        }
    }

    /** Formats the visual appearance of a solution in the listbox */
    private HorizontalLayout createListBoxRenderer(Solution solution)
    {
        Div name = new Div();
        name.getStyle().set("font-weight", "bold");
        Div description = new Div();

        if(solution.equals(customSolution))
            name.setText("Custom Solution");
        else
        {
            // Add +1 to Index of solution to start counting at 1, which is more intuitive for a user
            name.setText("Solution " + (conflict.getSolutions().indexOf(solution) + 1));

            StringBuilder sb = new StringBuilder();
            ASPLiteral<?> targetLiteral = (ASPLiteral<?>) solution.getMetaData().get(SolutionMetaDataEnum.TARGETLITERAL);
            String rootRuleID = (String) solution.getMetaData().get(SolutionMetaDataEnum.ROOTRULE);

            // Find the label-ID of the rule, as the metadata only contains the (internal) ID which is used for computations
            int labelID = -1;
            if(rootRuleID != null)
            {
                for (ASPRule<?> conflictRule : conflict.getConflictingRules())
                {
                    if(rootRuleID.equals(conflictRule.getID()))
                    {
                        labelID = conflictRule.getLabelID();
                        break;
                    }
                }
            }

            if(targetLiteral != null)
            {
                sb.append("Focus Literal '" + targetLiteral.toString());
                if(rootRuleID != null)
                {
                    sb.append("' of Rule " + labelID);
                }
            }
            else if(rootRuleID != null)
            {
                sb.append("Focus Rule " + labelID);
            }
            
            description.setText(sb.toString());
        }

        VerticalLayout textLayout = new VerticalLayout(name, description);
        textLayout.setWidth("80%");
        textLayout.setMaxWidth("200px");
        textLayout.setPadding(false);
        textLayout.setMargin(false);
        textLayout.setSpacing(false);
        
        HorizontalLayout boxLayout = new HorizontalLayout(textLayout);
        boxLayout.setWidth(solutionListBox.getWidth());
        boxLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        boxLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        boxLayout.setSpacing(false);
        // Measures are not computed for the customSolution and would be absent anyways
        if(solution != customSolution)
        {
            Icon infoIcon = new Icon(VaadinIcon.INFO_CIRCLE_O);
            infoIcon.setColor("var(--lumo-primary-text-color)");
            infoIcon.setSize("18px");

            Tooltip tooltip = this.createTooltip(solution);
            tooltip.attachToComponent(infoIcon);
            boxLayout.add(infoIcon, tooltip);
        }

        return boxLayout;
    }

    private void configureListBoxListener()
    {
        // Register JavaScript-listener for CONTROL- and ALT-keys (ALT as backup if CONTROL isn't available because it is used otherwise etc.)
        solutionListBox.getElement().addEventListener("keydown", e -> 
        {
            keyIsPressed =   e.getEventData().get("event.keyCode === 17").asBoolean()
                        ||  e.getEventData().get("event.keyCode === 18").asBoolean();
        })
        .addEventData("event.keyCode === 17")
        .addEventData("event.keyCode === 18");
        solutionListBox.getElement().addEventListener("keyup", e -> keyIsPressed = false);

        solutionListBox.addSelectionListener(e -> 
        {
            // Ensure that some solution is selected at all time
            if(e.getAllSelectedItems().isEmpty())
            {
                solutionListBox.select(e.getOldSelection().stream().findFirst().get());
                return;
            }

            Solution selectedSolution = e.getRemovedSelection().stream()
            .findFirst()
            .orElse(null);
            
            // Deselecting a solution if other solutions are still selected (with ctrl/alt) has no other effect than the deselect
            if(selectedSolution != null & keyIsPressed)
                return;

            // If a selected solution is deselected without pressing ctrl/alt, the clicked solution gets selected as the only one
            if(selectedSolution != null & !keyIsPressed)
            {
                solutionListBox.updateSelection(new HashSet<>(Arrays.asList(selectedSolution)), e.getOldSelection());
                return;
            }

            // If this code is executed, it is clear that a solution has been selected
            selectedSolution = e.getAddedSelection().stream()
            .findFirst()
            .orElse(null);

            if(!keyIsPressed)
                solutionListBox.deselect(e.getOldSelection());

            solutionGrid.displaySolution(selectedSolution);
        });

        solutionListBox.addSelectionListener(e -> 
        {
            if(e.getAllSelectedItems().size() > 1)
            {
                solveButton.setEnabled(false);
                previewButton.setEnabled(false);
                customizeButton.setEnabled(true);
                solutionGrid.enableFooterRowComponents(false);
                solutionGrid.enableGridContextMenu(false);
            }
            else
            {
                solveButton.setEnabled(true);
                previewButton.setEnabled(true);

                // Is true if the customSolution is the only selected item (as this is executed in an else-block)
                if(e.getAllSelectedItems().contains(customSolution))
                {
                    solutionGrid.enableFooterRowComponents(true);
                    solutionGrid.enableGridContextMenu(true);
                    customizeButton.setEnabled(false);
                }
                else
                {
                    solutionGrid.enableFooterRowComponents(false);
                    solutionGrid.enableGridContextMenu(false);
                    customizeButton.setEnabled(true);
                }
            }
        });
    }

    /** Creates a tooltip to show the measures for solutions in the listbox */
    private Tooltip createTooltip(Solution solution)
    {
        Tooltip tooltip = new Tooltip();
        tooltip.setPosition(TooltipPosition.RIGHT);
        tooltip.setAlignment(TooltipAlignment.CENTER);
        tooltip.addThemeName("light");

        // Use list instead of measureTranslations to control the order of displayed measures
        List<SolutionMetaDataEnum> solutionOptions = new ArrayList<>();
        solutionOptions.add(SolutionMetaDataEnum.MEASURE_ANSWERSETCHANGES);
        solutionOptions.add(SolutionMetaDataEnum.MEASURE_RULECHANGES);
        for (SolutionMetaDataEnum solutionMeasure : solutionOptions)
        {
            Paragraph measureText = new Paragraph(measureTranslations.get(solutionMeasure) + ": ");
            Paragraph valueText = new Paragraph("-");
            if(solution.getMetaData().containsKey(solutionMeasure))
            {
                int measureValue = (int) solution.getMetaData().get(solutionMeasure);
                valueText = new Paragraph(Integer.toString(measureValue));
            }
    
            tooltip.add(new HorizontalLayout(measureText, valueText));
        }

        return tooltip;
    }

    private void sortSolutionListBoxItems(SolutionMetaDataEnum measure)
    {
        Set<Solution> selected = solutionListBox.getSelectedItems();
        solutionListBox.setItems(this.sortSolutions(measure));
        solutionListBox.select(selected);
    }

    /** Sorts the solutions by the given enum-value by assuming that the value is of type Integer */
    private List<Solution> sortSolutions(SolutionMetaDataEnum measure)
    {
        Comparator<Solution> comparator = (Solution first, Solution second) -> 
        {              
            int firstMeasure = -1;
            if(first.getMetaData().containsKey(measure))
            {
                firstMeasure = (int) first.getMetaData().get(measure);
            }
            int secondMeasure = -1;
            if(first.getMetaData().containsKey(measure))
            {
                secondMeasure = (int) second.getMetaData().get(measure);
            }

            if(secondMeasure == -1)
                return -1;
            if(firstMeasure == -1)
                return 1;
            if( firstMeasure == secondMeasure)
                return 0;
            
            return firstMeasure < secondMeasure ? -1 : 1;
        };

        List<Solution> solutions = new ArrayList<>(conflict.getSolutions());
        solutions.sort(comparator);
        solutions.remove(customSolution);
        solutions.add(solutions.size(), customSolution);

        return solutions;
    }

    private void createAnswerSetOutput()
    {
        for (int i = 0; i < conflict.getInvolvedAnwerSets().size(); i++)
        {
            AnswerSet<?, ?> currentAnswerSet = conflict.getInvolvedAnwerSets().get(i);

            StringBuilder sb = new StringBuilder();
            sb.append(currentAnswerSet.getLiterals());
            sb.replace(0, 1, "");
            sb.replace(sb.length()-1, sb.length(), "");

            // Start counting at 1 because it is more intuitive for the user
            Div resultText = new Div();
            resultText.add(new Text(String.format("%s.Result: %s", i+1, sb.toString())));
            conflictInfoLayout.add(resultText);
        }
    }

    public Conflict getConflict() {
        return conflict;
    }

    public void setConflict(Conflict conflict) {
        this.conflict = conflict;
    }

    public SolutionGridComponent getSolutionGrid() {
        return solutionGrid;
    }

    public void setSolutionGrid(SolutionGridComponent solutionGrid) {
        this.solutionGrid = solutionGrid;
    }
}
