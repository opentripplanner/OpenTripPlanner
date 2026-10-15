package org.opentripplanner.street.model.edge;

public enum NameType {
  /**
   * The name is a derived (generated) from the properties of the edge. In walk steps the name will
   * have a low priority.
   */
  DERIVED,
  /**
   * The name indicates a real entity in the street network and has a high priority in the walk
   * step generation.
   */
  REAL,
}
