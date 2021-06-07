package de.aspua.gui.UI.CustomComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Custom component to show different details in the drawer, depending on the current view
 */
public class DrawerContentComponent extends VerticalLayout
{
    private VerticalLayout helpLayout;
    private List<Details> helpDetails;
    private HashMap<String, Div> uploadQuestions;
    private HashMap<String, Div> updateQuestions;
    private HashMap<String, Div> conflictQuestions;
    private HashMap<String, Div> mergeQuestions;

    public DrawerContentComponent()
    {
        helpLayout = new VerticalLayout(new H3("Help"));
        helpLayout.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        helpDetails = new ArrayList<>();

        this.add(helpLayout);
        this.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);

        this.generateUploadQuestions();
        this.generateUpdateQuestions();
        this.generateConflictQuestions();
        this.generateMergeQuestions();
    }

    public void setContentForView(String viewName)
    {
        helpLayout.remove(helpDetails.toArray(new Details[helpDetails.size()]));
        helpDetails = new ArrayList<>();

        HashMap<String, Div> chosenQuestions;

        switch (viewName)
        {
            case "Upload":
                chosenQuestions = uploadQuestions;
                break;
            case "Update":
                chosenQuestions = updateQuestions;
                break;
            case "Conflict":
                chosenQuestions = conflictQuestions;
                break;
            case "Merge":
                chosenQuestions = mergeQuestions;
                break;
            default:
                chosenQuestions = new LinkedHashMap<>();
                break;
        }

        for(String questionString : chosenQuestions.keySet())
        {
            Div div = chosenQuestions.get(questionString);
            // div.getStyle().set("text-align", "justify");

            Details details = new Details(questionString, div);
            details.addThemeVariants(DetailsVariant.FILLED);
            helpDetails.add(details);
        }
        helpLayout.add(helpDetails.toArray(new Details[helpDetails.size()]));

        if(!helpDetails.isEmpty())
            helpDetails.get(0).setOpened(true);
    }

    private void generateUploadQuestions()
    {
        uploadQuestions = new LinkedHashMap<>();

        // What is ASPUA
        Div div1 = new Div();
        div1.add("ASPUA stands for 'Answer Set Programming Update Application' and provides a tool for updating knowledge bases (i.e. ASP-Programs). The update procedure is based on the work of Thevapalan and Kern-Isberner in ");
        Anchor paperAnchor = new Anchor("https://nmr2020.dc.uba.ar/WorkshopNotes.pdf", "Towards Interactive Conflict Resolution in ASP Programs");
        paperAnchor.setTarget("_blank");
        div1.add(paperAnchor);
        div1.add(". For further information about ASP, please refer to common sources, e.g. ");
        Anchor wikiAnchor = new Anchor("https://en.wikipedia.org/wiki/Answer_set_programming", "Wikipedia.");
        wikiAnchor.setTarget("_blank");
        div1.add(wikiAnchor);
        uploadQuestions.put("What is ASPUA?", div1);

        Div div2 = new Div();
        div2.add("Each row in the displayed table represents an ASP-rule. A literal consists of a 'predicate' and optional 'terms' and is written as 'predicate(term1,term2,...)'' or 'predicate'. Negated literals start with the symbol '-'. The column 'Prerequisites' corresponds to positive bodyliterals of an ASP-rule. Those literals have to be true in order to derive the literal in the 'Conclusion'-column. The literal in the 'Conclusion'-column corresponds to the head literal of an ASP-Rule. All literals in the column 'Justifications' represent literals with 'negation as failure' (i.e. negative body literals of an ASP-rule). Those literals are not allowed to be true in order to draw the conclusion.");
        div2.add(" For further information about ASP please refer to common sources, e.g. ");
        Anchor wikiAnchor2 = new Anchor("https://en.wikipedia.org/wiki/Answer_set_programming", "Wikipedia.");
        div2.add(wikiAnchor2);
        uploadQuestions.put("Table content", div2);

        Div div3 = new Div();
        div3.add("You can choose a knowledge base either by selecting an exisiting one from the dropdown-list or uploading your own knowledge base (i.e. ASP-Program). The chosen knowledge base will be updated in the next steps of the update procedure.");
        uploadQuestions.put("Choosing a knowledge base", div3);

        Div div4 = new Div();
        div4.add("You can upload your own knowledge base by clicking the 'Upload File'-Button and choosing a textfile (.txt) from your local device. The content has to match the syntax of an extended logic program in the programming language Prolog. You can get examples of the expected format by downloading one of the provided knowledge bases. Comments within a uploaded file are tolerated if they start with the symbol '%'.");
        uploadQuestions.put("Upload new knowledge base", div4);
    }

    private void generateUpdateQuestions()
    {
        updateQuestions = new LinkedHashMap<>();

        Div div1 = new Div();
        div1.add("In this step, you can model new knowledge which is supposed to be merged into the initially chosen knowledge base. If you finished the modeling process, inconsistencies (i.e. conflicts) between the older and newer knowledge are detected.");
        updateQuestions.put("Model new knowledge", div1);

        Div div2 = new Div();
        div2.add("You can model a new rule by entering the literals in the textfields of the corresponding column. You can copy the literals of an exisiting rule to the textfields by selecting a row from a table. The ID of a new rule can be changed, but has to be a positive number and unique. The rule can be saved by clicking the 'Add'-Button.");
        updateQuestions.put("Model a rule", div2);

        Div div3 = new Div();
        div3.add("A new literal consists of a 'predicate' and a list of 'terms'. The 'predicate' is mandatory, while terms are optional. A literal has the format 'predicate(term1, term2,...)'. If the literal doesn't contain terms, you can simply write 'predicate'. Here are some examples of INVALID literals, which will not be accepted: (predicate) | predicate(term1 | $%§&predicate | predicate((term1), term1)");
        updateQuestions.put("Literal syntax", div3);

        Div div4 = new Div();
        div4.add("An added rule can be deleted by selecting the rule in the grid, right-click the row and choosing the option 'remove'.");
        updateQuestions.put("Delete an added rule", div4);

    }

    private void generateConflictQuestions()
    {
        conflictQuestions = new LinkedHashMap<>();

        Div div1 = new Div();
        div1.add("A conflict appears if two complementary literals (i.e. 'literal' and '-literal') would appear in the same result. Those conflicts have to be resolved in order to maintain a consistent knowledge base which doesn't infer contradictory knowledge.");
        conflictQuestions.put("What is a conflict?", div1);

        Div div2 = new Div();
        div2.add("The update sequence includes all rules from the initial knowledge base as well as the new knowledge, which was modeled in the 2. step. If you made additional changes while solving conflicts, those changes are included as well. If a rule was modified, only the newest version of the rule is displayed. Deleted rules won't be included in the final, updated knowledge base.");
        conflictQuestions.put("What is an update sequence?", div2);

        Div div3 = new Div();
        div3.add("The values represent measures which rate the changes which will be caused by the solution. As those values only describe syntactical properties of a solution, they are not supposed to determine the applied solution.");
        conflictQuestions.put("What are 'changes in results/rules'?", div3);

        Div div4 = new Div();
        div4.add("The application computes possible solutions for each conflict. The operations which would be applied by a solution are displayed in the table on the right. A operation may contain possible variants, which can be replaced by one another, such that the conflict still be solved. You can inspect possible variations by clicking the '>'-symbol on the left side of a row and choose a variant by double-clicking the corresponding row. The solution can be applied by clicking the 'Solve'-button.");
        conflictQuestions.put("Choose a solution", div4);

        Div div5 = new Div();
        div5.add("You can create your own solution by choosing the entry 'Custom Solution' from the solution-list. You can customize existing solutions by selecting all solutions of interest while pressing the CTRL- or ALT-key and clicking the 'Customize'-button. The measures for a custom solution are computed if you click the 'Preview'-button.");
        conflictQuestions.put("Customize a solution", div5);
    }

    private void generateMergeQuestions()
    {
        mergeQuestions = new LinkedHashMap<>();

        Div div1 = new Div();
        div1.add("You can save the updated knowledge base within the application by saving it as a new knowledge base ('Save as new KB') or overwriting the initial knowledge base ('Overwrite existing KB'). In addition, you can download the updated knowledge base as a textfile. At least one option should be executed in order to save the updated knowledge base. Otherwise, all changes will be discarded and the initial knowledge base remains unchanged.");
        mergeQuestions.put("Save knowledge base", div1);

        Div div2 = new Div();
        div2.add("If the checkbox 'Show applied operations' is checked, all applied operations throughout the update process are highlighted. This includes all rules which were added as new knowledge, as well as all operations which were applied due to conflict resolutions. For modified rules, only the final (newest) version is displayed.");
        mergeQuestions.put("Show applied operations", div2);

        Div div3 = new Div();
        div3.add("If you notice that certain rules within the final knowledge base are incorrect, you can navigate to a previous step of the update process by clicking a entry in the navigation bar.");
        mergeQuestions.put("Reset changes", div3);

    }
}