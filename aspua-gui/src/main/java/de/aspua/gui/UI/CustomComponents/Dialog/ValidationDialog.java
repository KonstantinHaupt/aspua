package de.aspua.gui.UI.CustomComponents.Dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.shared.Registration;

/**
 * Validation-Dialog. Examples of use-cases are errors or warnings for unsaved changes.
 */
public class ValidationDialog extends BaseDialog 
{
    private ContinueNavigationAction action;
    private Registration nextButtonListener;
    private Registration cancelButtonListener;

    /** Action can be provided if a navigation should be performed, e.g. if the user chooses the 'save'-option. Pass null otherwise. */
    public ValidationDialog(ContinueNavigationAction action)
    {
        super("Discard Changes", 
            "All changes you made up to the selected step will be discarded. Are you sure you want to leave?");
        this.action = action;

        this.configureProperties();
        this.configureButtons();
        this.open();
    }

    private void configureProperties()
    {
        this.setWidth("500px");
        this.setHeight("250px");
        this.setModal(true);
        this.setDraggable(false);
    }

    private void configureButtons()
    {
        nextButton.setText(getTranslation("button.discard"));
        nextButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelButton.removeThemeVariants(ButtonVariant.LUMO_ERROR);
        
        nextButtonListener = nextButton.addClickListener(event -> 
        {
            this.close();
            action.proceed();
        });

        cancelButtonListener = cancelButton.addClickListener(event -> this.close());
    }

    /** Reassign the behaviour (clicklistener) of the save-button */
    public void setSaveButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener)
    {
        nextButtonListener.remove();
        nextButtonListener = nextButton.addClickListener(listener);
    }

    /** Reassign the behaviour (clicklistener) of the cancel-button */
    public void setCancelButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener)
    {
        cancelButtonListener.remove();
        cancelButtonListener = cancelButton.addClickListener(listener);
    }
}
