package org.opentripplanner.ext.restapi.model;

import java.util.List;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;

public class ApiVehicleRentalStation {

  public String id;
  public String name;
  public double x, y; //longitude, latitude
  public int bikesAvailable = Integer.MAX_VALUE;
  public int spacesAvailable = Integer.MAX_VALUE;
  public boolean allowDropoff = true;
  public boolean isFloatingBike = false;
  public boolean isCarStation = false;

  /**
   * List of compatible network names. Null (default) to be compatible with all.
   */
  public List<String> networks = null;

  /**
   * Whether this station has real-time data available currently. If no real-time data, users should
   * take bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity
   * divided by two.
   */
  public boolean realTimeData = true;

  public VehicleRentalStationUris rentalUris;
}
