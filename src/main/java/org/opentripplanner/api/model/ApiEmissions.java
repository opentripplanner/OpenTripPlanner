package org.opentripplanner.api.model;

import org.opentripplanner.framework.model.Grams;

/**
 * The emissions of an Itinerary
 */
public record ApiEmissions(
  /**
   * The carbon dioxide emissions of the itinerary in grams.
   */
  Grams co2
) {}
