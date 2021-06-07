package de.aspua.framework.Unit.Controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.aspua.framework.Controller.CausalRejectionController.ELPParser;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;

public class ELPParserTest {
    
    @Test
    public void ELPParser_parseProgram_EmptyParameter()
    {
        ELPParser parser = new ELPParser();
        ELPProgram program = parser.parseProgram(null, null);
        assertNull(program);

        program = parser.parseProgram("", null);
        assertNull(program);
    }

    @Test
    public void ELPParser_parseProgram_PositiveFact()
    {
        ELPParser parser = new ELPParser();

        ELPProgram program = parser.parseProgram("a.", null);
        ELPLiteral literal = new ELPLiteral(false,"a");
        ELPRule rule = new ELPRule(literal, null, null);
        assertTrue("A single literal without terms wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("A single literal without terms was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));

        program = parser.parseProgram("a(b).", null);
        literal = new ELPLiteral(false, "a", "b");
        rule = new ELPRule(literal, null, null);
        assertTrue("A single literal with one term wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("A single literal with one term was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));

        program = parser.parseProgram("a(b, c, d).", null);
        literal = new ELPLiteral(false, "a", "b", "c", "d");
        rule = new ELPRule(literal, null, null);
        assertTrue("A single literal with several terms wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("A single literal with several terms was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));
    }

    @Test
    public void ELPParser_parseProgram_NegatedFact()
    {
        ELPParser parser = new ELPParser();

        ELPProgram program = parser.parseProgram("-a.", null);
        ELPLiteral literal = new ELPLiteral(true,"a");
        ELPRule rule = new ELPRule(literal, null, null);
        assertTrue("A single negated literal without terms wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("A single negated literal without terms was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));

        program = parser.parseProgram("-a(b).", null);
        literal = new ELPLiteral(true, "a", "b");
        rule = new ELPRule(literal, null, null);
        assertTrue("A single negated literal with one term wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("A single negated literal with one term was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));

        program = parser.parseProgram("-a(b, c, d).", null);
        literal = new ELPLiteral(true, "a", "b", "c", "d");
        rule = new ELPRule(literal, null, null);
        assertTrue("A single negated literal with several terms wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("A single negated literal with several terms was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));
    }

    @Test
    public void ELPParser_parseProgram_PositiveRule()
    {
        ELPParser parser = new ELPParser();

        ELPProgram program = parser.parseProgram("a(c) :- -b(d, e).", null);
        ELPLiteral literal1 = new ELPLiteral(false, "a", "c");
        ELPLiteral literal2 = new ELPLiteral(true, "b", "d", "e");
        List<ELPLiteral> posBody = new ArrayList<>();
        posBody.add(literal2);
        ELPRule rule = new ELPRule(literal1, posBody, null);
        assertTrue("The rule 'a(c) :- -b(d, e).' wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("The rule 'a(c) :- -b(d, e).' was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));
    }

    @Test
    public void ELPParser_parseProgram_RuleWithDefaultNegation()
    {
        ELPParser parser = new ELPParser();

        ELPProgram program = parser.parseProgram("a(c, d) :- not -b(e).", null);
        ELPLiteral literal1 = new ELPLiteral(false,"a", "c", "d");
        ELPLiteral literal2 = new ELPLiteral(true, "b", "e");
        List<ELPLiteral> negBody = new ArrayList<>();
        negBody.add(literal2);
        ELPRule rule = new ELPRule(literal1, null, negBody);
        assertTrue("The rule 'a(c, d) :- not -b(e).' wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("The rule 'a(c, d) :- not -b(e).' was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));

        program = parser.parseProgram("a :- not -b, c(x, y), not d(z), -e.", null);
        literal1 = new ELPLiteral(false, "a");
        literal2 = new ELPLiteral(true, "b");
        ELPLiteral literal3 = new ELPLiteral(false, "c", "x", "y");
        ELPLiteral literal4 = new ELPLiteral(false, "d", "z");
        ELPLiteral literal5 = new ELPLiteral(true, "e");
        List<ELPLiteral> posBody = new ArrayList<>();
        posBody.add(literal3);
        posBody.add(literal5);

        negBody = new ArrayList<>();
        negBody.add(literal2);
        negBody.add(literal4);

        rule = new ELPRule(literal1, posBody, negBody);
        assertTrue("The rule 'a :- not -b, c(x, y), not d(z), -e.' wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("The rule 'a :- not -b, c(x, y), not d(z), -e.' was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));
    }

    @Test
    public void ELPParser_parseProgram_Constraint()
    {
        ELPParser parser = new ELPParser();

        ELPProgram program = parser.parseProgram(" :- a.", null);
        ELPLiteral literal = new ELPLiteral(false,"a");
        List<ELPLiteral> body = new ArrayList<>();
        body.add(literal);
        ELPRule rule = new ELPRule(null, body, null);
        assertTrue("The constraint ':- a.' wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("The constraint ':- a.' was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));

        program = parser.parseProgram(" :- not a.", null);
        literal = new ELPLiteral(false,"a");
        body = new ArrayList<>();
        body.add(literal);
        rule = new ELPRule(null, null, body);
        assertTrue("The constraint ':- not a.' wasn't parsed as a single rule.", program.getRuleSet().size() == 1);
        assertTrue("The constraint ':- not a.' was parsed incorrectly.", rule.equals(program.getRuleSet().get(0)));
    }

    @Test
    public void ELPParser_parseProgram_MultipleRules()
    {
        ELPParser parser = new ELPParser();

        String programString =  "a." + System.lineSeparator()
                            +   "-b :- c, a." + System.lineSeparator()
                            +   "d(x, y) :- not e(z).";

        ELPLiteral literal1 = new ELPLiteral(false,"a");
        ELPLiteral literal2 = new ELPLiteral(true,"b");
        ELPLiteral literal3 = new ELPLiteral(false,"c");
        ELPLiteral literal4 = new ELPLiteral(false, "d", "x", "y");
        ELPLiteral literal5 = new ELPLiteral(false, "e", "z");

        List<ELPRule> rules = new ArrayList<>();
        rules.add(new ELPRule(literal1, null, null));

        List<ELPLiteral> body = new ArrayList<>();
        body.add(literal3);
        body.add(literal1);
        rules.add(new ELPRule(literal2, body, null));

        body = new ArrayList<>();
        body.add(literal5);
        rules.add(new ELPRule(literal4, null, body));

        ELPProgram actualProgram = parser.parseProgram(programString, null);
        ELPProgram expectedProgram = new ELPProgram(rules);
        assertEquals(expectedProgram.getRuleSet(), actualProgram.getRuleSet());
    }

    @Test
    public void ELPParser_parseProgram_MultipleRulesWithComments()
    {
        ELPParser parser = new ELPParser();

        String programString =  "a. %test" + System.lineSeparator()
                            +   "%-b :- c, a." + System.lineSeparator()
                            +   "d(x, y) :- not e(z).";

        ELPLiteral literal1 = new ELPLiteral(false, "a");
        ELPLiteral literal2 = new ELPLiteral(false, "d", "x", "y");
        ELPLiteral literal3 = new ELPLiteral(false, "e", "z");

        List<ELPRule> rules = new ArrayList<>();
        rules.add(new ELPRule(literal1, null, null));

        List<ELPLiteral> body = new ArrayList<>();
        body.add(literal3);
        rules.add(new ELPRule(literal2, null, body));

        ELPProgram actualProgram = parser.parseProgram(programString, null);
        ELPProgram expectedProgram = new ELPProgram(rules);
        assertEquals(expectedProgram.getRuleSet(), actualProgram.getRuleSet());
    }

    @Test
    public void ELPParser_parseProgram_InvalidSyntax()
    {
        ELPParser parser = new ELPParser();

        List<String> invalidProgramStrings = new ArrayList<>();
        invalidProgramStrings.add("a");
        invalidProgramStrings.add("a :-.");
        invalidProgramStrings.add("not a.");
        invalidProgramStrings.add("a(()).");
        invalidProgramStrings.add("(a).");
        invalidProgramStrings.add("a(b, c.");

        for (String invalidProgramString : invalidProgramStrings)
        {
            ELPProgram program = parser.parseProgram(invalidProgramString, null);
            assertNull(program);
        }
    }

    @Test
    public void ELPParser_parseProgram_ProgramName()
    {
        ELPParser parser = new ELPParser();

        ELPProgram program = parser.parseProgram("a.", "test");
        assertEquals("test", program.getProgramName());
    }

    @Test
    public void ELPParser_parseAnswerSets_EmptyParameter()
    {
        ELPParser parser = new ELPParser();

        List<String> answerSets = new ArrayList<>();
        answerSets.add("");
        answerSets.add(null);

        List<AnswerSet<?,?>> parsedAnswerSets = parser.parseAnswerSets(answerSets);
        assertEquals(1, parsedAnswerSets.size());
    }

    @Test
    public void ELPParser_parseAnswerSets_MultipleAnswerSets()
    {
        ELPParser parser = new ELPParser();

        List<String> answerSets = new ArrayList<>();
        answerSets.add("a");
        answerSets.add("-d, a(b,c), e");
        answerSets.add("");

        List<AnswerSet<?,?>> expectedAnswerSets = new ArrayList<>();
        List<ELPLiteral> literals = new ArrayList<>();
        literals.add(new ELPLiteral(false, "a"));
        expectedAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(literals));

        literals = new ArrayList<>();
        literals.add(new ELPLiteral(true, "d"));
        literals.add(new ELPLiteral(false, "a", "b", "c"));
        literals.add(new ELPLiteral(false, "e"));
        expectedAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(literals));
        expectedAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(new ArrayList<>()));

        List<AnswerSet<?,?>> parsedAnswerSets = parser.parseAnswerSets(answerSets);
        assertEquals(expectedAnswerSets.size(), parsedAnswerSets.size());

        for (int i = 0; i < expectedAnswerSets.size(); i++)
        {
            assertEquals(expectedAnswerSets.get(i).getLiterals(), parsedAnswerSets.get(i).getLiterals());
        }
    }
}
