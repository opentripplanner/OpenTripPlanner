package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import org.opentripplanner.routing.api.request.request.filter.VehicleParkingFilterRequest;

/**
 * Class that stores information about what kind of parking lots should be used for Park & Ride
 * and Bike & Ride searches.
 */
public class VehicleParkingRequest implements Cloneable, Serializable {

  private VehicleParkingFilterRequest filter = VehicleParkingFilterRequest.empty();
  private VehicleParkingFilterRequest preferred = VehicleParkingFilterRequest.empty();
  private int unpreferredTagCost = 5 * 60;

  private boolean useAvailabilityInformation = false;

  public void setFilter(VehicleParkingFilterRequest filter) {
    this.filter = filter;
  }

  public void setPreferred(VehicleParkingFilterRequest filter) {
    this.preferred = filter;
  }

  /**
   * Which vehicle parking tags are preferred. Vehicle parking facilities that don't have one of these
   * tags receive an extra cost.
   * <p>
   * This is useful if you want to use certain kind of facilities, like lockers for expensive e-bikes.
   */
  public VehicleParkingFilterRequest preferred() {
    return this.preferred;
  }

  public void setUnpreferredCost(int cost) {
    unpreferredTagCost = cost;
  }

  public int unpreferredCost() {
    return unpreferredTagCost;
  }

  /**
   * If realtime availability data should be used when deciding af a parking facility should be
   * used.
   */
  public void setUseAvailabilityInformation(boolean b) {
    useAvailabilityInformation = b;
  }

  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  public VehicleParkingRequest clone() {
    try {
      return (VehicleParkingRequest) super.clone();
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public VehicleParkingFilterRequest filter() {
    return filter;
  }
}
