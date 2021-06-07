package de.aspua.framework.Controller.Measures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ASPUAFrameworkAPI;
import de.aspua.framework.Controller.ControllerInterfaces.IMeasureController;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.SolutionMetaDataEnum;

/**
 * Provides a measure to determine how many literals in the answer sets of the update sequence would be changed by a applied solution.
 * Different answer sets are compared by their 'interpretation-distance', which represents the symmetric difference between two answer sets.
 * The Kuhn–Munkres assignment algorithm is used to find a minimal assigment between the current answer sets and the answer sets after a solution is applied
 * (minimal w.r.t. the 'interpretation-distance' between the answer sets).
 * @see HungarianAlgorithm
 */
public class AnswerSetMeasure implements IMeasureController
{
    private static Logger LOGGER = LoggerFactory.getLogger(AnswerSetMeasure.class);
    
    private ASPUAFrameworkAPI frameworkAPI;
    private List<ASPProgram<?, ?>> updateSequence;

    public AnswerSetMeasure(ASPUAFrameworkAPI frameworkAPI)
    {
        this.frameworkAPI = frameworkAPI;
    }

    /**
     * Computes the answer set measure for the given solution.
     * The computed measure is set in the metadata-Attribute of the given solution with the key {@link SolutionMetaDataEnum#MEASURE_ANSWERSETCHANGES}.
     * The measure is only computed for the currently chosen operations. If a operation-variant is chosen, the measure has to be applied once again.
     */
    @Override
    public void computeMeasure(Solution solution)
    {
        List<AnswerSet<?, ?>> currentAnswerSets = frameworkAPI.getCurrentAnswerSets();
        List<AnswerSet<?, ?>> oldUpdateAnswerSets = new ArrayList<>();
        
        for (AnswerSet<?,?> answerSet : currentAnswerSets)
            oldUpdateAnswerSets.add(this.computeUpdateAnswerSet(answerSet));
        
        this.getCurrentUpdateSequence();
        List<AnswerSet<?, ?>> newUpdateAnswerSets = getAnswerSetsAfterSolution(solution);
        int result = this.computeAnswerSetMeasure(oldUpdateAnswerSets, newUpdateAnswerSets);

        if(result >= 0)
            solution.addMetaData(SolutionMetaDataEnum.MEASURE_ANSWERSETCHANGES, result);
    }

    /**
     * Copies the update sequence of the framework to the local update sequence.
     */
    private void getCurrentUpdateSequence()
    {
        List<ASPProgram<?, ?>> originalUpdateSequence = frameworkAPI.getUpdateSequence();
        updateSequence = new ArrayList<>();

        for (ASPProgram<?,?> program : originalUpdateSequence)
            updateSequence.add(program.createNewInstance());
    }

    /**
     * Removes all meta-literals from the given answer set.
     * A literal is considered as a meta-literal, if it isn't included in the literal base of the update sequence.
     * @param answerSet Answer set with meta literals
     * @return A new instance of the given {@link AnswerSet}-object without meta literals
     */
    private AnswerSet<?, ?> computeUpdateAnswerSet(AnswerSet<?, ?> answerSet)
    {
        List<ASPProgram<?, ?>> originalUpdateSequence = frameworkAPI.getUpdateSequence();
        answerSet = answerSet.createNewInstance();
        HashSet<ASPLiteral<?>> nonMetaLiterals = new HashSet<>();

        for (ASPProgram<?, ?> program : originalUpdateSequence)
            nonMetaLiterals.addAll(program.getLiteralBase().keySet());

        answerSet.getLiterals().removeIf(x -> !nonMetaLiterals.contains(x));
        return answerSet;
    }

    /**
     * Computes the update answer sets for the update sequence in which the given solution would be applied.
     * @param solution Applied solution
     * @return List of update answer sets for the resulting update sequence after applying the solution
     */
    private List<AnswerSet<?, ?>> getAnswerSetsAfterSolution(Solution solution)
    {
        List<AnswerSet<?, ?>> newAnswerSets = frameworkAPI.previewSolutionAnswerSets(solution);
        List<AnswerSet<?, ?>> updateAnswerSets = new ArrayList<>();

        for (AnswerSet<?,?> answerSet : newAnswerSets)
            updateAnswerSets.add(this.computeUpdateAnswerSet(answerSet));

        return updateAnswerSets;
    }

    /**
     * Computes the answer set measure for the given answer sets.
     * 1. Step: Assign interpretation distances for each pair of answer sets.
     * 2. Step: Compute a minimal assignment using the hungarian algorithm based on the interpretation-distances.
     * 3. Step: Add up all interpretation-distances of assigned answer sets.
     * @param oldAnswerSets Update answer sets of the current update sequence
     * @param newAnswerSets Update answer sets after the solution is applied
     * @return The answer set measure (minimal sum of interpretation-distances for assigned answer sets)
     */
    private int computeAnswerSetMeasure(List<AnswerSet<?, ?>> oldAnswerSets, List<AnswerSet<?, ?>> newAnswerSets)
    {
        while(oldAnswerSets.size() < newAnswerSets.size())
            oldAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(null));

        while(oldAnswerSets.size() > newAnswerSets.size())
            newAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(null));

        // 1. Step
        int[][] distanceMatrix = new int[oldAnswerSets.size()][oldAnswerSets.size()];

        for (int i = 0; i < distanceMatrix.length; i++)
            for (int j = 0; j < distanceMatrix[i].length; j++)
                distanceMatrix[i][j] = this.computeInterpretationDistance(oldAnswerSets.get(i).getLiterals(), newAnswerSets.get(j).getLiterals());

        // Copy distanceMatrix for parameter-call of HungarianAlgorithm, as the matrix would be modified otherwise (call by reference)
        int[][] parameterMatrix = new int[oldAnswerSets.size()][oldAnswerSets.size()];
        for(int i=0; i<distanceMatrix.length; i++)
            for(int j=0; j<distanceMatrix[i].length; j++)
                parameterMatrix[i][j]=distanceMatrix[i][j];

        // 2. Step
        HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(parameterMatrix);
        int[][] assignment = hungarianAlgorithm.findOptimalAssignment();

        if(assignment.length == 0)
        {
            LOGGER.info("The answerset-measure couldn't be applied.");
            return -1;
        }

        // 3. Step
        int finalMeasure = 0;
        for (int i = 0; i < assignment.length; i++)
            finalMeasure += distanceMatrix[assignment[i][0]][assignment[i][1]];
        
        return finalMeasure;
    }

    /**
     * Computes the interpretation-distance (symmetric difference) between two lists of literals.
     * The parameter-lists use wildcards because the literal-lists of different {@link AnswerSet}-objects might not use the same class-type.
     * This is only necessary for type-saefty! Only invoke the the method with lists of {@link ASPLiteral}-objects!
     * @param first List of {@link ASPLiteral}-objects, which represent the literals of the first answer set
     * @param second List of {@link ASPLiteral}-objects, which represent the literals of the second answer set
     * @return The interpretation-distance between the given lists of literals
     * @see RuleMeasure
     */
    private int computeInterpretationDistance(List<?> first, List<?> second)
    {
        if(first.size() == 0)
            return second.size();

        if(second.size() == 0)
            return first.size();

        // Convert wildcard-type back to concrete Literal<?> type
        List<ASPLiteral<?>> castedFirst = new ArrayList<>();
        List<ASPLiteral<?>> castedSecond = new ArrayList<>();
        first.forEach(x -> castedFirst.add((ASPLiteral<?>) x));
        second.forEach(x -> castedSecond.add((ASPLiteral<?>) x));

        int distance = 0;
        //Compute difference between first and second list of literals in answer set
        List<ASPLiteral<?>> firstLiteralList = new ArrayList<>(castedFirst);
        List<ASPLiteral<?>> secondLiteralList = new ArrayList<>(castedSecond);
        firstLiteralList.removeAll(secondLiteralList);
        distance = firstLiteralList.size();

        //Compute difference between second and first list of literals in answer set
        firstLiteralList = new ArrayList<>(castedFirst);
        secondLiteralList.removeAll(firstLiteralList);
        distance += secondLiteralList.size();

        return distance;
    }
}
