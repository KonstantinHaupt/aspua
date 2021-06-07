package de.aspua.gui.UI.CustomComponents.Dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Utils.SolutionMetaDataEnum;
import de.aspua.gui.UI.CustomComponents.Accordion.ConflictAccordionComponent;

/**
 * Dialog for displaying a comparison between the current answer sets and the answer sets after applying a given solution.
 */
public class SolutionPreviewDialog extends BaseDialog
{
    private boolean solved;
    private Solution chosenSolution;
    private Conflict previousConflict;
    private List<Conflict> newConflicts;
    private List<AnswerSet<?, ?>> previousAnswerSets;
    private List<AnswerSet<?, ?>> newAnswerSets;
    
    public SolutionPreviewDialog(Solution chosenSolution, List<Conflict> newConflicts,
                                 List<AnswerSet<?, ?>> previousAnswerSets, List<AnswerSet<?, ?>> newAnswerSets)
    {
        super();

        if(chosenSolution == null)
            this.chosenSolution = new Solution(null, null, null, null);
        else
            this.chosenSolution = chosenSolution;

        if(chosenSolution.getCause() == null)
            this.previousConflict = new Conflict(null, null);
        else
            this.previousConflict = chosenSolution.getCause();
        
        if(newConflicts == null)
            this.newConflicts = new ArrayList<>();
        else
            this.newConflicts = newConflicts;

        if(previousAnswerSets == null)
            this.previousAnswerSets = new ArrayList<>();
        else
            this.previousAnswerSets = previousAnswerSets;

        if(newAnswerSets == null)
            this.newAnswerSets = new ArrayList<>();
        else
            this.newAnswerSets = newAnswerSets;

        solved = true;
        for (Conflict conflict : newConflicts)
        {
            if(conflict.getConflictingRules().equals(previousConflict.getConflictingRules()))
            {
                solved = false;
                break;
            }
        }

        this.customizeBaseDialog();
        this.addAnswerSetDetails();
        this.addConflictDetails();
    }

    private void customizeBaseDialog()
    {
        this.setWidth("800px");
        this.setHeight("650px");

        this.setModal(true);
        this.setDraggable(true);

        this.generateHeader();
        this.generateMessageText();

        footerLayout.remove(cancelButton);
        footerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        nextButton.setText(getTranslation("button.okay"));
        nextButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        nextButton.addClickListener(e -> this.close());
    }

    /** Set header-text and suitable icon depending on solve-state/ consistency */
    private void generateHeader()
    {
        Icon headerLogo;

        // Check for valid conflict detection
        if(newAnswerSets == null || newAnswerSets.isEmpty())
        {
            headerTextComponent.setText("Preview: Conflicts cannot be detected");
            return;
        }

        if(solved)
        {
            headerLogo = new Icon(VaadinIcon.CHECK);
            headerLogo.setSize("20px");
            headerLogo.setColor("green");
            headerTextComponent.setText("Preview: Conflict will be solved");
            headerLayout.add(headerLogo);
        }
        else
        {
            headerLogo = new Icon(VaadinIcon.CLOSE);
            headerLogo.setSize("20px");
            headerLogo.setColor("red");
            headerTextComponent.setText("Preview: Conflict won't be solved");
            headerLayout.add(headerLogo);
        }

        // Use margins from H3 to align the icon correctly to the text
        headerLogo.getStyle().set("margin-top", "22.5px");
        headerLogo.getStyle().set("margin-bottom", "10px");
    }

    private void generateMessageText()
    {
        StringBuilder sb = new StringBuilder();
        ASPRule<?> oldRule = previousConflict.getConflictingRules().get(0);
        ASPRule<?> newRule = previousConflict.getConflictingRules().get(1);

        // Check for valid conflict detection
        if(newAnswerSets == null || newAnswerSets.isEmpty())
        {
            sb.append("Caution! If the selected solution is applied, the knowledge base becomes inconsistent, i.e. doesn't contain any results.");
            sb.append(" It is recommended that a knowledge base stays consistent at all times, especially because it is not possible to detect conflicts in inconsistent knowledge bases.");
        }
        else
        {
            if(solved)
            {
                sb.append(String.format("The conflict between rule %s and %s will be solved by the entered solution.",
                    oldRule.getLabelID(), newRule.getLabelID()));
                
                if(newConflicts == null || newConflicts.isEmpty())
                    sb.append(" No conflicts remain after the entered solution is applied.");
                else
                    sb.append(" The following conflict(s) remain(s) after the entered solution is applied.");
            }
            else
            {
                sb.append(String.format("Caution! The conflict between rule %s and %s won't be solved by the entered solution!",
                    oldRule.getLabelID(), newRule.getLabelID()));
                sb.append(" The following conflict(s) remain(s) after the entered solution is applied.");
            }
        }

        super.setMessageText(sb.toString());
        messageLayout.add(new Paragraph());

        /** Compare to ConflictPanelComponent.createTooltip() */
        HashMap<SolutionMetaDataEnum, String> measureTranslations = new LinkedHashMap<>();
        measureTranslations.put(SolutionMetaDataEnum.MEASURE_ANSWERSETCHANGES, getTranslation("label.measure.answerSetChanges"));
        measureTranslations.put(SolutionMetaDataEnum.MEASURE_RULECHANGES, getTranslation("label.measure.ruleChanges"));
        for (SolutionMetaDataEnum solutionMeasure : measureTranslations.keySet())
        {
            Text measureText = new Text(measureTranslations.get(solutionMeasure) + ": ");
            Text valueText = new Text("-");
            if(chosenSolution.getMetaData().containsKey(solutionMeasure))
            {
                int measureValue = (int) chosenSolution.getMetaData().get(solutionMeasure);
                valueText = new Text(Integer.toString(measureValue));
            }
            messageLayout.add(new Div(measureText, valueText));
        }
    }

    private void addAnswerSetDetails()
    {
        HorizontalLayout wrapperLayout = new HorizontalLayout();
        wrapperLayout.setSizeFull();

        VerticalLayout oldResultsLayout = new VerticalLayout();
        oldResultsLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        oldResultsLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        oldResultsLayout.add(new H6("Current Result(s)"));
        this.createAnswerSetOutput(oldResultsLayout, previousAnswerSets);

        VerticalLayout newResultsLayout = new VerticalLayout();
        newResultsLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        newResultsLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        newResultsLayout.add(new H6("New Result(s)"));
        this.createAnswerSetOutput(newResultsLayout, newAnswerSets);

        wrapperLayout.add(oldResultsLayout, newResultsLayout);
        bodyLayout.add(wrapperLayout);
    }

    private void addConflictDetails()
    {
        VerticalLayout wrapperLayout = new VerticalLayout();
        wrapperLayout.setMargin(false);
        wrapperLayout.add(new H6("Remaining Conflict(s)"));

        // Show placeholder text if no conflicts remain (and no Details-component is displayed)
        if(newConflicts == null || newConflicts.isEmpty())
        {
            bodyLayout.setSizeFull();
            wrapperLayout.setSizeFull();
            this.setHeight("450px");

            Div noConflictsDiv = new Div();
            noConflictsDiv.add(new Text("None"));

            wrapperLayout.add(noConflictsDiv);
            wrapperLayout.setAlignSelf(Alignment.CENTER);
            wrapperLayout.setAlignSelf(Alignment.CENTER, noConflictsDiv);
            noConflictsDiv.getStyle().set("justify-content", "center");
        }
        else
            wrapperLayout.add(new ConflictAccordionComponent(null, newConflicts, true));

        bodyLayout.add(wrapperLayout);
    }

    private void createAnswerSetOutput(VerticalLayout layout, List<AnswerSet<?, ?>> displayedAnswerSets)
    {
        for (int i = 0; i < displayedAnswerSets.size(); i++)
        {
            AnswerSet<?, ?> currentAnswerSet = displayedAnswerSets.get(i);

            StringBuilder sb = new StringBuilder();
            sb.append(currentAnswerSet.getLiterals());
            sb.replace(0, 1, "");
            sb.replace(sb.length()-1, sb.length(), "");

            // Start counting at 1 because it is more intuitive for the user
            Div resultDiv = new Div();
            resultDiv.add(new Text(String.format("%s.Result: %s", i+1, sb.toString())));
            layout.add(resultDiv);
        }
    }
}
