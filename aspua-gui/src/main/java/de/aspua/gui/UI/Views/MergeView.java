package de.aspua.gui.UI.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.HeaderRow.HeaderCell;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.beans.factory.annotation.Autowired;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.CustomComponents.DownloadWrapper;
import de.aspua.gui.UI.CustomComponents.ValidationNotification;
import de.aspua.gui.UI.CustomComponents.Dialog.ValidationDialog;
import de.aspua.gui.UI.CustomComponents.Grid.EditableGridComponent;

/**
 * 4. View of the GUI to merge the Update-Sequence into one knowledge base and finish the update process
 */
@Route(value = "Merge", layout = MainLayout.class)
@PageTitle("Save updated KB | ASPUA")
public class MergeView extends VerticalLayout
{    
    private ASPUAFrameworkAdapter aspuaAdapterService;

    /** Components */
    private HorizontalLayout buttonLayout;
    private EditableGridComponent finalProgramGrid;
    private DownloadWrapper updateSequenceDownload;
    private Button saveButton;
    private Button finishButton;

    private TextField newEntryTextField;
    private Button newEntryButton;

    /** Stored data */
    private ASPProgram<?,?> finalUpdateSequence;

    public MergeView(@Autowired ASPUAFrameworkAdapter aspuaAdapterService)
    {
        this.aspuaAdapterService = aspuaAdapterService;
        this.aspuaAdapterService.setUnsavedChanges(true);
        finalUpdateSequence = this.aspuaAdapterService.getMergedUpdateSequence();
        
        if(finalUpdateSequence == null)
            finalUpdateSequence = new ELPProgram();

        this.configureProperties();
        this.configureGridLayout();
        this.configureButtonLayout();
    }

    private void configureProperties()
    {
        this.setPadding(false);
        this.addClassName("centered-content");
        this.setHeightFull();
    }

    private void configureGridLayout()
    {
        // Configure Grid-Component
        H3 gridHeader = new H3(getTranslation("label.finalProgram"));
        finalProgramGrid = new EditableGridComponent(finalUpdateSequence.createNewInstance(), null, null);
        finalProgramGrid.setSizeFull();
        finalProgramGrid.registerValueProvider(aspuaAdapterService);
        finalProgramGrid.refreshGridContent(false);

        // Add custom button in header row to show changes from update process
        HeaderRow filterHeaderRow = finalProgramGrid.addFilterHeaderRow();
        HeaderCell filterHeaderCell = filterHeaderRow.getCell(finalProgramGrid.getColumnByKey("ID"));
        Checkbox operationChangesCheckbox = new Checkbox(getTranslation("checkbox.grid.operationChanges"), false);
        operationChangesCheckbox.addValueChangeListener(e ->
        {
            if(e.getValue())
                finalProgramGrid.addOperationColumn();
            else
                finalProgramGrid.removeColumnByKey("operation");

            finalProgramGrid.refreshGridContent(e.getValue());
        });
        HorizontalLayout cellLayout = new HorizontalLayout(finalProgramGrid.getFilterTextField(), operationChangesCheckbox);
        cellLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        filterHeaderCell.setComponent(cellLayout);

        this.add(gridHeader, finalProgramGrid);
    }

    private void configureButtonLayout()
    {
        // Configure finish button
        finishButton = new Button(getTranslation("button.finish"));
        finishButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        finishButton.addClickListener(e ->
        {
            // Show warning-dialog if updated knowledge base wasn't saved
            if(aspuaAdapterService.hasUnsavedChanges())
            {
                ValidationDialog unsavedDialog = new ValidationDialog(null);
                unsavedDialog.setHeaderText("Unsaved updated knowledge base");

                StringBuilder sb = new StringBuilder();
                sb.append("The updated knowledge base wasn't saved within the application.");
                sb.append(" If you finish the update process without saving, all changes which were made to the initial knowledge base will be discarded!");
                sb.append(" Are you sure you want to continue without saving?");
                
                unsavedDialog.setMessageText(sb.toString());
                unsavedDialog.setSaveButtonClickListener(event ->
                {
                    unsavedDialog.close();
                    aspuaAdapterService.setUnsavedChanges(false);
                    UI.getCurrent().navigate("");
                });
            }
            else
                UI.getCurrent().navigate("");
        });

        // Configure Download-Button
        Div filler = new Div();
        updateSequenceDownload = new DownloadWrapper(aspuaAdapterService, finalUpdateSequence, finalUpdateSequence.getProgramName());

        // Add all Buttons to one layout to display them in a row
        buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.add(updateSequenceDownload, filler);

        this.configureInternalSaveComponents();
        buttonLayout.add(finishButton);
        buttonLayout.expand(filler);
        buttonLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        this.add(buttonLayout);
    }

    /** Configures the Save and overwrite button */
    private void configureInternalSaveComponents()
    {
        // Configure Textfield to enter name of new knowledge base
        newEntryTextField = new TextField();
        newEntryTextField.setWidth("20%");
        newEntryTextField.setPlaceholder(getTranslation("placeholder.save.newEntryName"));
        newEntryButton = new Button(getTranslation("button.newEntry"));
        newEntryButton.addClickListener(event -> 
        {
            boolean validTextValue = this.validateTextField();
            if(!validTextValue)
            {
                newEntryTextField.setErrorMessage("Please choose a non-empty name which doesn't already exist.");
                newEntryTextField.setInvalid(true);
                return;
            }

            this.saveUpdateSequence(newEntryTextField.getValue().trim());
        });

        // Configure overwrite-button
        saveButton = new Button(getTranslation("button.saveOverwrite"));
        saveButton.addClickListener(e -> this.openOverwriteDialog());

        buttonLayout.add(newEntryTextField, newEntryButton);
        buttonLayout.add(saveButton);
    }

    /** Check if the name for new knowledge base already exists */
    private boolean validateTextField()
    {
        String value = newEntryTextField.getValue().trim();
        boolean validTextValue = true;

        if("".equals(value))
            validTextValue = false;
        else
        {
            for (ASPProgram<?,?> currentProgram : aspuaAdapterService.loadAvailablePrograms())
            {
                if(value.equals(currentProgram.getProgramName()))
                {
                    validTextValue = false;
                    break;
                }
            }
        }
        
        return validTextValue;
    }

    private void openOverwriteDialog()
    {
        ValidationDialog dialog = new ValidationDialog(null);
        dialog.setHeaderText("Overwrite existing knowledge base");
        dialog.setMessageText("You can save the initial knowledge base by downloading it beforehand. Otherwise, the initial knowledge base will be overwritten and can't be accessed anymore. " +
                "Are you sure you want to continue?");
        
        Button dialogCancel = dialog.getCancelButton();
        Button dialogNext = dialog.getNextButton();
        dialogNext.setText(getTranslation("button.overwrite"));
        dialogNext.removeThemeVariants(ButtonVariant.LUMO_ERROR);
        
        dialogCancel.setText(getTranslation("button.cancel"));
        dialogCancel.addClickListener(event -> dialog.close());
        dialog.setSaveButtonClickListener(event -> 
        {
            this.saveUpdateSequence(null);
            saveButton.setEnabled(false);
            dialog.close();
        });

        // Configure Download-Wrapper within the overwrite dialog to enable downloading before overwriting
        DownloadWrapper dialogDownload = new DownloadWrapper(aspuaAdapterService, aspuaAdapterService.getUnmodifiedInitialProgram(), finalUpdateSequence.getProgramName() + "_initial");
        dialogDownload.getDownloadButton().setText(getTranslation("button.download") + " initial KB");

        // Wrap cancel-download-overwrite buttons in a single row-layout
        HorizontalLayout dialogFooter = dialog.getFooterLayout();
        dialogFooter.removeAll();
        dialogFooter.add(dialogCancel, dialogDownload, dialogNext);
        dialogFooter.setSpacing(false);
        dialogFooter.setJustifyContentMode(JustifyContentMode.BETWEEN);

        dialog.open();
    }

    /** Used to save the updated knowledge base under the given name. Null indicates overwriting */
    private boolean saveUpdateSequence(String programName)
    {
        boolean success = aspuaAdapterService.persistUpdatedProgram(programName);
        if(success)
        {
            ValidationNotification notification = new ValidationNotification("The updated knowledge base was successfully saved.", false);
            notification.getLabel().getStyle().set("color", "var(--lumo-success-color)");
            notification.open();
            aspuaAdapterService.setUnsavedChanges(false);
        }
        else 
        {
            new ValidationNotification(
                "An error occured while trying to save the updated knowledge base. " +
                "Please try again or consider to download the knowledge base as a text-file.", true)
            .open();
        }

        return success;
    }
}
