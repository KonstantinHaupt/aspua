package de.aspua.gui.UI.CustomComponents;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.StreamResource;

import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.gui.Backend.ASPUAFrameworkAdapter;

/**
 * Custom component for a download button (wrapped in an Anchor-component)
 */

public class DownloadWrapper extends Anchor
{
    private Button downloadButton;
    private StreamResource href;

    private ASPUAFrameworkAdapter aspuaAdapterService;

    public DownloadWrapper(ASPUAFrameworkAdapter aspuaAdapterService, ASPProgram<?, ?> initialContent, String initialFileName)
    {
        this.aspuaAdapterService = aspuaAdapterService;

        if(initialFileName == null)
            initialFileName = "file";

        if(initialContent == null)
            initialContent = new ASPProgram<>();
        
        this.configureDownloadButton();
        this.setStreamRessource(initialContent, initialFileName);
        this.getElement().setAttribute("download", true);
        this.add(downloadButton);
    }

    private void configureDownloadButton()
    {
        downloadButton = new Button(getTranslation("button.download"), VaadinIcon.DOWNLOAD_ALT.create());
    }

    public void setStreamRessource(ASPProgram<?, ?> downloadedContent , String fileName)
    {
        // Use internal method createFileInputStream() to create an exportable .txt-file
        href = new StreamResource(fileName + ".txt", () -> this.createFileInputStream(downloadedContent));
        href.setCacheTime(0);
        this.setHref(href);
    }

    private InputStream createFileInputStream(ASPProgram<?, ?> downloadedContent)
    {
        try
        {
            // Use the adapter (which uses a FileController-object) to return a .txt-file
            return new FileInputStream(aspuaAdapterService.exportProgram(downloadedContent));
        } catch (IOException e)
        {
            new ValidationNotification("The download-file couldn't be created due to an internal error. Please try again.", true)
            .open();
            return null;
        }
    }

    public Button getDownloadButton() {
        return downloadButton;
    }

    public void setDownloadButton(Button downloadButton) {
        this.downloadButton = downloadButton;
    }
}
