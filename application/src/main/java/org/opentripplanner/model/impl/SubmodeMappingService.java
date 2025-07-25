package org.opentripplanner.model.impl;

import java.util.Map;
import org.opentripplanner.model.FeedType;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Trips can come from both GTFS and NeTEx sources, and to make GTFS Route.type and NeTEx
 * Trip.submode be reachable in a unified manner, there is an optional configuration file
 * submode-mapping.csv which contains entries keyed with input feed type (GTFS or NeTEx)
 * and input label (Route.type in the case of GTFS and Trip.submode in the case of NeTEx),
 * and the output values of NeTEx submode (not used currently because the transmodel query
 * api side is not done yet), Replacement mode (no longer used after Joel's requested
 * changes), Original mode (no longer used after Joel's requested changes), GTFS replacement
 * mode (transit mode to use for route in GTFS query if all trips match the submode in NeTEx
 * source), GTFS replacement type (extended type to use for route in GTFS query if all trips
 * match the submode in NeTEx source).
 * <p>
 * In GTFS queries Route.mode and Route.type can be overriden by this logic.
 * <p>
 * An example submode-mapping.csv:
 * <code>
 * Input feed type,Input label,NeTEx submode,Replacement mode,Original mode,GTFS replacement mode,GTFS replacement type
 * NeTEx,replacementRailService,railReplacementBus,BUS,,BUS,714
 * </code>
 * My NeTEx feed source uses a funny nonstandard submode "replacementRailService" which I
 * map to a standard one here. BUS will end up in Trip.replacementMode.
 * Original mode is left unspecified, and will default to Trip.mode in the case of NeTEx.
 */
public class SubmodeMappingService {

  private final Map<SubmodeMappingMatcher, SubmodeMappingRow> map;

  public SubmodeMappingService(Map<SubmodeMappingMatcher, SubmodeMappingRow> map) {
    this.map = map;
  }

  public TransitMode findGtfsReplacementMode(String submode) {
    var row = map.get(new SubmodeMappingMatcher(FeedType.NETEX, submode));
    if (row != null) {
      return row.gtfsReplacementMode();
    }
    return null;
  }

  public Integer findGtfsReplacementType(String submode) {
    var row = map.get(new SubmodeMappingMatcher(FeedType.NETEX, submode));
    if (row != null) {
      return row.gtfsReplacementType();
    }
    return null;
  }
}
