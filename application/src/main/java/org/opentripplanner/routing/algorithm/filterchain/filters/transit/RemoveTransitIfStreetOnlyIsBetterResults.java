package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import org.opentripplanner.framework.model.Cost;

/**
 * The RemoveTransitIfStreetOnlyIsBetter filter removes itineraries from a list of itineraries based on
 * a generalizedCostMaxLimit. Transit itineraries that have a higher generalized cost than the limit
 * will be filtered away. This results class is used as input for the PageCursor.
 */
public record RemoveTransitIfStreetOnlyIsBetterResults(Cost generalizedCostMaxLimit) {}
