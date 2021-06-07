package de.aspua.framework.Controller.CausalRejectionController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ControllerInterfaces.IParserController;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPAtom;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.Constants;

/**
 * Parses strings according to the syntax of extended logic programs (ELPs) in the notation of Prolog.
 * Comments within the String are tolerated if they are marked with '%'. All following characters in the same line will be ignored.
 * All terms are parsed as constants of the corresponding atom.
 * The following example describes a parsable String (comments included):
 * <pre>
 *                              % example comment
 * a(x).                        % fact
 * a :- not b(y, z), c.         % ELP-rule (negative body literal first)
 * a :- b, not c.               % ELP-rule (positiv body literal first)
 * :- a(x, y, z), not b.        % constraint
 * a. :- b, c(x). d :- not e.   % Multiple valid rules in the same line
 * </pre>
 */
public class ELPParser implements IParserController
{
    private static Logger LOGGER = LoggerFactory.getLogger(ELPParser.class);

    /**
     * Translates a given String to an {@link ELPProgram}-object.
     * @return An {@link ELPProgram}-object which corresponds to the given string.
	 * Returns null if the given program string doesn't follow the expected syntax.
     */
    @Override
    public ELPProgram parseProgram(String programString, String programName)
    {
        if(programString == null || programString.isEmpty())
        {
            LOGGER.warn("Parsing failed! The given string was null or empty, and therefore couldn't be parsed!");
            return null;
        }

        ELPProgram parsedProgram = readInProgram(programString);
        if(parsedProgram == null || parsedProgram.getRuleSet().isEmpty())
        {
            LOGGER.warn("Parsing failed!");
            return null;
        }
        else
        {
            parsedProgram.setProgramName(programName);
            LOGGER.info("Parsing successfull!");
            return parsedProgram;
        }
    }

    /**
     * Parses each String in the given list to an {@link AnswerSet}-object (Typed as <{@link ELPRule}, {@link ELPLiteral}>).
     */
    @Override
    public List<AnswerSet<?, ?>> parseAnswerSets(List<String> answerSetStrings)
    {
        if(answerSetStrings == null)
            return null;

        List<AnswerSet<?, ?>> answerSets = new ArrayList<>();
        answerSetStrings.removeAll(Collections.singletonList(null));
            
        for (String currentString : answerSetStrings)
        {
            if("".equals(currentString))
            {
                answerSets.add(new AnswerSet<>(new ArrayList<>()));
                continue;
            }

            List<String> literalStrings = parseRegex(currentString, Constants.REGEX_LITERAL);
            Collections.sort(literalStrings);
            
            List<ELPLiteral> literals = new ArrayList<>();
            literalStrings.stream().forEach(x -> literals.add(this.parseLiteral(x)));

            literals.removeAll(Collections.singletonList(null));

            if(!literals.isEmpty())
                answerSets.add(new AnswerSet<>(literals));
        }

        return answerSets;
    }

    /**
     * Parses a given String to a {@link ELPProgram}-object.
     * @param programString String containing all rules which are supposed to be parsed.
     * @return A parsed {@link ELPProgram}-object with a ruleset according to the given string.
     * Returns null if the given String contains invalid syntax.
     */
    private ELPProgram readInProgram(String programString)
    {
        List<ELPRule> ruleSet = new ArrayList<>();
        Scanner sc = new Scanner(programString);

        while(sc.hasNext())
        {
            String[] formattedLines = formatInputString(sc);

            // Iterate over each possible rule of the current line
            for (String currentRuleString : formattedLines)
            {
                // The current line might be empty if it was a comment.
                if(!currentRuleString.isEmpty())
                {
                    String head = "";
                    String body = "";
                    boolean containsBody = currentRuleString.contains(":-");
                    
                    // Split body from head
                    String[] headBody = currentRuleString.split(":-");
                    if(headBody.length > 2)
                    {
                        LOGGER.warn("The line '{}' doesn't fit the expected syntax (multiple ':-')!", currentRuleString);
                        return null;
                    }

                    // Check if the rule is a fact. If not, assign body
                    if(headBody.length > 1)
                    body = headBody[1].trim();
                    
                    head = headBody[0].trim();
                    if(parseRegex(head, Constants.REGEX_LITERAL).size() > 1)
                    {
                        LOGGER.warn("An extended logic Program only allows max. 1 literal in its head, but more were detected! " + head);
                        return null;
                    }
                    if(!parseRegex(head, Constants.REGEX_DEFNEGLITERAL).isEmpty())
                    {
                        LOGGER.warn("An extended logic Program does not allow Default-Negation in its head! " + head);
                        return null;
                    }

                    List<String> posBody = parseRegex(body, Constants.REGEX_LITERAL);
                    List<String> negBody = parseRegex(body, Constants.REGEX_DEFNEGLITERAL);

                    if(containsBody && (posBody.isEmpty() & negBody.isEmpty()))
                    {
                        LOGGER.warn("The body of the rule '{}' doesn't fit the expected syntax!", currentRuleString);
                        return null;
                    }

                    // Translate literal strings into model-objects
                    ELPLiteral headLiteral = parseLiteral(head);
                    List<ELPLiteral> posBodyLiterals = parseLiterals(posBody);
                    List<ELPLiteral> negBodyLiterals = parseLiterals(negBody);
                    
                    // Validate that no empty rule would be created
                    if(headLiteral != null || !posBodyLiterals.isEmpty() || !negBodyLiterals.isEmpty())
                    {
                        ruleSet.add(new ELPRule(headLiteral, posBodyLiterals, negBodyLiterals));
                        ruleSet.get(ruleSet.size()-1).setLabelID(ruleSet.size()-1);
                    }
                    else
                    {
                        LOGGER.warn("Could not parse the literals of the rule '{}'. Because empty rules are not allowed, the Rule will be skipped.", currentRuleString);
                    }
                }
            }
        }

        if(!ruleSet.isEmpty())
            return new ELPProgram(ruleSet);
        else
        {
            LOGGER.warn("Could not parse any rule from Input-File!");
            return null;
        }
    }

    /**
     * Parses a given String to a {@link ELPLiteral}-object by using regex.
     * @param literalString String which is parsed to a {@link ELPLiteral}-object
     * @return Parsed {@link ELPLiteral}-object.
     * Returns null if the String doesn't match the used regex (and therefore is no valid literal)
     * @see #parseLiterals(List)
     */
    public ELPLiteral parseLiteral(String literalString)
    {
        literalString = literalString.replaceAll("\\s+", " ");
        if(literalString.matches(Constants.REGEX_LITERAL))
        {
            Boolean isNegated = literalString.startsWith("-");
            if(isNegated)
                literalString = literalString.substring(1, literalString.length());

            String predicate = this.parseRegex(literalString, Constants.REGEX_PREDICATE).get(0);
            literalString = literalString.replace(predicate, "");
            
            List<String> terms = this.parseRegex(literalString, Constants.REGEX_CONSTANT_VARIABLE);
            ASPAtom atom = new ASPAtom(predicate, terms.toArray(new String[terms.size()]));

            return new ELPLiteral(isNegated, atom);
        }

        return null;
    }

    /**
     * Wrapper to parse a list of Strings to a list of {@link ELPLiteral}-objects.
     * @param literals List of strings which should be parsed to {@link ELPLiteral}-objects
     * @return List of parsed {@link ELPLiteral}-objects
     * @see #parseLiteral(String)
     */
    private List<ELPLiteral> parseLiterals(List<String> literals)
    {
        List<ELPLiteral> elpLiterals = new ArrayList<>();

        for (String currentLiteral : literals)
        {
            elpLiterals.add(this.parseLiteral(currentLiteral));
        }

        elpLiterals.removeAll(Collections.singletonList(null));
        return elpLiterals;
    }

    /**
     * Finds all matches for the given regex in the given String.
     * @param text String which is mapped against the regex
     * @param regex Regex criteria which is used to find matching literals
     * @return List of literal-strings that match the given regex
     */
    private List<String> parseRegex(String text, String regex)
    {
        List<String> literals = new ArrayList<>();
        Matcher matcher = Pattern.compile(regex).matcher(text);

        while(matcher.find())
        {
            String literal = matcher.group(0);

            if(regex == Constants.REGEX_DEFNEGLITERAL)
                literal = literal.substring(4);

            if(regex == Constants.REGEX_LITERAL && "not".equals(literal))
                matcher.find();
            else
                literals.add(literal.trim());
        }

        return literals;
    }

    /**
     * Read lines until a full rule was found (indicated by '.'). Removes all comments starting with '%'.
     * @param sc Scanner-object is needed to start at currently investigated input-line
     * @return Array of possible rules
     */
    private String[] formatInputString(Scanner sc)
    {
        String currentLine = "";

        do
        {
            currentLine += sc.nextLine();

            // Check if line includes a comment
            if(currentLine.contains("%"))
            {
                // If line includes a comment, delete it from the input String
                int commentBegin = currentLine.indexOf("%");
                currentLine = currentLine.substring(0, commentBegin).trim();
            }

        }while(!currentLine.endsWith(".") && sc.hasNext());

        if(!currentLine.endsWith("."))
            return new String[0];
        else
            return currentLine.split("\\.");
    }
}