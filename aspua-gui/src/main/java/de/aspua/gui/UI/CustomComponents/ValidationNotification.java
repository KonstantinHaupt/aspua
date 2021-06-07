package de.aspua.gui.UI.CustomComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

/**
 * Custom component to display notifications with error indication (red font color)
 */
public class ValidationNotification extends Notification
{
    private HorizontalLayout componentLayout;
    private Span label;
    private Button closeButton;

    public ValidationNotification(String message, boolean error)
    {
        label = new Span(message);
        label.getStyle().set("margin-right", "0.5rem");
        
        if(error)
            label.getStyle().set("color", "var(--lumo-error-color)");

        closeButton = new Button(getTranslation("button.close"), event -> this.close());
        closeButton.getStyle().set("margin-right", "0.5rem");

        componentLayout = new HorizontalLayout(label, closeButton);
        componentLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        this.add(componentLayout);
        this.setPosition(Position.BOTTOM_CENTER);
    }

    public HorizontalLayout getComponentLayout() {
        return componentLayout;
    }

    public void setComponentLayout(HorizontalLayout componentLayout) {
        this.componentLayout = componentLayout;
    }

    public Span getLabel() {
        return label;
    }

    public void setLabel(Span label) {
        this.label = label;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public void setCloseButton(Button closeButton) {
        this.closeButton = closeButton;
    }
}
