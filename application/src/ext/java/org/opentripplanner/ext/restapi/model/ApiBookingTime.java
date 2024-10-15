package org.opentripplanner.ext.restapi.model;

import java.io.Serializable;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * Represents either an earliest or latest time a trip can be booked relative to the departure day
 * of the trip.
 */
public class ApiBookingTime implements Serializable {

  /**
   * The latest time at which the trip must be booked.
   * <p>
   * Unit: seconds since midnight
   */
  public final int time;

  /**
   * How many days in advance this trip must be booked.
   */
  public final int daysPrior;

  public ApiBookingTime(int time, int daysPrior) {
    this.time = time;
    this.daysPrior = daysPrior;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addNum("time", time).addNum("daysPrior", time).toString();
  }
}
