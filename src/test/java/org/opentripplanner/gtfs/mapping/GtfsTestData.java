package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

/**
 * Create a valid Agency, Route and Trip
 */
public class GtfsTestData {

  final Agency agency = new Agency();
  final Route route = new Route();
  final Trip trip = new Trip();

  {
    agency.setId("F:A1");
    agency.setName("Agency Name");
    agency.setTimezone("Europe/Oslo");

    route.setId(new AgencyAndId("F", "R1"));
    route.setAgency(agency);
    route.setType(3);
    route.setShortName("R1");

    trip.setId(new AgencyAndId("F", "T1"));
    trip.setRoute(route);
  }
}
