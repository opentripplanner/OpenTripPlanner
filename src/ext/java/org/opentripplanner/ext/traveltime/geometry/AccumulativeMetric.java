package org.opentripplanner.ext.traveltime.geometry;

import org.locationtech.jts.geom.Coordinate;

/**
 * An accumulative metric give the behavior of combining several samples to a regular sample grid,
 * ie how we should weight and add several TZ values from inside a cell to compute the cell corner
 * TZ values.
 *
 * @author laurent
 */
public interface AccumulativeMetric<TZ> {
  /**
   * Callback function to handle a new added sample.
   *
   * @param C0           The initial position of the sample, as given in the addSample() call.
   * @param Cs           The position of the sample on the grid, never farther away than (dX,dY)
   * @param z            The z value of the initial sample, as given in the addSample() call.
   * @param zS           The previous z value of the sample. Can be null if this is the first time,
   *                     it's up to the caller to initialize the z value.
   * @param offRoadSpeed The offroad speed to assume.
   * @return The modified z value for the sample.
   */
  TZ cumulateSample(Coordinate C0, Coordinate Cs, TZ z, TZ zS, double offRoadSpeed);

  /**
   * Callback function to handle a "closing" sample (that is a sample post-created to surround
   * existing samples and provide nice and smooth edges for the algorithm).
   *
   * @param point The point to set Z.
   * @return True if the point "close" the set.
   */
  boolean closeSample(ZSamplePoint<TZ> point);
}
