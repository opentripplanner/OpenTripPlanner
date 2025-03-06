package org.opentripplanner.updater.trip.gtfs;

import com.google.transit.realtime.GtfsRealtime;
import de.mfdz.MfdzRealtimeExtensions;
import java.util.Objects;

final class AddedRoute {

  //bus
  public static final int FALLBACK_ROUTE_TYPE = 3;
  private final String routeUrl;
  private final String agencyId;
  private final Integer routeType;
  private final String routeLongName;

  public AddedRoute(String routeUrl, String agencyId, Integer routeType, String routeLongName) {
    this.routeUrl = routeUrl;
    this.agencyId = agencyId;
    this.routeType = routeType;
    this.routeLongName = routeLongName;
  }

  /**
   * If the route type is not defined in {@see MfdzRealtimeExtensions} then we fall back to 3 (bus).
   */
  public int routeType() {
    return Objects.requireNonNullElse(routeType, FALLBACK_ROUTE_TYPE);
  }

  public String routeUrl() {
    return routeUrl;
  }

  public String agencyId() {
    return agencyId;
  }

  public String routeLongName() {
    return routeLongName;
  }

  static AddedRoute ofTripDescriptor(GtfsRealtime.TripDescriptor tripDescriptor) {
    if (tripDescriptor.hasExtension(MfdzRealtimeExtensions.tripDescriptor)) {
      var ext = tripDescriptor.getExtension(MfdzRealtimeExtensions.tripDescriptor);
      var url = ext.getRouteUrl();
      var agencyId = ext.getAgencyId();
      var routeType = ext.getRouteType();
      var routeName = ext.getRouteLongName();
      return new AddedRoute(url, agencyId, routeType, routeName);
    } else {
      return new AddedRoute(null, null, null, null);
    }
  }
}
