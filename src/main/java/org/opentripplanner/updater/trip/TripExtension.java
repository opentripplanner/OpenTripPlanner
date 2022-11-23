package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import org.opentripplanner.GtfsRealtimeExtensions;
import org.opentripplanner.transit.model.organization.Agency;

public record TripExtension(
  String routeUrl,
  String agencyId,
  Integer routeType,
  String routeLongName
) {
  public boolean matchesAgencyId(Agency agency) {
    return agencyId != null && agencyId.equals(agency.getId().getId());
  }

  public int routeTypeWithFallback() {
    if (routeType == null) {
      //bus
      return 3;
    } else {
      return routeType;
    }
  }

  static TripExtension ofTripDescriptor(GtfsRealtime.TripDescriptor tripDescriptor) {
    if (tripDescriptor.hasExtension(GtfsRealtimeExtensions.tripDescriptor)) {
      var ext = tripDescriptor.getExtension(GtfsRealtimeExtensions.tripDescriptor);
      var url = ext.getRouteUrl();
      var agencyId = ext.getAgencyId();
      var routeType = ext.getRouteType();
      var routeName = ext.getRouteLongName();
      return new TripExtension(url, agencyId, routeType, routeName);
    } else {
      return new TripExtension(null, null, null, null);
    }
  }
}
