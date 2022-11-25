package org.opentripplanner.ext.traveltime;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.ext.traveltime.geometry.AccumulativeGridSampler;
import org.opentripplanner.ext.traveltime.geometry.AccumulativeMetric;
import org.opentripplanner.ext.traveltime.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.ext.traveltime.geometry.ZSampleGrid;
import org.opentripplanner.ext.traveltime.spt.SPTVisitor;
import org.opentripplanner.ext.traveltime.spt.SPTWalker;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleGridRenderer {

  private static final Logger LOG = LoggerFactory.getLogger(SampleGridRenderer.class);

  public static ZSampleGrid<WTWD> getSampleGrid(
    ShortestPathTree<State, Edge, Vertex> spt,
    TravelTimeRequest traveltimeRequest
  ) {
    final double offRoadDistanceMeters = traveltimeRequest.offRoadDistanceMeters;
    final double offRoadWalkSpeedMps = 1.00; // m/s, off-road walk speed

    // Create a sample grid based on the SPT.
    long t1 = System.currentTimeMillis();
    Coordinate coordinateOrigin = spt.getAllStates().iterator().next().getVertex().getCoordinate();
    final double gridSizeMeters = traveltimeRequest.precisionMeters;
    final double cosLat = Math.cos(Math.toRadians(coordinateOrigin.y));
    double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
    double dX = dY / cosLat;

    SparseMatrixZSampleGrid<WTWD> sampleGrid = new SparseMatrixZSampleGrid<>(
      16,
      spt.getVertexCount(),
      dX,
      dY,
      coordinateOrigin
    );
    sampleSPT(
      spt,
      sampleGrid,
      gridSizeMeters,
      offRoadDistanceMeters,
      offRoadWalkSpeedMps,
      (int) traveltimeRequest.maxCutoff.getSeconds(),
      cosLat
    );

    long t2 = System.currentTimeMillis();
    LOG.info("Computed sampling in {}msec", (int) (t2 - t1));

    return sampleGrid;
  }

  /**
   * Sample a SPT using a SPTWalker and an AccumulativeGridSampler.
   */
  public static void sampleSPT(
    final ShortestPathTree<State, Edge, Vertex> spt,
    final ZSampleGrid<WTWD> sampleGrid,
    final double gridSizeMeters,
    final double offRoadDistanceMeters,
    final double offRoadWalkSpeedMps,
    final int maxTimeSec,
    final double cosLat
  ) {
    final AccumulativeMetric<WTWD> accMetric = new WTWDAccumulativeMetric(
      cosLat,
      offRoadDistanceMeters,
      offRoadWalkSpeedMps,
      gridSizeMeters
    );
    final AccumulativeGridSampler<WTWD> gridSampler = new AccumulativeGridSampler<>(
      sampleGrid,
      accMetric
    );

    // At which distance we split edges along the geometry during sampling.
    // For best results, this should be slighly lower than the grid size.
    double walkerSplitDistanceMeters = gridSizeMeters * 0.5;

    SPTVisitor visitor = new SampleGridSPTVisitor(maxTimeSec, gridSampler, offRoadWalkSpeedMps);
    new SPTWalker(spt).walk(visitor, walkerSplitDistanceMeters);
    gridSampler.close();
  }
}
