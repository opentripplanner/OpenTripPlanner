package org.opentripplanner.graph_builder.module.osm;

enum LinearBarrierNodeType {
  /**
   * Create a split vertex for the node on the linear barrier. Used when the barrier runs along
   * an area.
   */
  SPLIT,
  /**
   * Create a barrier vertex for the node on the linear barrier. Used when the barrier cuts through
   * a linear highway.
   */
  BARRIER_VERTEX,
}
