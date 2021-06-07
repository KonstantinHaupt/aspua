package de.aspua.gui.UI.Views;

import java.util.HashMap;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.beans.factory.annotation.Autowired;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.OperationTypeEnum;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.CustomComponents.ValidationNotification;
import de.aspua.gui.UI.CustomComponents.Accordion.ConflictAccordionComponent;
import de.aspua.gui.UI.CustomComponents.Accordion.ConflictPanelComponent;
import de.aspua.gui.UI.CustomComponents.Grid.EditableGridComponent;

/**
 * 3. View of the GUI to resolve conflicts between old and new rules
 */
@Route(value = "Conflict", layout = MainLayout.class)
@PageTitle("Conflict Solving | ASPUA")
@CssImport("./styles/Views/conflictView-styles.css")
@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
public class ConflictView extends VerticalLayout implements AfterNavigationObserver
{
    private ASPUAFrameworkAdapter aspuaAdapterService;

    /** Components */
    private ConflictAccordionComponent conflictAccordion;
    private EditableGridComponent updateSequenceASPGrid;
    private Details updateSequenceDetail;

    /** Stored data */
    private List<Conflict> currentConflicts;
    private ASPProgram<?,?> initialProgram;
    private ASPProgram<?,?> newProgram;

    public ConflictView(@Autowired ASPUAFrameworkAdapter aspuaAdapterService)
    {
        this.aspuaAdapterService = aspuaAdapterService;
        newProgram = this.aspuaAdapterService.getNewProgram();
        initialProgram = this.aspuaAdapterService.getInitialProgram();
        currentConflicts = this.aspuaAdapterService.getCachedConflicts();
        aspuaAdapterService.setUnsavedChanges(false);

        if(initialProgram == null)
            initialProgram = new ELPProgram();

        if(newProgram == null)
            newProgram = new ELPProgram();

        this.configureProperties();        
        this.configureConflictAccordion();
        this.configureASPGrid();
    }

    private void configureProperties()
    {
        this.setJustifyContentMode(JustifyContentMode.START);
        this.addClassName("conflictView-centered-content");
    }

    private void configureConflictAccordion()
    {        
        conflictAccordion = new ConflictAccordionComponent(aspuaAdapterService, currentConflicts, false);

        // Register view for navigation if all conflicts are resolved
        conflictAccordion.registerView(this);
        this.add(new H4(getTranslation("label.details.conflictHeader")), conflictAccordion);
    }

    private void configureASPGrid()
    {
        // Label all new rules as added for the update sequence grid
        HashMap<Integer, OperationTypeEnum> ruleOperationMapping = new HashMap<>();
        ASPProgram<?,?> updateSequence = initialProgram.createNewInstance();
        for (ASPRule<?> currentRule : newProgram.createNewInstance().getRuleSet())
        {
            updateSequence.addRule(currentRule);
            ruleOperationMapping.put(currentRule.getLabelID(), OperationTypeEnum.ADD);
        }
        
        updateSequenceASPGrid = new EditableGridComponent(updateSequence, updateSequence, ruleOperationMapping);
        updateSequenceASPGrid.registerValueProvider(aspuaAdapterService);
        updateSequenceASPGrid.addOperationColumn();
        updateSequenceASPGrid.setHeight("500px");
        updateSequenceASPGrid.addFilterHeaderRow();

        // Add selection-listener to add all literals of the selected rule to the footer-components for rule modeling
        updateSequenceASPGrid.addSelectionListener(e -> 
        {
            if(!conflictAccordion.getOpenedPanel().isPresent())
                return;
            
            ConflictPanelComponent panel = (ConflictPanelComponent) conflictAccordion.getOpenedPanel().get();
            ELPRule selectedRule = e.getFirstSelectedItem().orElse(null);
            if(selectedRule != null)
            {
                panel.getSolutionGrid().deselectAll();
                String addLabel = panel.getSolutionGrid().getOperationTranslations().get(OperationTypeEnum.ADD);
                if(!panel.getSolutionGrid().getOperationSelect().getValue().equals(addLabel))
                    panel.getSolutionGrid().getRuleIDComboBox().setValue(selectedRule.getLabelID());
                else
                    panel.getSolutionGrid().getRuleIDComboBox().setValue(null);
                    
                panel.getSolutionGrid().setFooterRowValues(selectedRule);
            }
        });

        // Wrap grid in Details-Component
        updateSequenceDetail = new Details(new H4("Update Sequence"), updateSequenceASPGrid);
        updateSequenceDetail.getElement().getStyle().set("width", "100%");
        updateSequenceDetail.setOpened(false);
        this.add(updateSequenceDetail);
    }

    /** Is called from the ConflictAccordionComponent if all conflicts are resolved */
    public void handleNoConflicts()
    {
        currentConflicts = aspuaAdapterService.getCachedConflicts();

        // Null indicates an error. Is successful otherwise
        if(currentConflicts == null)
        {
            ValidationNotification notification = new ValidationNotification("There was a problem with computing exisiting conflicts. Please restart the update process and try again.", false);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
            return;
        }

        if(currentConflicts.isEmpty())
        {
            aspuaAdapterService.setUnsavedChanges(false);

            // Check if there are no conflicts because no conflicts exist or there was an 'illegal' forward navigation
            if(aspuaAdapterService.getNewProgram() != null)
                UI.getCurrent().navigate("Merge");
            else
                UI.getCurrent().navigate("Upload");
        }
    }

    /** Is needed for the ConflictAccordionComponent, so that the grid can be refreshed after a solution has been applied */
    public EditableGridComponent getUpdateSequenceASPGrid() {
        return updateSequenceASPGrid;
    }

    /**
     * Is needed for back-navigation from 4. View and there are no conflicts.
     * After this view is built, the navigation has to be triggered again immediately
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event)
    {
        if(currentConflicts == null || currentConflicts.isEmpty())
            this.handleNoConflicts();
    }
}