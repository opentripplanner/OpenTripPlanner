package org.opentripplanner.ext.flex;

import org.opentripplanner.framework.time.DurationUtils;

/**
 * This value-object contains the durations for a Flex access or egress path. The path may also
 * contain access and egress "parts" - a walk part in the beginning and/or at the end. It has method
 * for mapping between the flex-trip-times and the router(OTP) time.
 * <p>
 * If the {@code access/egress} is empty the duration is zero. The flex-trip duration must be at
 * least 1 second. This is just a sanity check, there might be bussiness rules elsewhere restricting
 * this futher.
 *
 * @param access The duration of the walking in the beginning of the Flex path. Should be zero or a
 *               positive number. Zero means that the path start with the flex trip, there is no
 *               access.
 * @param trip   The duration of the flex ride. Should be zero or a positive number (zero is a valid
 *               value since timetables have usually a 1-minute resolution: it is then possible to
 *               have a 0-second trip between 2 stops).
 * @param egress The duration of the walking at the end of the Flex path. Should be zero or a
 *               positive number. Zero means that the path ends after the flex trip, there is no
 *               egress.
 * @param offset The flex trip times is using service-time(noon minus 12 hours) as "midnight", the
 *               {@code offset} is the duration from router midnight to service-time midnight. This
 *               is used to handle day-light-saving-time adjustments.
 */
public record FlexPathDurations(int access, int trip, int egress, int offset) {
  public FlexPathDurations {
    if (access < 0) {
      throw new IllegalArgumentException("The access duration must be 0 or a positive number.");
    }
    if (trip < 0) {
      throw new IllegalArgumentException("The trip duration must be 0 or a positive number.");
    }
    if (egress < 0) {
      throw new IllegalArgumentException("The egress duration must be 0 or a positive number.");
    }
  }

  /**
   * The total duration of the flex access/egress path
   */
  public int total() {
    return access + trip + egress;
  }

  public int mapToFlexTripDepartureTime(int routerDepartureTime) {
    return toFlexTime(routerDepartureTime + access);
  }

  public int mapToRouterDepartureTime(int flexTripDepartureTime) {
    return toRouterTime(flexTripDepartureTime - access);
  }

  public int mapToFlexTripArrivalTime(int routerArrivalTime) {
    return toFlexTime(routerArrivalTime - egress);
  }

  public int mapToRouterArrivalTime(int flexTripArrivalTime) {
    return toRouterTime(flexTripArrivalTime + egress);
  }

  @Override
  public String toString() {
    var buf = new StringBuilder("(");
    buf.append(DurationUtils.durationToStr(access)).append(" + ");
    buf.append(DurationUtils.durationToStr(trip)).append(" + ");
    buf.append(DurationUtils.durationToStr(egress));
    if (offset != 0) {
      buf.append(", offset: ").append(DurationUtils.durationToStr(offset));
    }
    return buf.append(")").toString();
  }

  private int toFlexTime(int routerTime) {
    return routerTime - offset;
  }

  private int toRouterTime(int flexTripTime) {
    return flexTripTime + offset;
  }
}
