package de.aspua.framework.Controller.CausalRejectionController;

import java.util.ArrayList;
import java.util.List;

import de.aspua.framework.Controller.ASPUAFrameworkAPI;
import de.aspua.framework.Controller.ControllerInterfaces.IConflictDetectionController;
import de.aspua.framework.Controller.ControllerInterfaces.IFactoryController;
import de.aspua.framework.Controller.ControllerInterfaces.IIOController;
import de.aspua.framework.Controller.ControllerInterfaces.IMeasureController;
import de.aspua.framework.Controller.ControllerInterfaces.IParserController;
import de.aspua.framework.Controller.ControllerInterfaces.ISolverController;
import de.aspua.framework.Controller.ControllerInterfaces.IStrategyController;
import de.aspua.framework.Controller.Measures.AnswerSetMeasure;
import de.aspua.framework.Controller.Measures.RuleMeasure;
import de.aspua.framework.Controller.Strategies.DirectModificationStrategy;
import de.aspua.framework.Controller.Strategies.RejectionRuleStrategy;
import de.aspua.framework.Controller.Strategies.IndirectModificationStrategy;

/**
 * Provides controller-objects to perform an update process based on 'Towards Interactive Conflict Resolution in ASP Programs' by Thevapalan and Kern-Isberner for ELPs.
 */
public class CRFileFactory implements IFactoryController
{
	private ASPUAFrameworkAPI frameworkAPI;

	@Override
	public void setFrameworkAPI(final ASPUAFrameworkAPI frameworkAPI)
	{
		this.frameworkAPI = frameworkAPI;
	}

	@Override
	public IIOController createIOController()
	{
		return new FileController();
	}

	@Override
	public IParserController createParser()
	{
		return new ELPParser();
	}

	@Override
	public ISolverController createSolver()
	{
		return new ClingoRemoteSolver();
	}

	@Override
	public IConflictDetectionController createConflictDetector()
	{
		return new CRConflictDetector();
	}

    @Override
	public List<IStrategyController> getApplicableStrategies()
	{
		List<IStrategyController> strategies = new ArrayList<>();
		strategies.add(new DirectModificationStrategy());
		strategies.add(new IndirectModificationStrategy());
		strategies.add(new RejectionRuleStrategy());
		return strategies;
	}
	
	@Override
	public List<IMeasureController> createMeasures()
	{
		List<IMeasureController> measures = new ArrayList<>();
		measures.add(new AnswerSetMeasure(frameworkAPI));
		measures.add(new RuleMeasure(frameworkAPI));
		return measures;
	}
}
