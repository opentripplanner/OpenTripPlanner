package org.opentripplanner.routing.algorithm.filterchain;

/**
 * This enum is used to signal which part of a list an operation apply to. You may remove elements
 * from the HEAD or TAIL of the list. It may refer to one or more elements.
 */
public enum ListSection {
  /** The beginning of the list. */
  HEAD,

  /** The end of the list */
  TAIL,
}
