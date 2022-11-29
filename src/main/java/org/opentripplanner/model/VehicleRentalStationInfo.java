package org.opentripplanner.model;

import org.opentripplanner.street.model.vertex.VehicleRentalPlaceVertex;

/**
 * This is a response model class which holds data that will be serialized and returned to the
 * client. It is not used internally in routing.
 */
public class VehicleRentalStationInfo {

  public String id;
  public String name;
  public Double lat, lon;

  public VehicleRentalStationInfo(VehicleRentalPlaceVertex vertex) {
    id = vertex.getStation().getStationId();
    name = vertex.getDefaultName();
    lat = vertex.getLat();
    lon = vertex.getLon();
  }
}
