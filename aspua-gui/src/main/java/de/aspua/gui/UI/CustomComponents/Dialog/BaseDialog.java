package de.aspua.gui.UI.CustomComponents.Dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

/**
 * Basic Dialog layout with header and footer-row.
 */
public class BaseDialog extends Dialog
{
    // Overall layout
    protected VerticalLayout dialogLayout;

    protected HorizontalLayout headerLayout;
    protected VerticalLayout bodyLayout;
    protected VerticalLayout messageLayout;
    protected HorizontalLayout footerLayout;

    protected H3 headerTextComponent;
    protected Button cancelButton;
    protected Button nextButton;
    protected Icon closeIcon;

    public BaseDialog()
    {
        dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.addClassName("draggable");
        dialogLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        this.configureProperties();

        this.configureHeader();
        this.configureBodyLayout();
        this.configureFooterLayout();

        dialogLayout.setSizeFull();
        this.add(dialogLayout);
    }

    public BaseDialog(String headerText, String messageText)
    {
        this();
        headerTextComponent.setText(headerText);
        this.setMessageText(messageText);
    }

    private void configureProperties()
    {
        this.setWidth("700px");
        this.setHeight("400px");

        this.setCloseOnEsc(true);
        this.setCloseOnOutsideClick(false);
        this.setModal(false);
        this.setDraggable(true);
    }

    private void configureHeader()
    {
        headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setHeight("50px");
        headerLayout.setPadding(true);
        headerLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.START);

        headerTextComponent = new H3();
        headerLayout.add(headerTextComponent);
        dialogLayout.add(headerLayout);
    }

    private void configureBodyLayout()
    {
        bodyLayout = new VerticalLayout();
        bodyLayout.setJustifyContentMode(JustifyContentMode.START);

        messageLayout = new VerticalLayout();
        messageLayout.setWidthFull();
        messageLayout.setSpacing(false);
        messageLayout.setPadding(true);
        bodyLayout.add(messageLayout);
        
        Scroller bodyWrapper = new Scroller(bodyLayout);
        bodyWrapper.setSizeFull();
        dialogLayout.addAndExpand(bodyWrapper);
    }

    private void configureFooterLayout()
    {
        footerLayout = new HorizontalLayout();
        footerLayout.setWidthFull();
        footerLayout.setHeight("50px");
        footerLayout.setPadding(true);
        footerLayout.setSpacing(false);
        footerLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        footerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footerLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");

        cancelButton = new Button(getTranslation("button.cancel"));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        nextButton = new Button(getTranslation("button.next"));
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footerLayout.add(cancelButton, nextButton);
        dialogLayout.add(footerLayout);
    }

    public void setHeaderText(String newHeaderText) {
        headerTextComponent.setText(newHeaderText);
    }

    public void setMessageText(String messageText)
    {
        Div messageContainer = new Div();
        messageContainer.add(messageText);

        if(messageLayout.getComponentCount() > 0)
            messageLayout.replace(messageLayout.getComponentAt(0), messageContainer);
        else
            messageLayout.add(messageContainer);
    }

    public HorizontalLayout getFooterLayout() {
        return footerLayout;
    }
    public Button getCancelButton() {
        return cancelButton;
    }

    public Button getNextButton() {
        return nextButton;
    }
}
