package org.opentripplanner.graph_builder.module.osm;

/**
 * Record that holds forward and back safety factors for cycling or walking.
 */
public record SafetyFeatures(Double forward, Double back) {}
