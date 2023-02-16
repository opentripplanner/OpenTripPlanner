package org.opentripplanner.api.model;

import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;

/**
 * This is a response model class which holds data that will be serialized and returned to the
 * client. It is not used internally in routing.
 */
public class ApiRentalStation {

  public String id;
  public String name;
  public Double lat, lon;

  public ApiRentalStation(VehicleRentalPlaceVertex vertex) {
    id = vertex.getStation().getStationId();
    name = vertex.getDefaultName();
    lat = vertex.getLat();
    lon = vertex.getLon();
  }
}
