package de.aspua.gui.UI.Views;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;
import de.aspua.gui.UI.CustomComponents.DownloadWrapper;
import de.aspua.gui.UI.CustomComponents.ValidationNotification;
import de.aspua.gui.UI.CustomComponents.Dialog.ValidationDialog;
import de.aspua.gui.UI.CustomComponents.Grid.BaseGridComponent;

/**
 * 1. View of the GUI to choose the initial ASP-Program for updating
 */
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "Upload", layout = MainLayout.class)
@PageTitle("Upload | ASPUA")
@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
public class UploadView extends VerticalLayout
{
    private static Logger LOGGER = LoggerFactory.getLogger(UploadView.class);
    private ASPUAFrameworkAdapter aspuaAdapterService;
    
    /** Components */
    private HorizontalLayout uploadLayout;
    private HorizontalLayout selectionLayout;
    private Select<ASPProgram<?,?>> programSelect;
    private Button deleteProgramButton;
    private BaseGridComponent gridChooseProgram;
    private Upload upload;
    private TextField textFieldProgramName;
    private DownloadWrapper downloadWrapper;
    private Button proceedButton;

    /** Stored data */
    private List<ASPProgram<?,?>> availablePrograms;

    public UploadView(@Autowired ASPUAFrameworkAdapter aspuaAdapterService)
    {
        this.aspuaAdapterService = aspuaAdapterService;

        // Remove existing new Program if the view is called from Navigation
        this.aspuaAdapterService.setUnsavedChanges(false);
        this.aspuaAdapterService.setNewProgram(null);
        availablePrograms = this.aspuaAdapterService.loadAvailablePrograms();

        if(availablePrograms == null)
            availablePrograms = new ArrayList<>();
        
        this.configureProperties();
        this.configureGrid();
        this.configureSelectionLayout();
        this.configureUploadRow();

        this.add(gridChooseProgram, uploadLayout);
    }

    private void configureProperties()
    {
        this.setPadding(false);
        this.addClassName("centered-content");
        this.setHeightFull();
    }

    private void configureSelectionLayout()
    {
        selectionLayout = new HorizontalLayout();
        selectionLayout.setWidthFull();
        selectionLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        
        // Configure Select-Component
        programSelect = new Select<>();
        programSelect.setWidth("20%");
        programSelect.setLabel(getTranslation("label.selectKB"));
        programSelect.setItemLabelGenerator(ASPProgram<?,?>::getProgramName);
        programSelect.setItems(availablePrograms);

        if(availablePrograms != null && !availablePrograms.isEmpty())
            programSelect.setValue(availablePrograms.get(0));

        programSelect.addValueChangeListener(event -> gridChooseProgram.setASPProgram((ELPProgram) event.getValue()));

        // Configure Download-Wrapper/Button
        ASPProgram<?,?> selectedProgram = programSelect.getValue();
        if(selectedProgram == null)
            downloadWrapper = new DownloadWrapper(aspuaAdapterService, selectedProgram, "file");
        else
            downloadWrapper = new DownloadWrapper(aspuaAdapterService, selectedProgram, programSelect.getValue().getProgramName());

        programSelect.addValueChangeListener(e -> 
        {
            // Workaround: Event is fired twice after uploading a new program and has a null-Value at the first time
            // The If-Statement prevends NullPointerExceptions, but should be overwritten by the Else-Statement when the event is fired the second time
            if(e.getValue() == null)
                downloadWrapper.setStreamRessource(availablePrograms.get(0), availablePrograms.get(0).getProgramName());
            else
                downloadWrapper.setStreamRessource(e.getValue(), e.getValue().getProgramName());
        });
        
        // Configure Delete-Button
        deleteProgramButton = new Button(getTranslation("button.delete"));
        deleteProgramButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteProgramButton.addClickListener(e ->
        {
            if(availablePrograms.size() < 2)
            {
                new ValidationNotification("At least one knowledge base should always be available at all time." , true)
                .open();
            }
            else
                this.createDeleteDialog();
        });

        selectionLayout.add(programSelect, downloadWrapper, deleteProgramButton);
        this.add(selectionLayout);
    }

    private void createDeleteDialog()
    {
        ValidationDialog deleteDialog = new ValidationDialog(null);
        deleteDialog.setHeaderText("Delete selected Knowledge Base");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Are you sure you want to delete the knowledge base '%s'? ", programSelect.getValue().getProgramName()));
        sb.append("The knowledge base will be permanently removed from the application. ");
        sb.append("You can save the knowledge base on your device beforehand by clicking the 'Download'-Button.");
        deleteDialog.setMessageText(sb.toString());

        deleteDialog.getNextButton().setText(getTranslation("button.delete"));
        deleteDialog.setSaveButtonClickListener(e ->
        {
            String deletedProgram = programSelect.getValue().getProgramName();
            boolean success = aspuaAdapterService.deleteProgram(programSelect.getValue());
    
            if(success)
            {
                new ValidationNotification(String.format("The knowledge base '%s' was successfully deleted.", deletedProgram), false)
                .open();
                
                availablePrograms = aspuaAdapterService.loadAvailablePrograms();
                programSelect.setItems(availablePrograms);
                programSelect.setValue(availablePrograms.get(0));
            }
            else
            {
                new ValidationNotification("The selected knowledge base couldn't be deleted due to an internal error. Please try again.", true)
                .open();
            }
            deleteDialog.close();
        });

        deleteDialog.open();
    }

    private void configureGrid()
    {
        if(availablePrograms != null && !availablePrograms.isEmpty())
            gridChooseProgram = new BaseGridComponent(availablePrograms.get(0));
        else
            gridChooseProgram = new BaseGridComponent(null);
        
        gridChooseProgram.addFilterHeaderRow();
        gridChooseProgram.setSizeFull();
    }

    private void configureUploadRow()
    {
        uploadLayout = new HorizontalLayout();
        uploadLayout.setWidthFull();

        MemoryBuffer memoryBuffer = new MemoryBuffer();
        upload = new Upload(memoryBuffer);
        upload.setWidth("350px");
        upload.setAcceptedFileTypes("text/*");

        // Overwrite default-Button because the .getUploadButton()-Method returns null otherwise
        upload.setUploadButton(new Button(getTranslation("button.upload")));
        textFieldProgramName = new TextField();
        upload.setDropLabel(textFieldProgramName);
        this.configureUploadTextField();
        upload.setAutoUpload(false);

        // Add listener to validate the entered name & parse the uploaded file
        upload.addStartedListener(e -> this.validateUploadTextField());
        upload.addSucceededListener(e -> this.addUploadedProgram(memoryBuffer.getInputStream(), textFieldProgramName.getValue()));

        // Configure Proceed-button to navigate to the second view
        proceedButton = new Button(getTranslation("button.proceed"));
        proceedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        proceedButton.addClickListener(e -> 
        {
            ASPProgram<?,?> selectedProgram = programSelect.getValue();
            if(selectedProgram == null)
            {
                new ValidationNotification("Please select a knowledge base first.", true)
                    .open();
                return;
            }
            aspuaAdapterService.setInitialProgram(selectedProgram.getProgramName());
            UI.getCurrent().navigate("Update");
        });

        uploadLayout.add(upload, proceedButton);
        uploadLayout.setSpacing(false);
        uploadLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        uploadLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        this.add(uploadLayout);
    }

    /** Checks for unique name for new KB */
    private void validateUploadTextField()
    {
        String value = textFieldProgramName.getValue().trim();
        boolean validTextValue = true;

        if("".equals(value))
            validTextValue = false;
        else
        {
            for (ASPProgram<?,?> elpProgram : availablePrograms)
            {
                if(value.equals(elpProgram.getProgramName()))
                {
                    validTextValue = false;
                    break;
                }
            }
        }

        // If entered name isn't unique, the upload is indicated as failed
        if(!validTextValue)
        {
            upload.interruptUpload();
            LOGGER.warn("Interrupted File-Upload because the program-name '{}' is invalid.", value);

            textFieldProgramName.setInvalid(true);
            new ValidationNotification("Please choose a non-empty name for the uploaded knowledge base which doesn't already exist.", true)
            .open();
        }
        else
            textFieldProgramName.setInvalid(false);
    }

    private void configureUploadTextField()
    {
        textFieldProgramName.setPlaceholder(getTranslation("placeholder.upload.name"));
        textFieldProgramName.setClearButtonVisible(true);
        textFieldProgramName.setWidth("85%");
    }

    private void addUploadedProgram(InputStream stream, String programName)
    {
        try
        {
            String programString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            ASPProgram<?,?> uploadedProgram = aspuaAdapterService.parseUploadedProgram(programString, programName);

            // Null indicates an error
            if(uploadedProgram == null)
            {
                LOGGER.warn("The uploaded file couldn't be resolved to an ASP-Program! The upload will be aborted.");
                new ValidationNotification("The uploaded file is invalid! Please ensure the file content has the expected format.", true)
                .open();
                return;
            }

            boolean success = aspuaAdapterService.addUploadedProgram(uploadedProgram);
            if(success)
            {
                programSelect.setItems(availablePrograms);
                programSelect.setValue(uploadedProgram);
                String validationMessage = String.format("The knowledge base '%s' was successfully uploaded.", uploadedProgram.getProgramName());
                new ValidationNotification(validationMessage, false)
                .open();
                LOGGER.info("Succesfully uploaded the ASP-Program {}.", uploadedProgram.getProgramName());
            }
            else
            {
                upload.interruptUpload();
                LOGGER.warn("An error occured while adding the uploaded Program to the List of existing Programs! The upload will be aborted.");
                new ValidationNotification("An internal error occured while trying to upload the knowledge base. Please try again.", true)
                .open();
            }

        } catch (IOException e) {
            upload.interruptUpload();
            LOGGER.error("An error occured while trying to parse the uploaded file to a String!", System.lineSeparator(), e);
            ValidationNotification notification = new ValidationNotification("An internal Error occured! Please ensure the file content has the expected format.", false);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        }
    }
}

