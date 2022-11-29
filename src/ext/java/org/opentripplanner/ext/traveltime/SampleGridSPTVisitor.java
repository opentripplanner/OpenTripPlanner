package org.opentripplanner.ext.traveltime;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.traveltime.geometry.AccumulativeGridSampler;
import org.opentripplanner.ext.traveltime.spt.SPTVisitor;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.state.State;

class SampleGridSPTVisitor implements SPTVisitor {

  private final int maxTimeSec;
  private final AccumulativeGridSampler<WTWD> gridSampler;
  private final double offRoadWalkSpeedMps;

  public SampleGridSPTVisitor(
    int maxTimeSec,
    AccumulativeGridSampler<WTWD> gridSampler,
    double offRoadWalkSpeedMps
  ) {
    this.maxTimeSec = maxTimeSec;
    this.gridSampler = gridSampler;
    this.offRoadWalkSpeedMps = offRoadWalkSpeedMps;
  }

  @Override
  public boolean accept(Edge e) {
    return e instanceof StreetEdge;
  }

  @Override
  public void visit(
    Edge e,
    Coordinate c,
    State s0,
    State s1,
    double d0,
    double d1,
    double speedAlongEdge
  ) {
    double t0 = s0.getElapsedTimeSeconds() + d0 / speedAlongEdge;
    double t1 = s1.getElapsedTimeSeconds() + d1 / speedAlongEdge;
    if (t0 < maxTimeSec || t1 < maxTimeSec) {
      if (!Double.isInfinite(t0) || !Double.isInfinite(t1)) {
        WTWD z = new WTWD();
        z.w = 1.0;
        z.d = 0.0;
        if (t0 < t1) {
          z.wTime = t0;
          z.wWalkDist = s0.getWalkDistance() + d0;
        } else {
          z.wTime = t1;
          z.wWalkDist = s1.getWalkDistance() + d1;
        }
        gridSampler.addSamplingPoint(c, z, offRoadWalkSpeedMps);
      }
    }
  }
}
