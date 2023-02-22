package org.opentripplanner.raptor.spi;

import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;

/**
 * This builder is used to build unknown path string. It used the {@link UnknownPath#toString()}
 * to do so. You can initialize the builder with a duration and number-of-transfers, then
 * creating as many 'toStrings' you like. This is useful in test were we expect a path with
 * the same number of transfers and with the same duration, but time-sifted to a specific
 * departure or arrival time.
 */
public class UnknownPathString {

  private final int duration;
  private final int numberOfTransfers;

  private UnknownPathString(int duration, int numberOfTransfers) {
    this.duration = duration;
    this.numberOfTransfers = numberOfTransfers;
  }

  public static UnknownPathString of(int duration, int numberOfTransfers) {
    return new UnknownPathString(duration, numberOfTransfers);
  }

  public static UnknownPathString of(String duration, int numberOfTransfers) {
    return of(DurationUtils.durationInSeconds(duration), numberOfTransfers);
  }

  public String arrivalAt(int arrivalTime) {
    return new UnknownPath<>(arrivalTime - duration, arrivalTime, numberOfTransfers).toString();
  }

  public String arrivalAt(String arrivalTime) {
    return arrivalAt(TimeUtils.time(arrivalTime));
  }

  public String departureAt(int departureTime) {
    return new UnknownPath<>(departureTime, departureTime + duration, numberOfTransfers).toString();
  }

  public String departureAt(String departureTime) {
    return departureAt(TimeUtils.time(departureTime));
  }
}
