package de.aspua.gui.UI.Views;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.CustomComponents.DrawerContentComponent;
import de.aspua.gui.UI.CustomComponents.Dialog.ValidationDialog;

/**
 * Overall Layout for all Views of the GUI
 */
@PWA(name = "Answer Set Programming Update Application",
     shortName = "ASPUA",
     description = "Application for updating Knowledge Bases",
     enableInstallPrompt = false)
@CssImport("./styles/shared-styles.css")
@CssImport(value = "./styles/Components/dialog-styles.css", themeFor = "vaadin-dialog-overlay")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@Theme(value = Lumo.class)
public class MainLayout extends AppLayout implements BeforeLeaveObserver, AfterNavigationObserver
{
    private static Logger LOGGER = LoggerFactory.getLogger(MainLayout.class);
    private ASPUAFrameworkAdapter aspuaAdapterService;

    /** Components */
    private DrawerToggle drawerToggle;
    private DrawerContentComponent drawerContent;
    private HorizontalLayout header;
    private Tabs headerTabs;
    private List<Tab> tabList;

    /** Stored data */
    private String previousView;

    public MainLayout(@Autowired ASPUAFrameworkAdapter aspuaService)
    {
        this.aspuaAdapterService = aspuaService;
        this.configureTabs();
        this.createDrawer();
        this.createHeader();
    }
    
    /** Used to prevent navigation to "unallowed" views, i.e. foward-navigation via URL */
    @Override
    public void afterNavigation(AfterNavigationEvent event)
    {
        String path = event.getLocation().getPath();
        ASPProgram<?, ?> initialProgram = aspuaAdapterService.getInitialProgram();
        Boolean hasProgram = initialProgram != null && !initialProgram.getRuleSet().isEmpty();
        
        if (!hasProgram && !(path.equals("") || path.equals("Upload")))
        {
            aspuaAdapterService.setUnsavedChanges(false);
            UI.getCurrent().navigate("");
        }
    }

    /** Adds navigation-layout with help-icon, logo and tabs */
    private void createHeader()
    {
        H1 logo = new H1("ASPUA");
        logo.addClassName("logo");
        
        header = new HorizontalLayout(drawerToggle, logo, headerTabs);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerTabs.setWidthFull();
        headerTabs.setFlexGrowForEnclosedTabs(1);
        header.setWidth("100%");
        header.addClassName("header");

        this.addToNavbar(header);
    }

    /** Creates the sidebar */
    private void createDrawer()
    {
        drawerToggle = new DrawerToggle();
        drawerToggle.setIcon(new Icon(VaadinIcon.QUESTION_CIRCLE_O));

        // Change logo depending on visibility of drawer
        drawerToggle.addClickListener(e -> 
        {
            if(this.isDrawerOpened())
                drawerToggle.setIcon(new Icon(VaadinIcon.QUESTION_CIRCLE));
            else
                drawerToggle.setIcon(new Icon(VaadinIcon.QUESTION_CIRCLE_O));
        });

        drawerContent = new DrawerContentComponent();
        this.addToDrawer(drawerContent);
        this.setDrawerOpened(false);
    }

    /** 
     * Updated the enabled-state of the tab-entries depending on the current view.
     * Prevents forward navigation and shows "update-progess".
     * */
    @Override
    public void showRouterLayoutContent(HasElement content)
    {
        super.showRouterLayoutContent(content);

        int tabIndex = -1;
        if(content instanceof UploadView)
        {
            tabIndex = 0;
        }
        else if (content instanceof UpdateView)
        {
            tabIndex = 1;
        }
        else if(content instanceof ConflictView)
        {
            tabIndex = 2;
        }
        else if(content instanceof MergeView)
        {
            tabIndex = 3;
        }

        if(tabIndex == -1)
        {
            LOGGER.warn("There's no dedicated Tab for the selected page! Therefore, only the first Tab will be enabled.");
            tabIndex = 0;
        }
        
        for(int i = 0; i < headerTabs.getComponentCount(); i++)
        {
            ((Tab) headerTabs.getComponentAt(i)).setEnabled(i <= tabIndex);
        }

        headerTabs.setSelectedIndex(tabIndex);
    }

    /** Configures Tabs and adds click-listener to navigate */
    private void configureTabs()
    {
        tabList = new ArrayList<>();

        Tab initial = new Tab(getTranslation("label.navigation.initial"));
        initial.setId("Upload");
        tabList.add(initial);

        Tab update = new Tab(getTranslation("label.navigation.update"));
        update.setId("Update");
        tabList.add(update);

        Tab conflict = new Tab(getTranslation("label.navigation.conflict"));
        conflict.setId("Conflict");
        tabList.add(conflict);

        Tab merge = new Tab(getTranslation("label.navigation.merge"));
        merge.setId("Merge");
        tabList.add(merge);

        headerTabs = new Tabs(false, tabList.toArray(new Tab[tabList.size()]));
        headerTabs.addSelectedChangeListener(e ->
        {
            // Check the differences between the previous view & the opened view:
            // -> If several steps (i.e.) views are skipped in the navigation, a ValidationDialog should appear,
            //    because if the previous and future view aren't adjacent, there are always unsaved changes for the skipped views/steps.
            // -> Exception: Last view to first view, which indicates a finished update-process
            Tab previousTab = e.getPreviousTab();
            int previousTabIndex = tabList.indexOf(previousTab);
            int tabDifference = previousTabIndex - headerTabs.getSelectedIndex();
            if(tabDifference > 1 & tabDifference < 3)
                aspuaAdapterService.setUnsavedChanges(true);

            // Check for previous path prevents infinite loop with showRouterLayoutContent()-Method
            // Can't use the previousTab-ID because the previousTab is null when the Upload-View is initially built after starting the Application
            previousView = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
            
            switch (e.getSelectedTab().getId().orElse("Upload"))
            {
                case "Upload":
                    drawerContent.setContentForView("Upload");
                    if(!"Upload".equals(previousView))
                        UI.getCurrent().navigate("Upload");
                    break;
                case "Update":
                    drawerContent.setContentForView("Update");
                    if(!"Update".equals(previousView))
                        UI.getCurrent().navigate("Update");
                    break;
                case "Conflict":
                     drawerContent.setContentForView("Conflict");
                    if(!"Conflict".equals(previousView))
                    {
                        // Reset the update sequence to its state before applying solutions
                        ASPProgram<?, ?>  unmodifiedInitial = aspuaAdapterService.getUnmodifiedInitialProgram();
                        ASPProgram<?, ?> unmodifiedNew = aspuaAdapterService.getUnmodifiedNewProgram();
                        aspuaAdapterService.setInitialProgram(unmodifiedInitial);
                        aspuaAdapterService.startUpdateProcess(unmodifiedNew);
                        UI.getCurrent().navigate("Conflict");
                    }
                    break;
                case "Merge":
                    drawerContent.setContentForView("Merge");
                    if(!"Merge".equals(previousView))
                        UI.getCurrent().navigate("Merge");
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * After a navigation via the tab-entries is triggered, the selected tab is selected immediately.
     * If the navigation is aborted by clicking "Cancel" in a ValidationDialog for unsaved changes,
     * the selected tab has to be resetted to match the (unchanged) view.
     * @see #beforeLeave(BeforeLeaveEvent)
     */
    private void checkTabStatus()
    {
        String currentView = UI.getCurrent().getInternals().getActiveViewLocation().getPath();

        if(!currentView.equals(headerTabs.getSelectedTab().getId().get()))
        {
            Tab searchedViewTab = null;
            for (Tab currentTab : tabList)
            {
                if(currentView.equals(currentTab.getId().get()))
                {
                    searchedViewTab = currentTab;
                    break;
                }
            }

            if(searchedViewTab != null)
                headerTabs.setSelectedTab(searchedViewTab);
        }
    }

    /** Use the adapter service to decide whether a ValidationDialog with a warning for unsaved changes has to appear */
    @Override
    public void beforeLeave(BeforeLeaveEvent event)
    {
        ContinueNavigationAction action = event.postpone();
        if(!aspuaAdapterService.hasUnsavedChanges())
            action.proceed();
        else
        {
            ValidationDialog dialog = new ValidationDialog(action);
            dialog.getCancelButton().addClickListener(e -> this.checkTabStatus());
        }
    }
}