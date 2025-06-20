package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;

/**
 * Create a valid Agency, Route and Trip
 */
public class GtfsTestData {

  final Agency agency = new Agency();
  final Route route = new Route();
  final Route route_2 = new Route();
  final Trip trip = new Trip();
  final Trip trip_2 = new Trip();
  final Stop stop = new Stop();
  final Stop stop_2 = new Stop();
  final Stop stop_3 = new Stop();
  final Stop stop_4 = new Stop();
  final Stop stop_5 = new Stop();

  {
    agency.setId("F:A1");
    agency.setName("Agency Name");
    agency.setTimezone("Europe/Oslo");

    route.setId(new AgencyAndId("F", "R1"));
    route.setAgency(agency);
    route.setType(3);
    route.setShortName("R1");

    route_2.setId(new AgencyAndId("F", "R2"));
    route_2.setAgency(agency);
    route_2.setType(3);
    route_2.setShortName("R2");

    trip.setId(new AgencyAndId("F", "T1"));
    trip.setRoute(route);
    trip.setServiceId(new AgencyAndId("F", "SID1"));

    trip_2.setId(new AgencyAndId("F", "T2"));
    trip_2.setRoute(route_2);
    trip_2.setServiceId(new AgencyAndId("F", "SID1"));

    stop.setId(new AgencyAndId("F", "S1"));
    stop_2.setId(new AgencyAndId("F", "S2"));
    stop_3.setId(new AgencyAndId("F", "S3"));
    stop_4.setId(new AgencyAndId("F", "S4"));
    stop_5.setId(new AgencyAndId("F", "S5"));
  }
}
