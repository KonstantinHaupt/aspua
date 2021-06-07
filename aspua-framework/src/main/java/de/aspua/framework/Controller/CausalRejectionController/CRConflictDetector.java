package de.aspua.framework.Controller.CausalRejectionController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ControllerInterfaces.IConflictDetectionController;
import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPAtom;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPRule;

/**
 * Implements a Conflict-Detection as seen in 'Towards Interactive Conflict Resolution in ASP Programs' by Thevapalan and Kern-Isberner.
 */
public class CRConflictDetector implements IConflictDetectionController
{
	private static Logger LOGGER = LoggerFactory.getLogger(CRConflictDetector.class);

	private ELPProgram initialProgram;
	private ELPProgram newProgram;
	private ELPProgram modifiedUpdateProgram;

	private final String id = UUID.randomUUID().toString().substring(0, 8);
	private final String rejPred = "rej_" + id;
	private final String rejCausePred = "rej_cause_" + id;
	private final String activePred = "active_" + id;

	/**
	 * Generates a modified update programm (MUP) which uses the meta-literals 'rej(.)'', 'rej_cause(.,.)' and 'active(.)' to detect conflicts.
	 * @return Modified update program for the given update sequence
	 */
	@Override
	public ASPProgram<?, ?> computeConflictDetectionProgram(List<ASPProgram<?, ?>> updateSequence)
	{
		if(updateSequence.size() != 2)
		{
			LOGGER.warn("The current Conflict-Detection only works for Update-Sequences of size 2. The Program-Modification will be aborted.");
			return null;
		}

		modifiedUpdateProgram = new ELPProgram();
		initialProgram = (ELPProgram) updateSequence.get(0).createNewInstance();
		newProgram = (ELPProgram) updateSequence.get(1).createNewInstance();

		this.addModifiedRules(true);
		this.addModifiedRules(false);
		this.addRejectionCauseRules();
		this.addTranslationRules();

		for (ELPRule currentRule : modifiedUpdateProgram.getRuleSet())
			currentRule.setLabelID(modifiedUpdateProgram.getRuleSet().indexOf(currentRule));

		return modifiedUpdateProgram;
	}

	/**
	 * Detects conflicts from the answer sets of an MUP by searching for 'rej_cause(.,.)'-literals.
	 * @param answerSets Answer sets of an MUP
	 */
	@Override
	public List<Conflict> detectConflicts(List<ASPProgram<?, ?>> updateSequence, List<AnswerSet<?, ?>> answerSets)
	{
		List<Conflict> conflicts = new ArrayList<>();

		for (AnswerSet<?, ?> currentAnswerSet : answerSets)
		{
			for (ASPLiteral<?> currentLiteral : currentAnswerSet.getLiterals())
			{
				if(rejCausePred.equals(currentLiteral.getAtom().getPredicate()))
				{
					List<String> constants = currentLiteral.getAtom().getConstants();
					if(constants.size() != 2)
						LOGGER.warn("A rej_cause(*,*)-Literal did contain more or less than 2 constants (e.g. IDs of conflicting rules).", System.lineSeparator(),
						"As the Syntax does not match the Conflict-Detection from modified Update-Programs, the conflict won't be considered!");
					else
					{	
						boolean alreadyExists = false;
						for (Conflict currentConflict : conflicts)
						{
							alreadyExists = currentConflict.getConflictingRules().stream()
								.allMatch(rule -> constants.contains(rule.getID()));

							if(alreadyExists)
							{
								currentConflict.getInvolvedAnwerSets().addAll(this.createUpdateAnswerSets(updateSequence, currentAnswerSet));
								break;
							}
						}
						
						if(!alreadyExists)
						{
							AnswerSet<?, ?>[] conflictAnswerSets = new AnswerSet<?, ?>[] { currentAnswerSet };

							List<ASPRule<?>> conflictingRules = new ArrayList<>();
							for (int i = 0; i < constants.size(); i++)
							{
								ASPRule<?> conflictRule = updateSequence.get(i).getRule(constants.get(i)).createNewInstance();
								conflictingRules.add(conflictRule);
							}
							
							conflicts.add(new Conflict(conflictingRules, this.createUpdateAnswerSets(updateSequence, conflictAnswerSets)));
						}
					}
				}	
			}
		}

		return conflicts;
	}

	/**
	 * Removes meta-literals from answer sets to provide update answer sets.
	 * @param updateSequence Update sequence which provides the set of atoms which are allowed in the update answer sets
	 * @param answerSets Answer sets with meta literals which are filtered
	 * @return A list of {@link AnswerSet}-Objects which contain no meta literals
	 */
	private List<AnswerSet<?, ?>> createUpdateAnswerSets(List<ASPProgram<?, ?>> updateSequence, AnswerSet<?, ?>... answerSets)
	{
		List<AnswerSet<?, ?>> updateAnswerSets = new ArrayList<>();
        HashSet<ELPLiteral> nonMetaLiterals = new HashSet<>();

		for (ASPProgram<?, ?> program : updateSequence)
			nonMetaLiterals.addAll(((ELPProgram) program).getLiteralBase().keySet());

        for (AnswerSet<?, ?> currentAnswerSet : answerSets)
        {
            List<ELPLiteral> answerSetLiterals = new ArrayList<ELPLiteral>();
            for (ASPLiteral<?> elpLiteral : currentAnswerSet.getLiterals())
            {
                answerSetLiterals.add((ELPLiteral) elpLiteral);
            }

            answerSetLiterals.removeIf(x -> !nonMetaLiterals.contains(x));
            updateAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(answerSetLiterals));
        }

        return updateAnswerSets;
	}

	/**
	 * Adds all rules of the form (i), (m-ii-a) & (m-ii-b) to the modified update program.
	 * @param initial Determines if the initial ASP-Program of the update sequence is inspected (i.e. if (m-ii-a) or (m-ii-b) is used)
	 */
	private void addModifiedRules(Boolean initial)
	{
		String suffix = initial ? "_1" : "_2";
		ELPProgram translatedProgram = initial ? initialProgram : newProgram;

		for(ELPRule currentRule : translatedProgram.getRuleSet())
		{
			// (i)
			if(currentRule.isContraint())
			{
				ELPRule newRule = currentRule.createNewInstance();
				newRule.setID(null);
				modifiedUpdateProgram.addRule(newRule);
				continue;
			}

			ELPRule newRule = currentRule.createNewInstance();
			newRule.setID(null);
			ELPLiteral newHead = newRule.getHead().get(0);
			newHead.getAtom().setPredicate(newHead.getAtom().getPredicate() + suffix);
			modifiedUpdateProgram.addRule(newRule);

			// (m-ii-a)
			if(initial)
			{
				ELPLiteral rejLiteral = this.buildMetaLiteral(rejPred, newRule.getID());
				newRule.getNegBody().add(rejLiteral);
			}
			// (m-ii-b)
			else
			{
				ELPRule activeRule = currentRule.createNewInstance();
				activeRule.setID(null);
				ELPLiteral activeLiteral = this.buildMetaLiteral(activePred, newRule.getID());
				List<ELPLiteral> activeHead = new ArrayList<ELPLiteral>();
				activeHead.add(activeLiteral);
				activeRule.setHead(activeHead);

				modifiedUpdateProgram.addRule(activeRule);
			}
		}
	}

	/**
	 * Adds all rules of the form (m-iii) to the modified update program.
	 */
	private void addRejectionCauseRules()
	{
		// (m-iii)
		for (ELPRule initialRule : initialProgram.getRuleSet())
		{
			if(initialRule.isContraint())
				continue;

			for (ELPRule newRule : newProgram.getRuleSet())
			{
				if(newRule.isContraint())
					continue;

				ELPLiteral initialHead = initialRule.getHead().get(0);
				ELPLiteral newHead = newRule.getHead().get(0);

				if(initialHead.getAtom().equals(newHead.getAtom())
				&& initialHead.isNegated() != newHead.isNegated())
				{
					// Build first rule
					ELPRule rejCauseRule = initialRule.createNewInstance();
					rejCauseRule.setID(null);

					ELPLiteral rejCauseLiteral = this.buildMetaLiteral(rejCausePred, initialRule.getID(), newRule.getID());
					List<ELPLiteral> rejCauseHead = new ArrayList<>();
					rejCauseHead.add(rejCauseLiteral);
					rejCauseRule.setHead(rejCauseHead);

					ELPLiteral activeLiteral = this.buildMetaLiteral(activePred, newRule.getID());
					rejCauseRule.getBody().add(activeLiteral);
					
					modifiedUpdateProgram.addRule(rejCauseRule);

					// Build second rule
					ELPLiteral rejLiteral = this.buildMetaLiteral(rejPred, initialRule.getID());
					List<ELPLiteral> rejBody = new ArrayList<>();
					rejBody.add(rejCauseLiteral.createNewInstance());
					ELPRule rejRule = new ELPRule(rejLiteral, rejBody, null);

					modifiedUpdateProgram.addRule(rejRule);
				}
			}
		}
	}

	/**
	 * Adds all translation-rules of the form (m-iv) to the modified update program.
	 */
	private void addTranslationRules()
	{
		// (m-iv)
		Set<ELPLiteral> literalSets = new HashSet<>(initialProgram.getLiteralBase().keySet());
		literalSets.addAll(newProgram.getLiteralBase().keySet());
		List<ELPLiteral> allLiterals = new ArrayList<>(literalSets);

		for (ELPLiteral currentLiteral : allLiterals)
		{
			ELPLiteral firstLiteral = currentLiteral.createNewInstance();
			ELPLiteral secondLiteral = currentLiteral.createNewInstance();
			firstLiteral.getAtom().setPredicate(firstLiteral.getAtom().getPredicate() + "_1");
			secondLiteral.getAtom().setPredicate(secondLiteral.getAtom().getPredicate() + "_2");

			List<ELPLiteral> body = new ArrayList<>();
			body.add(secondLiteral);
			ELPRule second2First = new ELPRule(firstLiteral, body, null);
			body = new ArrayList<>();
			body.add(firstLiteral.createNewInstance());
			ELPRule first2Zero = new ELPRule(currentLiteral.createNewInstance(), body, null);
			
			modifiedUpdateProgram.addRule(second2First);
			modifiedUpdateProgram.addRule(first2Zero);
		}
	}

	private ELPLiteral buildMetaLiteral(String predicate, String... terms)
	{
		ASPAtom atom = new ASPAtom(predicate, terms);
		return new ELPLiteral(false, atom);
	}
}