package de.aspua.gui.UI.Views;

import java.util.HashMap;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.beans.factory.annotation.Autowired;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.OperationTypeEnum;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.CustomComponents.ValidationNotification;
import de.aspua.gui.UI.CustomComponents.Dialog.ValidationDialog;
import de.aspua.gui.UI.CustomComponents.Grid.BaseGridComponent;
import de.aspua.gui.UI.CustomComponents.Grid.EditableGridComponent;


/**
 * 2. View of the GUI to model the new knowledge which is merged into the initial knowledge base
 */
@Route(value = "Update", layout = MainLayout.class)
@PageTitle("Model new Knowledge | ASPUA")
@CssImport("./styles/Views/updateView-styles.css")
@CssImport(value = "./styles/Components/grid-styles.css", themeFor = "vaadin-grid")
public class UpdateView extends VerticalLayout
{
    private ASPUAFrameworkAdapter aspuaAdapterService;

    /** Components */
    private EditableGridComponent newProgramASPGrid;
    private BaseGridComponent initialProgramASPGrid;
    private HorizontalLayout buttonRowLayout;
    private Button proceedButton;

    /** Stored data */
    private ASPProgram<?,?> initialProgram;
    private ASPProgram<?,?> newProgram;

    public UpdateView(@Autowired ASPUAFrameworkAdapter aspuaAdapterService)
    {
        this.aspuaAdapterService = aspuaAdapterService;
        initialProgram = this.aspuaAdapterService.getUnmodifiedInitialProgram();
        newProgram = this.aspuaAdapterService.getUnmodifiedNewProgram();

        if(initialProgram == null)
            initialProgram = new ELPProgram();

        // Check if view is accessed from back navigation (and already contains new knowledge)
        if(newProgram == null)
        {
            newProgram = new ELPProgram();
            aspuaAdapterService.setUnsavedChanges(false);
        }
        else
            aspuaAdapterService.setUnsavedChanges(true);

        this.configureProperties();
        this.configureASPGrids();
        this.configureButtonRowLayout();
    }

    private void configureProperties()
    {
        this.addClassName("updateView-centered-content");
        this.setSpacing(true);
        this.setJustifyContentMode(JustifyContentMode.BETWEEN);
    }

    private void configureButtonRowLayout()
    {
        buttonRowLayout = new HorizontalLayout();
        buttonRowLayout.setWidthFull();
        buttonRowLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        buttonRowLayout.setJustifyContentMode(JustifyContentMode.END);

        // Configure Proceed-Button
        proceedButton = new Button(getTranslation("button.proceed"));
        proceedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        proceedButton.addClickListener(e ->
        {
            // Check if at least one new rule is introduced
            newProgram = new ELPProgram(newProgramASPGrid.getGridRules(OperationTypeEnum.ADD));
            if(newProgram.getRuleSet().isEmpty())
            {
                new ValidationNotification("The Update has to contain at least one Rule.", true)
                .open();
            }
            else
            {
                // Start conflict detection
                boolean success = aspuaAdapterService.startUpdateProcess(newProgram);
                if(!success)
                {
                    new ValidationNotification("There was a Problem with detecting possible Conflicts. Please try again.", true)
                    .open();
                }
                else
                {
                    // Check for inconsistency
                    List<AnswerSet<?,?>> answerSets = aspuaAdapterService.getCurrentUpdateAnswerSets();
                    if(answerSets == null || answerSets.isEmpty())
                    {
                        this.createValidationDialog();
                        return;
                    }

                    // Navigate to next view (depends on the existence of conflicts)
                    aspuaAdapterService.setUnsavedChanges(false);
                    List<Conflict> cachedConflicts = aspuaAdapterService.getCachedConflicts();
                    if(cachedConflicts == null || cachedConflicts.isEmpty())
                        UI.getCurrent().navigate("Merge");
                    else
                        UI.getCurrent().navigate("Conflict");
                }
            }
        });

        buttonRowLayout.add(proceedButton);
        this.add(buttonRowLayout);
    }

    /** Create validation dialog if Update-Sequence would be inconsistent */
    private void createValidationDialog()
    {
        ValidationDialog dialog = new ValidationDialog(null);
        dialog.setModal(true);
        dialog.setDraggable(false);
        dialog.setHeaderText("Conflict detection not possible");

        StringBuilder sb = new StringBuilder();
        sb.append("Caution! By adding the new rules to the initial knowledge base, the combined knowledge base contains no results.");
        sb.append(" Therefore, possible conflicts cannot be detected. Are you sure you want to continue the update process?");
        dialog.setMessageText(sb.toString());

        dialog.getCancelButton().addClickListener(e -> dialog.close());
        dialog.getNextButton().setText(getTranslation("button.yes"));
        dialog.getNextButton().removeThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.setSaveButtonClickListener(e -> 
        {
            aspuaAdapterService.setUnsavedChanges(false);
            UI.getCurrent().navigate("Merge");
            dialog.close();
        });
        dialog.open();
    }

    private void configureASPGrids()
    {
        // Configure grid for modeling new rules
        newProgramASPGrid = new EditableGridComponent(newProgram, initialProgram, null);
        newProgramASPGrid.setHeight("500px");
        newProgramASPGrid.addFilterHeaderRow();
        newProgramASPGrid.addFooterRow(false);
        newProgramASPGrid.configureCustomValueListener(aspuaAdapterService);
        newProgramASPGrid.getOperationSelect().setValue(getTranslation("select.ruleOperation.add"));
        newProgramASPGrid.getApplyButton().addClickListener(e -> aspuaAdapterService.setUnsavedChanges(true));
        this.add(newProgramASPGrid.enableGridContextMenu(true));

        // Wrap grid in Details-Component
        Details newGridDetail = new Details(new H4(getTranslation("label.details.newKB")), newProgramASPGrid);
        newGridDetail.getElement().getStyle().set("width", "100%");
        newGridDetail.setOpened(true);
        this.add(newGridDetail);

        // If rules are already entered at the initialization (e.g. caused by back-navigation), all entered rules are labeled as added
        if(!newProgram.getRuleSet().isEmpty())
        {
            HashMap<Integer, OperationTypeEnum> ruleOperationMapping = new HashMap<>();
            for (ASPRule<?> rule : newProgram.getRuleSet())
                ruleOperationMapping.put(rule.getLabelID(), OperationTypeEnum.ADD);

            newProgramASPGrid.setRuleOperationMapping(ruleOperationMapping);
        }
        
        // Configure grid for showing the initial knowledge base
        initialProgramASPGrid = new BaseGridComponent(initialProgram);
        initialProgramASPGrid.setHeight("500px");
        initialProgramASPGrid.addFilterHeaderRow();

        // Wrap grid in Details-Component
        Details initialDetail = new Details(new H4(getTranslation("label.details.oldKB") + ": "+ initialProgram.getProgramName()), initialProgramASPGrid);
        initialDetail.getElement().getStyle().set("width", "100%");
        initialDetail.setOpened(false);
        this.add(initialDetail);

        // Add click-listener to copy rule-literals into footer-row for modeling
        initialProgramASPGrid.addSelectionListener(e -> 
        {
            ELPRule selectedRule = e.getFirstSelectedItem().orElse(null);
            if(selectedRule != null)
            {
                newProgramASPGrid.deselectAll();
                newProgramASPGrid.setFooterRowValues(selectedRule);
            }
            else
            {
                if(newProgramASPGrid.getSelectedItems().isEmpty())
                    newProgramASPGrid.getRuleIDComboBox().setValue(null);
            }
        });

        newProgramASPGrid.addSelectionListener(e -> 
        {
            ELPRule selectedRule = e.getFirstSelectedItem().orElse(null);
            if(selectedRule != null)
            {
                initialProgramASPGrid.deselectAll();
                newProgramASPGrid.setFooterRowValues(selectedRule);
            }
            else
            {
                if(initialProgramASPGrid.getSelectedItems().isEmpty())
                    newProgramASPGrid.getRuleIDComboBox().setValue(null);
            }
        });
    }
}