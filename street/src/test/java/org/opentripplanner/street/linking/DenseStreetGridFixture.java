package org.opentripplanner.street.linking;

import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdgeBuilder;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * A synthetic street network approximating a dense city-centre grid cell of the edge spatial index
 * at ~60°N: a rectangular grid of short two-way streets plus a few "plazas" whose boundary nodes
 * are fully interconnected by long visibility-style edges (as area linking produces). Spans several
 * grid cells so linking queries straddle cell boundaries.
 */
class DenseStreetGridFixture {

  final Graph graph = new Graph();
  final List<StreetEdge> edges = new ArrayList<>();
  final double minLon;
  final double minLat;
  final double maxLon;
  final double maxLat;

  /**
   * @param columns          intersections in the east-west direction
   * @param rows             intersections in the north-south direction
   * @param spacingMeters    distance between neighbouring intersections
   * @param plazas           number of plazas to add
   * @param plazaNodes       boundary nodes per plaza (each plaza adds nodes*(nodes-1) edges)
   */
  DenseStreetGridFixture(
    double originLon,
    double originLat,
    int columns,
    int rows,
    double spacingMeters,
    int plazas,
    int plazaNodes
  ) {
    double dLat = SphericalDistanceLibrary.metersToDegrees(spacingMeters);
    double dLon = dLat / Math.cos(Math.toRadians(originLat));
    this.minLon = originLon;
    this.minLat = originLat;
    this.maxLon = originLon + (columns - 1) * dLon;
    this.maxLat = originLat + (rows - 1) * dLat;

    var grid = new IntersectionVertex[columns][rows];
    for (int c = 0; c < columns; c++) {
      for (int r = 0; r < rows; r++) {
        var v = intersectionVertex("g_" + c + "_" + r, originLat + r * dLat, originLon + c * dLon);
        grid[c][r] = v;
        graph.addVertex(v);
      }
    }
    for (int c = 0; c < columns; c++) {
      for (int r = 0; r < rows; r++) {
        if (c + 1 < columns) {
          twoWay(grid[c][r], grid[c + 1][r], StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        }
        if (r + 1 < rows) {
          // every fourth north-south street is car-only, so mode filtering matters
          var perm =
            c % 4 == 0
              ? StreetTraversalPermission.CAR
              : StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
          twoWay(grid[c][r], grid[c][r + 1], perm);
        }
      }
    }
    // plazas: a ring of boundary nodes inside one grid square, fully interconnected
    for (int p = 0; p < plazas; p++) {
      int c = 1 + ((p * 3) % Math.max(1, columns - 2));
      int r = 1 + ((p * 5) % Math.max(1, rows - 2));
      double cx = originLon + (c + 0.5) * dLon;
      double cy = originLat + (r + 0.5) * dLat;
      var nodes = new IntersectionVertex[plazaNodes];
      for (int i = 0; i < plazaNodes; i++) {
        double a = (2 * Math.PI * i) / plazaNodes;
        nodes[i] = intersectionVertex(
          "p_" + p + "_" + i,
          cy + 0.4 * dLat * Math.sin(a),
          cx + 0.4 * dLon * Math.cos(a)
        );
        graph.addVertex(nodes[i]);
      }
      for (int i = 0; i < plazaNodes; i++) {
        for (int j = 0; j < plazaNodes; j++) {
          if (i != j) {
            oneWay(nodes[i], nodes[j], StreetTraversalPermission.PEDESTRIAN);
          }
        }
      }
      // connect the plaza to the surrounding grid
      twoWay(nodes[0], grid[c + 1][r], StreetTraversalPermission.PEDESTRIAN);
    }
    graph.index();
  }

  private void twoWay(IntersectionVertex a, IntersectionVertex b, StreetTraversalPermission perm) {
    oneWay(a, b, perm);
    oneWay(b, a, perm);
  }

  private void oneWay(IntersectionVertex a, IntersectionVertex b, StreetTraversalPermission perm) {
    double meters = SphericalDistanceLibrary.distance(a.getCoordinate(), b.getCoordinate());
    edges.add(streetEdgeBuilder(a, b, meters, perm).buildAndConnect());
  }

  Coordinate randomPoint(java.util.Random random) {
    return new Coordinate(
      minLon + random.nextDouble() * (maxLon - minLon),
      minLat + random.nextDouble() * (maxLat - minLat)
    );
  }

  static double xscale(double lat) {
    return Math.cos(Math.toRadians(lat));
  }

  static org.locationtech.jts.geom.Point point(Coordinate c) {
    return GeometryUtils.getGeometryFactory().createPoint(c);
  }
}
