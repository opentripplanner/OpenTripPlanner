package org.opentripplanner.ext.traveltime.geometry;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to fill-in a ZSampleGrid from a given loosely-defined set of sampling points.
 *
 * The process is customized by an "accumulative" metric which gives the behavior of cumulating
 * several values onto one sampling point.
 *
 * To use this class, create an instance giving an AccumulativeMetric implementation as parameter.
 * Then for each source sample, call "addSample" with the its TZ value. At the end, call close() to
 * close the sample grid (ie add grid node at the edge to make sure borders are correctly defined,
 * the definition of correct is left to the client).
 *
 * @author laurent
 */
public class AccumulativeGridSampler<TZ> {

  private static final Logger LOG = LoggerFactory.getLogger(AccumulativeGridSampler.class);

  private final AccumulativeMetric<TZ> metric;

  private final ZSampleGrid<TZ> sampleGrid;

  private boolean closed = false;

  /**
   * @param metric TZ data "behavior" and "metric".
   */
  public AccumulativeGridSampler(ZSampleGrid<TZ> sampleGrid, AccumulativeMetric<TZ> metric) {
    this.metric = metric;
    this.sampleGrid = sampleGrid;
  }

  public final void addSamplingPoint(Coordinate C0, TZ z, double offRoadSpeed) {
    if (closed) throw new IllegalStateException("Can't add a sample after closing.");
    int[] xy = sampleGrid.getLowerLeftIndex(C0);
    int x = xy[0];
    int y = xy[1];
    List<ZSamplePoint<TZ>> ABCD = List.of(
      sampleGrid.getOrCreate(x, y),
      sampleGrid.getOrCreate(x + 1, y),
      sampleGrid.getOrCreate(x, y + 1),
      sampleGrid.getOrCreate(x + 1, y + 1)
    );
    for (ZSamplePoint<TZ> P : ABCD) {
      Coordinate C = sampleGrid.getCoordinates(P);
      P.setZ(metric.cumulateSample(C0, C, z, P.getZ(), offRoadSpeed));
    }
  }

  /**
   * Surround all existing samples on the edge by a layer of closing samples.
   */
  public final void close() {
    if (closed) return;
    closed = true;
    List<ZSamplePoint<TZ>> processList = new ArrayList<>(sampleGrid.size());
    for (ZSamplePoint<TZ> A : sampleGrid) {
      processList.add(A);
    }
    int round = 0;
    int n = 0;
    while (!processList.isEmpty()) {
      List<ZSamplePoint<TZ>> newProcessList = new ArrayList<>(processList.size());
      for (ZSamplePoint<TZ> A : processList) {
        if (A.right() == null) {
          ZSamplePoint<TZ> B = closeSample(A.getX() + 1, A.getY());
          if (B != null) newProcessList.add(B);
          n++;
        }
        if (A.left() == null) {
          ZSamplePoint<TZ> B = closeSample(A.getX() - 1, A.getY());
          if (B != null) newProcessList.add(B);
          n++;
        }
        if (A.up() == null) {
          ZSamplePoint<TZ> B = closeSample(A.getX(), A.getY() + 1);
          if (B != null) newProcessList.add(B);
          n++;
        }
        if (A.down() == null) {
          ZSamplePoint<TZ> B = closeSample(A.getX(), A.getY() - 1);
          if (B != null) newProcessList.add(B);
          n++;
        }
      }
      processList = newProcessList;
      LOG.debug("Round {} : next process list {}", round, processList.size());
      round++;
    }
    LOG.info("Added {} closing samples to get a total of {}.", n, sampleGrid.size());
  }

  private ZSamplePoint<TZ> closeSample(int x, int y) {
    ZSamplePoint<TZ> A = sampleGrid.getOrCreate(x, y);
    boolean ok = metric.closeSample(A);
    if (ok) {
      return null;
    } else {
      return A;
    }
  }
}
