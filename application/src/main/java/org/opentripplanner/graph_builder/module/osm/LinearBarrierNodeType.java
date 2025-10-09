package org.opentripplanner.graph_builder.module.osm;

enum LinearBarrierNodeType {
  /**
   * Create a split vertex for the node on the linear barrier. Used when the barrier runs along
   * an area.
   */
  SPLIT,
  /**
   * Create a normal vertex for the node on the linear barrier with an issue generated.
   * Used when the barrier cuts through a linear highway.
   */
  NORMAL,
}
