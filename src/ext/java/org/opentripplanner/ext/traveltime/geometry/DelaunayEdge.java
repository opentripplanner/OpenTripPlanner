package org.opentripplanner.ext.traveltime.geometry;

/**
 * A DelaunayEdge is a directed segment between two DelaunayPoints of the triangulation.
 * <p>
 * The interface is kept minimal for isoline building purposes.
 *
 * @author laurent
 */
interface DelaunayEdge<TZ> {
  /**
   * @return The start point (node) of this edge.
   */
  DelaunayPoint<TZ> getA();

  /**
   * @return The end point (node) of this edge.
   */
  DelaunayPoint<TZ> getB();

  /**
   * @param ccw true (CCW) for A->B left edge, false (CW) for right edge.
   * @return The edge starting at B, going right or left.
   */
  DelaunayEdge<TZ> getEdge1(boolean ccw);

  /**
   * @param ccw For same value of ccw, will return the same side as getEdge1().
   * @return The edge starting at A, going right or left.
   */
  DelaunayEdge<TZ> getEdge2(boolean ccw);

  /**
   * HACK. This should not be here really. But with Java, attaching some user value to an object
   * rely on another level of indirection and costly maps/arrays. Exposing this flag directly here
   * saves *lots* of processing time. TODO Is there a better way to do that?
   *
   * @return The flag set by setProcessed.
   */
  boolean isProcessed();

  /**
   *
   */
  void setProcessed(boolean processed);
}
