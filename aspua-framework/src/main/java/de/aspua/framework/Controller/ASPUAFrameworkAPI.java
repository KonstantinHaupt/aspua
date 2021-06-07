package de.aspua.framework.Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aspua.framework.Controller.ControllerInterfaces.IConflictDetectionController;
import de.aspua.framework.Controller.ControllerInterfaces.IFactoryController;
import de.aspua.framework.Controller.ControllerInterfaces.IIOController;
import de.aspua.framework.Controller.ControllerInterfaces.IMeasureController;
import de.aspua.framework.Controller.ControllerInterfaces.IParserController;
import de.aspua.framework.Controller.ControllerInterfaces.ISolverController;
import de.aspua.framework.Controller.ControllerInterfaces.IStrategyController;
import de.aspua.framework.Model.Conflict;
import de.aspua.framework.Model.Solution;
import de.aspua.framework.Model.ASP.BaseEntities.AnswerSet;
import de.aspua.framework.Model.ASP.BaseEntities.ASPProgram;
import de.aspua.framework.Model.ASP.BaseEntities.ASPRule;
import de.aspua.framework.Utils.OperationTypeEnum;

/**
 * API for the update-framework as described in 'Towards Interactive Conflict Resolution in ASP Programs' by Thevapalan and Kern-Isberner.
 * The API provides methods for detecting and solving conflicts between ASP-Programs. The update process can be divided into 5 separate steps: <p>
 *
 * 0. Initialize the API-Class with an instance of the {@link #IFactoryController} ({@link #ASPUAFrameworkAPI(IFactoryController)}). <p>
 * 1. Add a number of logic Programs to the update sequence ({@link #addToUpdateSequence}). <p>
 * 2. Start the update process by detecting conflicts between the rules of the programs within the update sequence ({@link #detectConflicts()}). <p>
 * 3. Solve each detected conflict ({@link #solveConflict(Solution)}). <P>
 * 4. Merge and persist the updated, conflict-free update sequence ({@link #persistUpdatedProgram(String)}). <p>
 * 
 * If serveral update-iterations should be performed, the local attributes should be cleared between each iteration using {@link #clearData()}.
 * 
 * @author Konstantin Haupt
 */
public class ASPUAFrameworkAPI
{
	private static Logger LOGGER = LoggerFactory.getLogger(ASPUAFrameworkAPI.class);

	private IFactoryController usedFactory;
	private ISolverController solver;
	private IParserController parser;
	private IConflictDetectionController conflictDetector;
	private IIOController iOController;
	
	private List<IMeasureController> measures;
	private List<IStrategyController> strategies;
	
	private List<ASPProgram<?,?>> updateSequence;
	private List<ASPProgram<?,?>> unmodifiedUpdateSequence;
	private List<Conflict> currentConflicts;

	private List<AnswerSet<?, ?>> currentAnswerSets;
	private HashMap<OperationTypeEnum, List<ASPRule<?>>> appliedSolutionOperations;

	/**
	 * Constructor to provide all necessary references to Controllers which are used by the API in the update process.
	 * @param factory Object which is used to initialize the used controller-objects
	 * @see IFactoryController
	 */
	public ASPUAFrameworkAPI(IFactoryController factory)
	{
		printStartUpBanner();

		usedFactory = factory;
		usedFactory.setFrameworkAPI(this);
		if(usedFactory == null)
        {
            LOGGER.error("A fatal error occured while trying to instantiate the Factory for Framwork-Startup! The execution will be aborted!");
			System.exit(1);
        }
		else
			LOGGER.info("Factory-Initialization successfull. Using the Factory-Class '{}' for further execution.", usedFactory.getClass().getName());

		solver = usedFactory.createSolver();
		parser = usedFactory.createParser();
		conflictDetector = usedFactory.createConflictDetector();
		iOController = usedFactory.createIOController();
		measures = usedFactory.createMeasures();
		strategies = usedFactory.getApplicableStrategies();
		this.clearData();
	}

	/**
	 * Loads all available {@link ASPProgram}-objects which are provided by the current input source.
	 * @return List of all available {@link ASPProgram}-objects
	 * @see IIOController
	 */
	public List<ASPProgram<?,?>> loadAvailablePrograms()
	{
		List<ASPProgram<?,?>> availablePrograms = iOController.loadAvailableParsedPrograms();

		// Fallback if parsed Programs aren't available (e.g. in FileController)
		if(availablePrograms == null)
		{
			availablePrograms = new ArrayList<>();
			Map<String,String> loadedPrograms = iOController.loadAvailableProgramStrings();
			if(loadedPrograms == null)
			{
				LOGGER.warn("The available Programs couldn't be loaded by the current IOController.");
				return new ArrayList<>();
			}

			for (String programName : loadedPrograms.keySet())
			{
				ASPProgram<?,?> program = parser.parseProgram(loadedPrograms.get(programName), programName);

				if(program != null)
					availablePrograms.add(program);
			}
		}

		return availablePrograms;
	}

	/**
	 * 1. Step of the update process <p>
	 * Adds the given {@link ASPProgram}-object to the end of the update sequence.
	 * This method has to be invoked at least two times to define a valid update sequence for executing further steps of the update process.
	 * @param program object which is added to the update sequence
	 * @param adaptLabelIDs If true, all label-IDs of the given object are reassigned to a unique value within the update sequence
	 * (starting from the total number of rules in the update sequence)
	 * @return True, if the given object was succesfully added to the update program, false otherwise
	 */
	public boolean addToUpdateSequence(ASPProgram<?, ?> program, boolean adaptLabelIDs)
	{
		if(program == null || program.getRuleSet().isEmpty())
		{
			LOGGER.warn("The given ASP-Program wasn't added to the Update-Sequence because it was null or empty!");
			return false;
		}

		if(adaptLabelIDs)
			this.applyNameFunctionToLabelID(program);

		updateSequence.add(program);
		unmodifiedUpdateSequence.add(program.createNewInstance());
		return true;
	}

	/**
	 * 1. Step of the update process <p>
	 * Reads in the ASP-program with the given name and adds the corresponding {@link ASPProgram}-object to the end of the update sequence.
	 * This method has to be invoked at least two times to define a valid update sequence for executing further steps of the update process.
	 * @param program object which is added to the update sequence
	 * @param adaptLabelIDs If true, all label-IDs of the given object are reassigned to a unique value within the update sequence
	 * (starting from the total number of rules in the update sequence)
	 * @return True, if the given object was succesfully added to the update program, false otherwise
	 * @see IIOController
	 * @see IParserController
	 */
	public boolean addToUpdateSequence(String programName)
	{
		if(programName == null || programName.isEmpty())
		{
			LOGGER.warn("The given ASP-Program wasn't added to the Update-Sequence because the given String was null or empty!");
			return false;
		}

		String programString = iOController.loadProgram(programName);
        ASPProgram<?, ?> program = parser.parseProgram(programString, programName);

		if(program == null || program.getRuleSet().isEmpty())
		{
			LOGGER.warn("The given ASP-Program wasn't added to the Update-Sequence because no rules could be parsed from the Input-String!");
			return false;
		}
		this.applyNameFunctionToLabelID(program);

		updateSequence.add(program);
		unmodifiedUpdateSequence.add(program.createNewInstance());
		return true;
	}
	
	/** 
	 * 2. Step of the update process <p>
	 * Detects all conflicts between rules in the current update sequence. For all detected conflicts, all non-complex strategies for conflict-solving are applied. 
	 * In addition, all provided measures are computed for the assigned solutions. The update sequence has to contain (at least) two ASP-programs. <p>
	 * Depending on the used technique for conflict-detection, an empty list as a return value doesn't necessarily indicates a conflict-free update sequence.
	 * As the conflict-detection is based on the inspection of models, a non-prevented inconsistency of the used ASP-program for conflict detection
	 * could also be the cause for no detected conflicts. To ensure that the return value of an empty list equals a conflict-free update sequence, check
	 * {@link #getCurrentAnswerSets()}.
	 * @return List of all detected conflicts, whose {@link Conflict}-objects contain all generated {@link Solution}-objects.
	 * Returns an empty list if no conflicts were detected.
	 * Return null if an error occured while computing the conflicts.
	 * @see IConflictDetectionController
	 * @see IMeasureController
	 * @see IParserController
	 * @see ISolverController
	 * @see IStrategyController
	 */
	public List<Conflict> detectConflicts()
	{
		if(updateSequence.size() < 2)
		{
			LOGGER.warn("The update rocess cannot be initiated with less than two ASP-Programs!");
			return null;
		}

		return this.computeConflicts();
	}

	/**
	 * 3. Step of the update process. <p>
	 * Applies all rule operations which are defined in the given {@link Solution}-object to the ASP-programs of the update sequence.
	 * Afterwards, the conflicts for the resulting update sequence are computed as described in {@link #detectConflicts()}.
	 * @param solution Solution which provides all rule operations which are applied to the ASP-programs of the update sequence.
	 * @return List of all detected conflicts, whose {@link Conflict}-objects contain all generated {@link Solution}-objects.
	 * Returns an empty list if no conflicts were detected.
	 * Return null if an error occured while computing the conflicts.
	 * @see IConflictDetectionController
	 * @see IMeasureController
	 * @see IParserController
	 * @see ISolverController
	 * @see IStrategyController
	 */
	public List<Conflict> solveConflict(Solution solution)
	{
		this.applySolutionToHelperSequence(solution, updateSequence);

		// All added rules have to be new rules, and therefore can immediately be added to the list of overall added rules
		appliedSolutionOperations.get(OperationTypeEnum.ADD).addAll(solution.getAddedRules());
		// Rules can only be deleted once, and therefore can immediately be added to the list of overall deleted rules
		appliedSolutionOperations.get(OperationTypeEnum.DELETE).addAll(solution.getDeletedRules());

		for (ASPRule<?> modifiedRule : solution.getModifiedRules())
		{
			// If a rule is modified multiple times, only the most recent state of the rule is saved. The previous state is removed
			appliedSolutionOperations.get(OperationTypeEnum.MODIFY)
				.removeIf(x -> x.getID().equals(modifiedRule.getID()));

			// If the modified rule was only added in the conflict-resolution process, the modified rule is effectively still an added rule
			boolean wasAdded = appliedSolutionOperations.get(OperationTypeEnum.ADD).stream()
				.anyMatch(x -> x.getID().equals(modifiedRule.getID()));
			
			if(wasAdded)
				appliedSolutionOperations.get(OperationTypeEnum.ADD).add(modifiedRule);
			else
				appliedSolutionOperations.get(OperationTypeEnum.MODIFY).add(modifiedRule); 
		}

		return this.computeConflicts();
	}

	/**
	 * 4. Step of the update process. <p>
	 * Merges all rules of the current update sequence into a single ASP-program and persists it under the given name.
	 * This method should only be invoked if all detected conflict have been solving using the {@link #solveConflict(Solution)}-method.
	 * If this method is successfully executed, the update-process is finished. How the updated ASP-program is persisted is determined by the
	 * specific implementation of the {@link IIOController#persist(String)}-method. If the given name is null, empty or equals an existing entry,
	 * an entry may be overwritten.
	 * @param newProgramName Name for persisiting the updated ASP-program. 
	 * @return True, if the updated update sequence could be successfully merged into a single ASP-Program and persisted. False otherwise.
	 * @see IIOController
	 */
	public boolean persistUpdatedProgram(String newProgramName)
	{
		ASPProgram<?, ?> updatedProgram = this.getMergedUpdateSequence();

		if(updatedProgram == null)
		{
			LOGGER.warn("The Update-Sequence couldn't be persisted because a problem occured while trying to merge the Update-Sequence into a single Program.");
			return false;
		}

		if(newProgramName != null && !newProgramName.isEmpty())
			updatedProgram.setProgramName(newProgramName);
		
		boolean success = iOController.persist(updatedProgram, newProgramName);

		if(success)
			return true;
		else
		{
			LOGGER.warn("The Update-Sequence couldn't be persisted. Please ensure the used IOController {} works correctly.", iOController.getClass().getName());
			return false;
		}
	}

	/**
	 * Merges all ASP-Programms of the current update sequence into a single {@link ASPProgram}-object.
	 * A copy of the first ASP-program is used as a starting point. Thus, the name of the returned object equals the name of the first object of the update sequence.
	 * @return A new {@link ASPProgram}-object containing copies of all rules within the update sequence.
	 */
	public ASPProgram<?, ?> getMergedUpdateSequence()
	{
		if(currentConflicts != null && !currentConflicts.isEmpty())
		{
			LOGGER.warn("The Update-Sequence shouldn't be merged if there are still unsolved conflicts between rules! {} {}", System.lineSeparator(), currentConflicts);
		}

		if(this.getInitialProgram() == null)
		{
			LOGGER.warn("The Update-Sequence couldn't be merged because no initial Program exists!");
			return null;
		}

		ASPProgram<?, ?> initialProgram = this.getInitialProgram().createNewInstance();
		
		boolean success = true;
		for (int i = 1; i < updateSequence.size(); i++)
		{
			for (ASPRule<?> currentRule : updateSequence.get(i).getRuleSet())
			{
				success = initialProgram.addRule(currentRule.createNewInstance());

				if(!success)
					break;
			}
		}

		if(!success)
		{
			LOGGER.warn("The Update-Sequence couldn't be merged because at least one rule couldn't be added to the merged Program.");
			return null;
		}

		return initialProgram;
	}

	/**
	 * Applies a given solution to a copy of the current update sequence and computes remaining conflicts in the resulting update sequence.
	 * All internal data of this API-object such as the actual update sequence or the current answer sets remain unaffected.
	 * This method cannot be used to actually solve a conflict. For that purpose, use the {@link #solveConflict(Solution)}-method.
	 * @param solution Solution whose resulting update sequence is investigated
	 * @return List of all conflict which are detected in the (copied) update sequence after applying the solution
	 * Returns an empty list if no conflicts were detected.
	 * Return null if an error occured while computing the conflicts.
	 * @see IConflictDetectionController
	 * @see IParserController
	 * @see ISolverController
	 */
	public List<Conflict> previewSolutionConflicts(Solution solution)
	{
		// Adapt code from computeConflicts() but adjust the just local variables, as the attributes shouldn't be changed
		List<ASPProgram<?, ?>> previewSequence = this.copyUpdateSequence();
		this.applySolutionToHelperSequence(solution, previewSequence);
		
		ASPProgram<?, ?> conflictProgram = conflictDetector.computeConflictDetectionProgram(previewSequence);
		if(conflictProgram == null)
		{
			LOGGER.warn("The computed modified update program couldn't be computed. The conflict-detection will be aborted.");
			return null;
		}

		List<String> models = solver.computeModels(conflictProgram);
		if(models == null || models.isEmpty())
		{
			LOGGER.info("The program for conflict detection has no answer sets. Therefore, no conflicts can be detected.");
			return new ArrayList<>();
		}

		List<AnswerSet<?, ?>> answerSets = parser.parseAnswerSets(models);
		if(answerSets == null)
		{
			LOGGER.info("The solver computed models which couldn't be parsed to valid AnswerSet-objects. Therefore, no conflicts can be detected.");
			return null;
		}

		List<Conflict> previewedConflicts = conflictDetector.detectConflicts(previewSequence, answerSets);
		
		if(previewedConflicts == null)
			previewedConflicts = new ArrayList<>();
		
		return previewedConflicts;
	}
	
	/**
	 * Applies a given solution to a copy of the current update sequence and computes the answer sets of the resulting update sequence.
	 * All internal data of this API-object such as the actual update sequence or the current answer sets remain unaffected.
	 * This method cannot be used to actually solve a conflict. For that purpose, use the {@link #solveConflict(Solution)}-method.
	 * @param solution Solution whose resulting update sequence is investigated
	 * @return List with all answer sets of the (copied) update sequence after applying the solution
	 * Returns an empty list if the update sequence after applying the solution doesn't contain any answer sets
	 * Return null if an error occured while computing the conflicts.
	 * @see IConflictDetectionController
	 * @see IParserController
	 * @see ISolverController
	 */
	public List<AnswerSet<?, ?>> previewSolutionAnswerSets(Solution solution)
	{
		// Adapt code from computeConflicts() but adjust the just local variables, as the attributes shouldn't be changed
		List<ASPProgram<?, ?>> previewSequence = this.copyUpdateSequence();
		this.applySolutionToHelperSequence(solution, previewSequence);
		
		ASPProgram<?, ?> conflictProgram = conflictDetector.computeConflictDetectionProgram(previewSequence);
		if(conflictProgram == null)
		{
			LOGGER.warn("The computed modified update program couldn't be computed. The conflict-detection will be aborted.");
			return null;
		}

		List<String> models = solver.computeModels(conflictProgram);
		if(models == null || models.isEmpty())
		{
			LOGGER.info("The program for conflict detection has no answer sets. Therefore, no conflicts can be detected.");
			return new ArrayList<>();
		}

		return parser.parseAnswerSets(models);
	}

	/**
	 * Applies all measures to all {@Solution}-objects of the given conflict.
	 * If only one particular solution has to be inspected, the {@link #computeMeasures(Solution)}-method provides a more efficient computation.
	 * @param conflict Conflict, whose solutions should be measured
	 * @see IMeasureController
	 */
	public void computeMeasures(Conflict conflict)
	{
		for (IMeasureController measure : measures)
		{
			for (Solution solution : conflict.getSolutions())
				measure.computeMeasure(solution);
		}
	}

	/**
	 * Applies all measures to a single solution.
	 * If all solutions of a conflict are supposed to be inspected, the {@link #computeMeasures(Conflict)}-method may be used.
	 * @param solution Solution which should be measured
	 * @see IMeasureController
	 */
	public void computeMeasures(Solution solution)
	{
		for (IMeasureController measure : measures)
			measure.computeMeasure(solution);
	}

	/**
	 * Clears all internal data which was saved during the current update-process.
	 * This includes the update sequence as well as the current answer sets and conflicts.
	 * After invoking this method, a new update-process can be started by adding ASP-programs to the update sequence ({@link #addToUpdateSequence}).
	 */
	public void clearData()
	{
		updateSequence = new ArrayList<>();
		unmodifiedUpdateSequence = new ArrayList<>();
		currentConflicts = new ArrayList<>();
		currentAnswerSets = null;
		this.setAppliedSolutionOperations(null);
	}

	/**
	 * Returns the first ASP-program of the update sequence, which represents the oldest knowledge which is updated by all ASP-programs with higher order.
	 * @return The first {@link ASPProgram}-object within the current update sequence
	 * Null, if the update sequence doesn't contain any 
	 */
	public ASPProgram<?, ?> getInitialProgram()
	{
		if(updateSequence.size() > 0)
			return updateSequence.get(0);
		else
			return null;
	}

	/**
	 * Sets the first ASP-program of the update sequence, which represents the oldest knowledge which is updated by all ASP-programs with higher order.
	 * @param initialProgram {@link ASPProgram}-object which is set at the first position of the update sequence
	 * @return True, if the given ASP-program was successfully set as the first program of the update sequence. False otherwise.
	 */
	public boolean setInitialProgram(ASPProgram<?, ?> initialProgram)
	{
		if(initialProgram == null || initialProgram.getRuleSet().isEmpty())
		{
			LOGGER.warn("The given ASP-Program wasn't set as the initial ASP-Program of the Update-Sequence because it was null or empty!");
			return false;
		}

		boolean success = true;
		if(updateSequence.size() > 0)
		{
			updateSequence.set(0, initialProgram);
			unmodifiedUpdateSequence.set(0, initialProgram.createNewInstance());
		}
		else
		{
			success = updateSequence.add(initialProgram);
			unmodifiedUpdateSequence.add(initialProgram.createNewInstance());
		}

		return success;
	}

	/**
	 * Returns the current update sequence. While conflicts are solved,
	 * this method always returns the most recent update sequence with all applied changes.
	 * To compare the current update sequence to the 'original' update sequence without any
	 * applied changes through conflict resolutions, use {@link #getUnmodifiedUpdateSequence()}.
	 * @return The current update sequence
	 */
	public List<ASPProgram<?, ?>> getUpdateSequence() {
		return updateSequence;
	}

	/**
	 * Returns the update sequence at the beginning of the 2. Step of the update process,
	 * i.e. before solutions were applied to the ASP-programs of the update sequence.
	 * To get the current update sequence with all applied changes, use {@link #getUpdateSequence()}.
	 * @return The initial update sequence without any changes.
	 */
	public List<ASPProgram<?, ?>> getUnmodifiedUpdateSequence() {
		return unmodifiedUpdateSequence;
	}

	/**
	 * Returns all conflicts for the current update sequence. Is equal to the return values of {@link #detectConflicts()} and {@link #solveConflict(Solution)}. 
	 * @return List of all conflicts for the current update sequence
	 */
	public List<Conflict> getCurrentConflicts() {
		return currentConflicts;
	}

	/**
	 * Returns all operations which were applied by solutions of the current update process.
	 */
	public HashMap<OperationTypeEnum, List<ASPRule<?>>> getAppliedSolutionOperations() {
		return appliedSolutionOperations;
	}

	/**
	 * Returns the answer sets of the current update sequence.
	 * Depending on the used strategy for conflict-detection, the answer sets may contain meta-literals.
	 * @see IConflictDetectionController
	 */
	public List<AnswerSet<?, ?>> getCurrentAnswerSets() {
		return currentAnswerSets;
	}

	public ISolverController getSolver() {
		return solver;
	}

	public void setSolver(ISolverController solver) {
		this.solver = solver;
	}

	public IParserController getParser() {
		return parser;
	}

	public void setParser(IParserController parser) {
		this.parser = parser;
	}

	public IFactoryController getUsedFactory() {
		return usedFactory;
	}

	public void setUsedFactory(IFactoryController usedFactory) {
		this.usedFactory = usedFactory;
	}

	public IConflictDetectionController getConflictDetector() {
		return conflictDetector;
	}

	public void setConflictDetector(IConflictDetectionController conflictDetector) {
		this.conflictDetector = conflictDetector;
	}

	public IIOController getiOController() {
		return iOController;
	}

	public void setiOController(IIOController iOController) {
		this.iOController = iOController;
	}

	public List<IStrategyController> getStrategies() {
		return strategies;
	}

	public void setStrategies(List<IStrategyController> strategies) {
		this.strategies = strategies;
	}

	public List<IMeasureController> getMeasures() {
		return measures;
	}

	public void setMeasures(List<IMeasureController> measures) {
		this.measures = measures;
	}

	private void setAppliedSolutionOperations(HashMap<OperationTypeEnum, List<ASPRule<?>>> appliedSolutionOperations)
	{
		if(appliedSolutionOperations == null || appliedSolutionOperations.isEmpty())
		{
			this.appliedSolutionOperations = new HashMap<>();
			this.appliedSolutionOperations.put(OperationTypeEnum.ADD, new ArrayList<>());
			this.appliedSolutionOperations.put(OperationTypeEnum.MODIFY, new ArrayList<>());
			this.appliedSolutionOperations.put(OperationTypeEnum.DELETE, new ArrayList<>());
		}
		else
			this.appliedSolutionOperations = appliedSolutionOperations;
	}

	/**
	 * Internal method for detecting conflicts using the {@link IConflictDetectionController}-object provided by the factory.
	 * @return List of all detected conflicts, whose {@link Conflict}-objects contain the generated {@link Solution}-objects with computed measures.
	 * Returns an empty list if no conflicts were detected.
	 * Return null if an error occured while computing the conflicts.
	 */
	private List<Conflict> computeConflicts()
	{
		ASPProgram<?, ?> conflictDetectionProgram = conflictDetector.computeConflictDetectionProgram(updateSequence);
		if(conflictDetectionProgram == null)
		{
			LOGGER.warn("The computed modified update program couldn't be computed. The conflict-detection will be aborted.");
			return null;
		}

		List<String> models = solver.computeModels(conflictDetectionProgram);
		if(models == null || models.isEmpty())
		{
			LOGGER.info("The program for conflict detection has no answer sets. Therefore, no conflicts can be detected.");
			currentAnswerSets = null;
			currentConflicts = new ArrayList<>();
			return currentConflicts;
		}

		currentAnswerSets = parser.parseAnswerSets(models);
		if(currentAnswerSets == null)
		{
			LOGGER.info("The solver computed models which couldn't be parsed to valid AnswerSet-objects. Therefore, no conflicts can be detected.");
			return null;
		}

		currentConflicts = conflictDetector.detectConflicts(updateSequence, currentAnswerSets);

		if(currentConflicts == null)
		{
			currentConflicts = new ArrayList<>();
			return currentConflicts;
		}

		if(!currentConflicts.isEmpty())
		{
			for (IStrategyController currentStrategy : strategies)
			{
				if(currentStrategy.isComplex())
					continue;
				
				for (Conflict currentConflict : currentConflicts)
				{
					currentStrategy.apply(updateSequence, currentConflict);
				}
			}

			for (Conflict currentConflict : currentConflicts)
			{
				this.computeMeasures(currentConflict);
			}
		}

		return currentConflicts;
	}

	/**
	 * Applies a given solution to a given update sequence.
	 * The update sequence is explicitly given to distinguish between previews and actual solving
	 * @param solution Solution which should be applied
	 * @param updatedSequence Update sequence which is 
	 */
	private void applySolutionToHelperSequence(Solution solution, List<ASPProgram<?, ?>> updatedSequence)
	{
		if(solution == null || updatedSequence == null)
		{
			LOGGER.warn("The application of a solution was requested, but wasn't executed, as the solution or update-sequence were null.");
			return;
		}

		List<ASPRule<?>> add = solution.getAddedRules();
		List<ASPRule<?>> modify = solution.getModifiedRules();
		List<ASPRule<?>> delete = solution.getDeletedRules();

		for (ASPRule<?> currentRule : add)
		{
			updatedSequence.get(updatedSequence.size()-1).addRule(currentRule);
		}

		for (ASPRule<?> currentRule : modify)
		{
			for (ASPProgram<?, ?> currentProgram : updatedSequence)
			{
				currentProgram.modifyRule(currentRule);
			}
		}

		for (ASPRule<?> currentRule : delete)
		{
			for (ASPProgram<?, ?> currentProgram : updatedSequence)
			{
				currentProgram.deleteRule(currentRule.getID());
			}
		}
	}
	
	/**
	 * Helper-method to create new instances for each object in the update sequence
	 */
	private List<ASPProgram<?,?>> copyUpdateSequence()
	{
		List<ASPProgram<?, ?>> copiedSequence = new ArrayList<>();
		for (ASPProgram<?,?> currentProgram : updateSequence)
		{
			copiedSequence.add(currentProgram.createNewInstance());
		}

		return copiedSequence;
	}

	/**
	 * Assigns unique labelIDs to the rules of a given program by incrementing a counter
	 * starting from the total number of rules within the update sequence.
	 */
	private void applyNameFunctionToLabelID(ASPProgram<?,?> program)
	{
		ASPProgram<?,?> combinedSequence = this.getMergedUpdateSequence();

		if(combinedSequence == null)
			combinedSequence = new ASPProgram<>();
	
		int ruleCount = combinedSequence.getRuleSet().size();
		for(int i = 0; i < program.getRuleSet().size(); i++)
		{
			while(combinedSequence.getRuleByLabelID(ruleCount + i) != null)
				ruleCount++;

			program.getRuleSet().get(i).setLabelID(ruleCount + i);
		}
	}
	
	/**
	 * Prints a Banner to the console to signal the invocation of the API (i.e. the constructor).
	 * By printing the banner, the invocation is easily noticable if the framework is used in larger applications/frameworks such as Spring.
	 */
	private void printStartUpBanner()
    {
        try
		{
			InputStream inputStream = this.getClass().getResourceAsStream("/framework_banner.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

			String line = br.readLine();
            while(line != null)
            {
                System.out.println(line);
                line = br.readLine();
            }
            br.close();
            
        } catch (IOException e) {
			System.out.println("---------------------------------------");
            System.out.println("Started the ASPUA-Framework");
			System.out.println("---------------------------------------");
        }
    }
}
