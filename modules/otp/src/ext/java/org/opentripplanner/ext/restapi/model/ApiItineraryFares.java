package org.opentripplanner.ext.restapi.model;

import java.util.List;
import java.util.Map;

/**
 * @param fare              The Fares V1 fares.
 * @param details           The Fares V1 fare components.
 * @param coveringItinerary The Fares V2 products that are valid for the entire Itinerary.
 * @param legProducts       The Fares V2 products that cover only parts of the legs of the
 *                          itinerary, ie. the customer has to buy more than one ticket.
 */
public record ApiItineraryFares(
  Map<String, ApiMoney> fare,
  Map<String, List<ApiFareComponent>> details,
  List<ApiFareProduct> coveringItinerary,
  List<ApiLegProducts> legProducts
) {}
