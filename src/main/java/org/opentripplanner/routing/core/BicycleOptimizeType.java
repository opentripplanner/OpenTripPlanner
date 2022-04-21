package org.opentripplanner.routing.core;

/**
 * When planning a bicycle route what should be optimized for.
 */
public enum BicycleOptimizeType {
  QUICK,/* the fastest trip */
  SAFE,
  FLAT,/* needs a rewrite */
  GREENWAYS,
  TRIANGLE,
}
