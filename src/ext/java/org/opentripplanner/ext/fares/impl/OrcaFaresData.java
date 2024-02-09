package org.opentripplanner.ext.fares.impl;

import java.util.Map;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;

/**
 * Class used to store all the fares for Sounder commuter rail.
 * Data comes from a Python script that parses SoundTransit's website.
 * A matrix or CSV parser would be a better approach to storing this data,
 * but a refactor is unneeded given the proximity of GTFS Fares V2 which will render this redundant.
 */
class OrcaFaresData {

  // Spaces have been removed from the route name because of inconsistencies in the WSF GTFS route dataset.
  public static Map<String, Map<FareType, Money>> washingtonStateFerriesFares = Map.ofEntries(
    sEntry("Seattle-BainbridgeIsland", 9.85f, 4.90f),
    sEntry("Seattle-Bremerton", 9.85f, 4.90f),
    sEntry("Mukilteo-Clinton", 6f, 3f),
    sEntry("Fauntleroy-VashonIsland", 6.50f, 3.25f),
    sEntry("Fauntleroy-Southworth", 7.70f, 3.85f),
    sEntry("Edmonds-Kingston", 9.85f, 4.90f),
    sEntry("PointDefiance-Tahlequah", 6.50f, 3.25f),
    sEntry("Anacortes-FridayHarbor", 15.85f, 7.90f),
    sEntry("Anacortes-LopezIsland", 15.85f, 7.90f),
    sEntry("Anacortes-OrcasIsland", 15.85f, 7.90f),
    sEntry("Anacortes-ShawIsland", 15.85f, 7.90f),
    sEntry("Coupeville-PortTownsend", 4.10f, 2.05f),
    sEntry("PortTownsend-Coupeville", 4.10f, 2.05f),
    sEntry("Southworth-VashonIsland", 6.50f, 3.25f)
  );

  private static Map.Entry<String, Map<FareType, Money>> sEntry(
    String name,
    float regularFare,
    float seniorFare
  ) {
    return Map.entry(
      name,
      Map.of(
        FareType.regular,
        Money.usDollars(regularFare),
        FareType.senior,
        Money.usDollars(seniorFare)
      )
    );
  }
}
