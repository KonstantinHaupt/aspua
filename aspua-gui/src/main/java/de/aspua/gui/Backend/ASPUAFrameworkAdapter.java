package de.aspua.gui.Backend;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.aspua.framework.Controller.ASPUAFrameworkAPI;
import de.aspua.framework.Controller.CausalRejectionController.CRFileFactory;
import de.aspua.framework.Controller.CausalRejectionController.CRSerialFactory;
import de.aspua.framework.Controller.CausalRejectionController.ELPParser;
import de.aspua.framework.Controller.CausalRejectionController.FileController;
import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPLiteral;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Model.ASP.ELP.ELPLiteral;
import de.aspua.framework.Model.ASP.ELP.ELPRule;
import de.aspua.framework.Utils.OperationTypeEnum;

/**
 * Adapter between the views/custom-components and an instance of {@link ASPUAFrameworkAPI} for performing the computations
 */
@Service
public class ASPUAFrameworkAdapter implements Serializable
{
    private static Logger LOGGER = LoggerFactory.getLogger(ASPUAFrameworkAdapter.class);
    public ASPUAFrameworkAPI frameworkAPI;

    // Indicates whether a unsaved-Changes dialog has to appear
    private boolean unsavedChanges;
    // Caches all loaded Programs from the Framework
    private List<ASPProgram<?,?>> availablePrograms;
    // Caches all conflicts from the beginning of the conflict resolution to enable back-navigation to ConflictView 
    private List<Conflict> cachedConflicts;

    public ASPUAFrameworkAdapter()
    {
        frameworkAPI = new ASPUAFrameworkAPI(new CRSerialFactory());
        availablePrograms = new ArrayList<>();
        cachedConflicts = new ArrayList<>();
        unsavedChanges = false;
    }
    
    public List<ASPProgram<?,?>> loadAvailablePrograms()
    {
        availablePrograms = frameworkAPI.loadAvailablePrograms();

        // Try backup loading from textfile-directory if no serial files were found
        if(availablePrograms == null || availablePrograms.isEmpty())
        {
            ASPUAFrameworkAPI helperFrameworkAPI = new ASPUAFrameworkAPI(new CRFileFactory());
            availablePrograms = helperFrameworkAPI.loadAvailablePrograms();

            if(availablePrograms != null)
            {
                for (ASPProgram<?, ?> program : availablePrograms)
                {
                    frameworkAPI.getiOController().persist(program, "");
                }
                availablePrograms = frameworkAPI.loadAvailablePrograms();
            }
        }

        if(availablePrograms.isEmpty())
            LOGGER.info("Didn't receive any Programs from the ASP-Update-Framework.");
        else
            LOGGER.info("Received {} Programs from the ASP-Update-Framework.", availablePrograms.size());
        
        return availablePrograms;
    }

    public ASPProgram<?,?> parseUploadedProgram(String programString, String programName)
    {
        ASPProgram<?,?> uploadedProgram = frameworkAPI.getParser().parseProgram(programString, programName);

        if(uploadedProgram == null || uploadedProgram.getRuleSet().isEmpty())
            return null;
        else
            return uploadedProgram;
    }

    public boolean addUploadedProgram(ASPProgram<?,?> uploadedProgram)
    {
        if(uploadedProgram == null || uploadedProgram.getRuleSet().isEmpty())
            return false;

        // Check if entry with the same name already exists
        if(availablePrograms.stream().anyMatch(x -> x.getProgramName().equals(uploadedProgram.getProgramName())))
            return false;

        // Check if persisting was successful
        if(!frameworkAPI.getiOController().persist(uploadedProgram, uploadedProgram.getProgramName()))
            return false;
        
        return availablePrograms.add(uploadedProgram);
    }

    public boolean deleteProgram(ASPProgram<?, ?> deletedProgram)
    {
        return frameworkAPI.getiOController().deleteProgram(deletedProgram);
    }

    public ASPLiteral<?> parseNewLiteral(String literalString)
    {
        // Abuse parseAnswerSets()-Method to parse the Literal
        List<String> literalList = new ArrayList<>();
        literalList.add(literalString);
        return new ELPParser().parseLiteral(literalString);
    }

    public boolean startUpdateProcess(ASPProgram<?, ?> newProgram)
    {
        if(newProgram == null || newProgram.getRuleSet().isEmpty())
            return false;

        this.setNewProgram(newProgram);
        cachedConflicts = frameworkAPI.detectConflicts();

        return cachedConflicts != null;
    }

    public File exportProgram(ASPProgram<?, ?> exportProgram)
    {
        FileController fileController = new FileController();
        return fileController.exportProgram(exportProgram);
    }

    public boolean persistUpdatedProgram(String programName) {
        return frameworkAPI.persistUpdatedProgram(programName);
    }

    public void computeMeasures(Solution solution) {
        frameworkAPI.computeMeasures(solution);
    }

    public List<Conflict> previewSolutionConflicts(Solution solution) {
        return frameworkAPI.previewSolutionConflicts(solution);
    }

    public List<AnswerSet<?, ?>> previewSolutionUpdateAnswerSets(Solution solution) {
        return this.generateUpdateAnswerSets(frameworkAPI.previewSolutionAnswerSets(solution));
    }

    public void solveConflict(Solution solution)
    {
        this.cachedConflicts = frameworkAPI.solveConflict(solution);
    }

    public ASPProgram<?,?> getUnmodifiedUpdateSequence()
    {
        List<ASPProgram<?, ?>> unmodifiedUpdateSequence = frameworkAPI.getUnmodifiedUpdateSequence();

        if(unmodifiedUpdateSequence == null || unmodifiedUpdateSequence.isEmpty())
            return null;
        
        ASPProgram<?,?> updateSequence = unmodifiedUpdateSequence.get(0).createNewInstance();
        for (ASPRule<?> rule : unmodifiedUpdateSequence.get(1).getRuleSet())
        {
            updateSequence.addRule(rule.createNewInstance());    
        }

        return updateSequence;
    }

    public HashMap<Integer, OperationTypeEnum> getAppliedSolutionOperations()
    {
        HashMap<Integer, OperationTypeEnum> transformedMapping = new HashMap<>();
        HashMap<OperationTypeEnum, List<ASPRule<?>>> originalMapping = frameworkAPI.getAppliedSolutionOperations();

        for (OperationTypeEnum currentOperation : originalMapping.keySet())
        {
            for (ASPRule<?> currentRule : originalMapping.get(currentOperation))
            {
                transformedMapping.put(currentRule.getLabelID(), currentOperation);
            }    
        }

        return transformedMapping;
    }
    
    public List<AnswerSet<?, ?>> getCurrentUpdateAnswerSets() {
        return this.generateUpdateAnswerSets(frameworkAPI.getCurrentAnswerSets());
    }

    public ASPProgram<?,?> getMergedUpdateSequence() {
        return frameworkAPI.getMergedUpdateSequence();
    }

    public ASPProgram<?,?> getInitialProgram() {
            return frameworkAPI.getInitialProgram();
    }

    public ASPProgram<?,?> getUnmodifiedInitialProgram() {
        if(!frameworkAPI.getUnmodifiedUpdateSequence().isEmpty())
            return frameworkAPI.getUnmodifiedUpdateSequence().get(0);
        else
            return null;
    }

    public void setInitialProgram(String programName){
        ASPProgram<?,?> initialProgram = availablePrograms.stream()
            .filter(x -> x.getProgramName().equals(programName))
            .findFirst()
            .orElse((availablePrograms.get(0)));

        frameworkAPI.clearData();
        frameworkAPI.setInitialProgram(initialProgram);
    }

    public void setInitialProgram(ASPProgram<?, ?> program){
        frameworkAPI.clearData();
        frameworkAPI.setInitialProgram(program);
    }

    public ASPProgram<?,?> getNewProgram() {
        if(frameworkAPI.getUpdateSequence().size() > 1)
            return frameworkAPI.getUpdateSequence().get(1);
        else
            return null;
    }

    public ASPProgram<?,?> getUnmodifiedNewProgram() {
        if(frameworkAPI.getUnmodifiedUpdateSequence().size() > 1)
            return frameworkAPI.getUnmodifiedUpdateSequence().get(1);
        else
            return null;
    }

    public void setNewProgram(ASPProgram<?, ?> newProgram) {

        if(frameworkAPI.getUnmodifiedUpdateSequence().size() > 1)
        {
            ASPProgram<?, ?> initialProgram = frameworkAPI.getUnmodifiedUpdateSequence().get(0);
            frameworkAPI.clearData();
            frameworkAPI.setInitialProgram(initialProgram);
        }

        if(newProgram != null)
            frameworkAPI.addToUpdateSequence(newProgram, false);
    }

    public List<Conflict> getCachedConflicts() {
        return cachedConflicts;
    }

    public void setCachedConflicts(List<Conflict> cachedConflicts) {
        this.cachedConflicts = cachedConflicts;
    }

    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    public void setUnsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;
    }

    private List<AnswerSet<?, ?>> generateUpdateAnswerSets(List<AnswerSet<?, ?>> answerSets)
    {
        if(answerSets == null || answerSets.isEmpty())
            return answerSets;
            
        List<AnswerSet<?, ?>> updateAnswerSets = new ArrayList<>();
        HashSet<ASPLiteral<?>> nonMetaLiterals = new HashSet<>(this.getInitialProgram().getLiteralBase().keySet());
        nonMetaLiterals.addAll(this.getNewProgram().getLiteralBase().keySet());

        for (AnswerSet<?, ?> currentAnswerSet : answerSets)
        {
            List<ELPLiteral> answerSetLiterals = new ArrayList<ELPLiteral>();
            for (ASPLiteral<?> currentLiteral : currentAnswerSet.getLiterals())
            {
                answerSetLiterals.add((ELPLiteral) currentLiteral);
            }

            answerSetLiterals.removeIf(x -> !nonMetaLiterals.contains(x));
            updateAnswerSets.add(new AnswerSet<ELPRule, ELPLiteral>(answerSetLiterals));
        }

        return updateAnswerSets;
    }
}