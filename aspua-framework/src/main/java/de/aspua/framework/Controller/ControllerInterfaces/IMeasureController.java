package de.aspua.framework.Controller.ControllerInterfaces;

import de.aspua.framework.Model.Solution;

/**
 * Provides an Interface for computing measures for {@link Solution}-objects of a conflict.
 * Each implementations represents a measure with its own {@link SolutionMetaDataEnum}-key.
 */
public interface IMeasureController
{
    /**
     * Computes a specific measure for the given solution. The computed measure is set in the metadata-Attribute of the given solution.
     * The measure is only computed for the currently chosen operations. If a operation-variant is chosen, the measure has to be applied once again.
     * @param solution Solution of interest
     * @see Solution#chooseVariant(de.aspua.framework.Model.ASP.BaseEntities.ASPRule)
     */
    public void computeMeasure(Solution solution);
}
