package org.opentripplanner.api.model;

/**
 * The emissions of an Itinerary
 */
public record ApiEmissions(
  /**
   * The carbon dioxide emissions of the itinerary in grams.
   */
  Double co2grams
) {}
