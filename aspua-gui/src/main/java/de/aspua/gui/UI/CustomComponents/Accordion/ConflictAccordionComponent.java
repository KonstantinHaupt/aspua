package de.aspua.gui.UI.CustomComponents.Accordion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.accordion.Accordion;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.Views.ConflictView;

/**
 * Custom component for displaying {@link ConflictPanelComponent}-objects.
 * Provides a compact version for only displaying the affected answer sets of conflicts.
 */
public class ConflictAccordionComponent extends Accordion
{
    private ASPUAFrameworkAdapter aspuaAdapterService;

    /** Components */
    // Reference to caller to be able to inform the view if a conflict were resolved
    private ConflictView registeredView;
    // Saves which panel corresponds to which conflict
    private Map<ConflictPanelComponent, Conflict> conflictPanelMapping;

    /** Stored data */
    private List<Conflict> conflicts;
    // Is passed to the ConflictPanelComponent-components for compact version
    private boolean compact;

    public ConflictAccordionComponent(ASPUAFrameworkAdapter aspuaAdapterService, List<Conflict> conflicts, boolean compact)
    {
        this.aspuaAdapterService = aspuaAdapterService;
        this.compact = compact;

        if(conflicts == null)
            this.conflicts = new ArrayList<>();
        else
            this.conflicts = conflicts;
            
        this.conflictPanelMapping = new HashMap<>();

        this.configureProperties();
        this.createConflictPanel();
    }

    private void configureProperties() {
        this.setSizeFull();
    }

    private void createConflictPanel()
    {
        for (Conflict conflict : this.conflicts)
        {
            ConflictPanelComponent conflictPanel = new ConflictPanelComponent(aspuaAdapterService, conflict, compact);
            this.add(conflictPanel);
            conflictPanelMapping.put(conflictPanel, conflict);
        }
    }

    protected void handleAppliedSolution()
    {
        for (ConflictPanelComponent panel : conflictPanelMapping.keySet())
            this.remove(panel);
        
        conflicts = aspuaAdapterService.getCachedConflicts();
        if(conflicts == null || conflicts.isEmpty())
            registeredView.handleNoConflicts();
        else
        {
            // The invokation of this method indicates that a solution has been applied
            aspuaAdapterService.setUnsavedChanges(true);
            registeredView.getUpdateSequenceASPGrid().refreshGridContent(true);
        }

        conflictPanelMapping = new HashMap<>();
        this.createConflictPanel();
        this.open(0);
        this.updateViewGridConflictInfo();
    }

    /** Register view so other view-components can be updated if a solution was applied */
    public void registerView(ConflictView view)
    {
        this.registeredView = view;
        this.addOpenedChangeListener(e -> this.updateViewGridConflictInfo());
    }

    /**
     * Sets the filter in the (conflict-)view grid of the update sequence to the IDs of the conflicting rules.
     * Also adds a 'Conflicting'-label for each conflicting rule in the grid.
     */
    private void updateViewGridConflictInfo()
    {
        ConflictPanelComponent openedPanel;
        if(this.getOpenedPanel().isEmpty())
        {
            registeredView.getUpdateSequenceASPGrid().setConflictingRules(null);
            return;
        }
        else
            openedPanel = (ConflictPanelComponent) this.getOpenedPanel().get();

        List<ASPRule<?>> conflictingRules = openedPanel.getConflict().getConflictingRules();
        StringBuilder sb = new StringBuilder();

        for (ASPRule<?> currentRule : conflictingRules)
            sb.append(currentRule.getLabelID() + " ");
        
        List<ELPRule> castedConflictingRules = new ArrayList<>();
        conflictingRules.forEach(rule -> castedConflictingRules.add((ELPRule) rule));
        registeredView.getUpdateSequenceASPGrid().setConflictingRules(castedConflictingRules);
        registeredView.getUpdateSequenceASPGrid().getFilterTextField().setValue(sb.toString());
    }
}
