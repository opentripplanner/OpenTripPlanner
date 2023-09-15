package org.opentripplanner.ext.traveltime.spt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Walk over a SPT tree to geometrically visit all nodes and edge geometry. For each geometry longer
 * than the provided base length d0, split it in several steps of equal length and shorter than d0.
 * For each walk step call the visitor callback.
 *
 * @author laurent
 */
public class SPTWalker {

  private static final Logger LOG = LoggerFactory.getLogger(SPTWalker.class);

  private final ShortestPathTree<State, Edge, Vertex> spt;

  public SPTWalker(ShortestPathTree<State, Edge, Vertex> spt) {
    this.spt = spt;
  }

  /**
   * Walk over a SPT. Call a visitor for each visited point.
   */
  public void walk(SPTVisitor visitor, double d0) {
    int nTotal = 0, nSkippedDupEdge = 0, nSkippedNoGeometry = 0;
    Collection<? extends State> allStates = spt.getAllStates();
    Set<Vertex> allVertices = new HashSet<>(spt.getVertexCount());
    for (State s : allStates) {
      allVertices.add(s.getVertex());
    }
    Set<Edge> processedEdges = new HashSet<>(allVertices.size());
    for (Vertex v : allVertices) {
      State s0 = spt.getState(v);
      if (s0 == null || !s0.isFinal()) continue;
      for (Edge e : s0.getVertex().getIncoming()) {
        // Take only street
        if (e != null && visitor.accept(e)) {
          State s1 = spt.getState(e.getFromVertex());
          if (s1 == null || !s1.isFinal()) continue;
          if (e.getFromVertex() != null && e.getToVertex() != null) {
            // Hack alert: e.hashCode() throw NPE
            if (processedEdges.contains(e)) {
              nSkippedDupEdge++;
              continue;
            }
            processedEdges.add(e);
          }
          Vertex vx0 = s0.getVertex();
          Vertex vx1 = s1.getVertex();
          LineString lineString = e.getGeometry();
          if (lineString == null) {
            nSkippedNoGeometry++;
            continue;
          }

          // Compute speed along edge
          double speedAlongEdge = s0.getPreferences().walk().speed();
          if (e instanceof StreetEdge se) {
            /*
             * Compute effective speed, taking into account end state mode (car, bike,
             * walk...) and edge properties (car max speed, slope, etc...)
             */
            TraverseMode mode = s0.currentMode();
            speedAlongEdge = se.calculateSpeed(s0.getPreferences(), mode, s0.isBackWalkingBike());
            if (mode != TraverseMode.CAR) {
              speedAlongEdge *= se.getEffectiveBikeDistance() / se.getDistanceMeters();
            }
            double avgSpeed =
              se.getDistanceMeters() / Math.abs(s0.getTimeSeconds() - s1.getTimeSeconds());
            if (avgSpeed < 1e-10) {
              avgSpeed = 1e-10;
            }
            /*
             * We can't go faster than the average speed on the edge. We can go slower
             * however, that simply means that one end vertice has a time higher than
             * the other end vertice + time to traverse the edge (can happen due to
             * max walk clamping).
             */
            if (speedAlongEdge > avgSpeed) speedAlongEdge = avgSpeed;
          }

          // Length of linestring
          double lineStringLen = SphericalDistanceLibrary.fastLength(lineString);
          visitor.visit(e, vx0.getCoordinate(), s0, s1, 0.0, lineStringLen, speedAlongEdge);
          visitor.visit(e, vx1.getCoordinate(), s0, s1, lineStringLen, 0.0, speedAlongEdge);
          nTotal += 2;
          Coordinate[] pList = lineString.getCoordinates();
          boolean reverse = vx1.getCoordinate().equals(pList[0]);
          // Split the linestring in nSteps
          if (lineStringLen > d0) {
            int nSteps = (int) Math.floor(lineStringLen / d0) + 1; // Number of steps
            double stepLen = lineStringLen / nSteps; // Length of step
            double startLen = 0; // Distance at start of current seg
            double curLen = stepLen; // Distance cursor
            int ns = 1;
            for (int i = 0; i < pList.length - 1; i++) {
              Coordinate p0 = pList[i];
              Coordinate p1 = pList[i + 1];
              double segLen = SphericalDistanceLibrary.fastDistance(p0, p1);
              while (curLen - startLen < segLen) {
                double k = (curLen - startLen) / segLen;
                Coordinate p = new Coordinate(p0.x * (1 - k) + p1.x * k, p0.y * (1 - k) + p1.y * k);
                visitor.visit(
                  e,
                  p,
                  reverse ? s1 : s0,
                  reverse ? s0 : s1,
                  curLen,
                  lineStringLen - curLen,
                  speedAlongEdge
                );
                nTotal++;
                curLen += stepLen;
                ns++;
              }
              startLen += segLen;
              if (ns >= nSteps) break;
            }
          }
        }
      }
    }
    LOG.info(
      "SPTWalker: Generated {} points ({} dup edges, {} no geometry) from {} vertices / {} states.",
      nTotal,
      nSkippedDupEdge,
      nSkippedNoGeometry,
      allVertices.size(),
      allStates.size()
    );
  }
}
