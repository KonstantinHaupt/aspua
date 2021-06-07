package de.aspua.framework.Controller.ControllerInterfaces;

import java.util.List;

import de.aspua.framework.Controller.ASPUAFrameworkAPI;

/**
 * Provides an abstract factory-pattern to create controller-objects for the {@link ASPUAFrameworkAPI}.
 */
public interface IFactoryController
{
	/**
	 * Dependency-Injection of the used {@link ASPUAFrameworkAPI}.
	 * May be relevant if a provided Interface-implementation needs a reference to an {@link ASPUAFrameworkAPI}-object
	 * in order to perform internal computations.
	 */
	public abstract void setFrameworkAPI(final ASPUAFrameworkAPI frameworkAPI);

	/**
	 * @return Instance of the {@link IIOController}-Interface
	 */
	public abstract IIOController createIOController();

	/**
	 * @return Instance of the {@link IParserController}-Interface
	 */
	public abstract IParserController createParser();

	/**
	 * @return Instance of the {@link ISolverController}-Interface
	 */
	public abstract ISolverController createSolver();

	/**
	 * @return Instance of the {@link IConflictDetectionController}-Interface
	 */
	public abstract IConflictDetectionController createConflictDetector();

	/**
	 * @return List of {@link IStrategyController}-Instances which are used to compute conflict solutions
	 */
	public abstract List<IStrategyController> getApplicableStrategies();

	/**
	 * @return List of {@link IMeasureController}-Instances which are used to compute measures for solutions
	 */
	public abstract List<IMeasureController> createMeasures();
}
