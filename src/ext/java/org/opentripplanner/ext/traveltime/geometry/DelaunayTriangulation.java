package org.opentripplanner.ext.traveltime.geometry;

/**
 * A Delaunay triangulation (adapted to isoline building).
 *
 * A simple interface returning a collection (an iterable) of DelaunayEdges. The interface is kept
 * minimal for isoline building purposes.
 *
 * @author laurent
 * @param <TZ> The value stored for each node.
 */
public interface DelaunayTriangulation<TZ> {
  int edgesCount();

  Iterable<? extends DelaunayEdge<TZ>> edges();
}
