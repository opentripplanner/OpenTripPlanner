package org.opentripplanner.routing.algorithm.mapping._support.model;

import java.util.Calendar;
import java.util.List;

/**
 * A Place is where a journey starts or ends, or a transit stop along the way.
 */
@Deprecated
public class ApiPlace {

  /**
   * For transit stops, the name of the stop.  For points of interest, the name of the POI.
   */
  public String name = null;

  /**
   * The ID of the stop. This is often something that users don't care about.
   */
  public String stopId = null;

  /**
   * The "code" of the stop. Depending on the transit agency, this is often something that users
   * care about.
   */
  public String stopCode = null;

  /**
   * The code or name identifying the quay/platform the vehicle will arrive at or depart from
   */
  public String platformCode = null;

  /**
   * The longitude of the place.
   */
  public Double lon = null;

  /**
   * The latitude of the place.
   */
  public Double lat = null;

  /**
   * The time the rider will arrive at the place.
   */
  public Calendar arrival = null;

  /**
   * The time the rider will depart the place.
   */
  public Calendar departure = null;

  public String zoneId;

  /**
   * For transit trips, the stop index (numbered from zero from the start of the trip
   */
  public Integer stopIndex;

  /**
   * For transit trips, the sequence number of the stop. Per GTFS, these numbers are increasing.
   */
  public Integer stopSequence;

  /**
   * Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit stop) Mostly used for better
   * localization of bike sharing and P+R station names
   */
  public ApiVertexType vertexType;

  /**
   * In case the vertex is of type Bike sharing station.
   */
  public String bikeShareId;

  /**
   * The vehicle rental networks that are available at this place.
   */
  public List<String> networks;

  /**
   * In case the vertex is of type VEHICLEPARKING.
   */
  public ApiVehicleParkingWithEntrance vehicleParking;
}
