package org.opentripplanner.routing.core;

/**
 * When planning a bicycle route what should be optimized for. Optimize types are basically
 * combined presets of routing parameters, except for triangle.
 */
public enum BicycleOptimizeType {
  QUICK,/* the fastest trip */
  SAFE,
  FLAT,/* needs a rewrite */
  GREENWAYS,
  TRIANGLE,
}
