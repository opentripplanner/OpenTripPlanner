package org.opentripplanner.ext.traveltime;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.traveltime.geometry.AccumulativeMetric;
import org.opentripplanner.ext.traveltime.geometry.ZSamplePoint;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;

/**
 * Any given sample is weighted according to the inverse of the squared normalized distance + 1 to
 * the grid sample. We add to the sampling time a default off-road walk distance to account for
 * off-road sampling. TODO how does this "account" for off-road sampling ?
 */
public class WTWDAccumulativeMetric implements AccumulativeMetric<WTWD> {

  private final double cosLat;
  private final double offRoadDistanceMeters;
  private final double offRoadSpeed;
  private final double gridSizeMeters;

  public WTWDAccumulativeMetric(
    double cosLat,
    double offRoadDistanceMeters,
    double offRoadSpeed,
    double gridSizeMeters
  ) {
    this.cosLat = cosLat;
    this.offRoadDistanceMeters = offRoadDistanceMeters;
    this.offRoadSpeed = offRoadSpeed;
    this.gridSizeMeters = gridSizeMeters;
  }

  @Override
  public WTWD cumulateSample(Coordinate C0, Coordinate Cs, WTWD z, WTWD zS, double offRoadSpeed) {
    double t = z.wTime / z.w;
    double wd = z.wWalkDist / z.w;
    double d = SphericalDistanceLibrary.fastDistance(C0, Cs, cosLat);
    // additional time
    double dt = d / offRoadSpeed;
    /*
     * Compute weight for time. The weight function to distance here is somehow arbitrary.
     * Its only purpose is to weight the samples when there is various samples within the
     * same "cell", giving more weight to the closest samples to the cell center.
     */
    double w = 1 / ((d + gridSizeMeters) * (d + gridSizeMeters));
    if (zS == null) {
      zS = new WTWD();
      zS.d = Double.MAX_VALUE;
    }
    zS.w = zS.w + w;
    zS.wTime = zS.wTime + w * (t + dt);
    zS.wWalkDist = zS.wWalkDist + w * (wd + d);
    if (d < zS.d) {
      zS.d = d;
    }
    return zS;
  }

  /**
   * A Generated closing sample take 1) as off-road distance, the minimum of the off-road distance
   * of all enclosing samples, plus the grid size, and 2) as time the minimum time of all enclosing
   * samples plus the grid size * off-road walk speed as additional time. All this are
   * approximations.
   * <p>
   * TODO Is there a better way of computing this? Here the computation will be different
   * based on the order where we close the samples.
   */
  @Override
  public boolean closeSample(ZSamplePoint<WTWD> point) {
    double dMin = Double.MAX_VALUE;
    double tMin = Double.MAX_VALUE;
    double wdMin = Double.MAX_VALUE;
    List<WTWD> zz = new ArrayList<>(4);
    if (point.up() != null) zz.add(point.up().getZ());
    if (point.down() != null) zz.add(point.down().getZ());
    if (point.right() != null) zz.add(point.right().getZ());
    if (point.left() != null) zz.add(point.left().getZ());
    for (WTWD z : zz) {
      if (z.d < dMin) dMin = z.d;
      double t = z.wTime / z.w;
      if (t < tMin) tMin = t;
      double wd = z.wWalkDist / z.w;
      if (wd < wdMin) wdMin = wd;
    }
    WTWD z = new WTWD();
    z.w = 1.0;
    /*
     * The computations below are approximation, but we are on the edge anyway and the
     * current sample does not correspond to any computed value.
     */
    z.wTime = tMin + gridSizeMeters / offRoadSpeed;
    z.wWalkDist = wdMin + gridSizeMeters;
    z.d = dMin + gridSizeMeters;
    point.setZ(z);
    return dMin > offRoadDistanceMeters;
  }
}
