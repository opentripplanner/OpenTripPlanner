package org.opentripplanner.raptor.api.model;

import static org.opentripplanner.raptor.api.model.RaptorConstants.SECONDS_IN_A_DAY;
import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.OtpNumberFormat;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;

/**
 * Encapsulate information about an access or egress path. We do not distinguish between
 * the access (origin to first stop) or egress (last stop to destination),
 * to Raptor - all these are the same thing.
 */
public interface RaptorAccessEgress {
  /**
   * <ul>
   *     <li>Access: The first stop in the journey, where the access path just arrived at.
   *     <li>Egress: Last stop before destination, hence not the arrival point, but the departure
   *     stop.
   * </ul>
   * The journey origin, destination and transit path board stop must be part of the context;
   * hence not a member attribute of this type.
   */
  int stop();

  /**
   * The generalized cost of this access/egress in centi-seconds. The value is used to compare with
   * riding transit, and will be one component of a full itinerary.
   * <p>
   * This method is called many times, so care needs to be taken that the value is stored, not
   * calculated for each invocation.
   * <p>
   * If this is {@link #isFree()}, then this method must return 0(zero).
   */
  int generalizedCost();

  /**
   * The time duration to walk or travel the path in seconds. This is not the entire duration from
   * the journey origin, but just:
   * <ul>
   *     <li>Access: journey origin to first stop.
   *     <li>Egress: last stop to journey destination.
   * </ul>
   */
  int durationInSeconds();

  /* TIME-DEPENDENT ACCESS/TRANSFER/EGRESS */
  // The methods below should be only overridden when an RaptorAccessEgress is only available at
  // specific times, such as flexible transit, TNC or shared vehicle schemes with limited opening
  // hours, not for regular access/transfer/egress.

  /**
   * Returns the earliest possible departure time for the path. Used Eg. in flex routing and TNC
   * when the access path can't start immediately, but have to wait for a vehicle arriving. Also DRT
   * systems or bike shares can have operation time limitations.
   * <p>
   * Returns {@link RaptorConstants#TIME_NOT_SET} if transfer
   * is not possible after the requested departure time.
   */
  int earliestDepartureTime(int requestedDepartureTime);

  /**
   * Returns the latest possible arrival time for the path. Used in DRT systems or bike shares
   * where they can have operation time limitations.
   * <p>
   * Returns {@link RaptorConstants#TIME_NOT_SET} if transfer
   * is not possible before the requested arrival time.
   */
  int latestArrivalTime(int requestedArrivalTime);

  /**
   * This method should return {@code true} if, and only if the instance have restricted
   * opening-hours.
   */
  boolean hasOpeningHours();

  /**
   * Return the opening hours in a short human-readable way for the departure at the origin. Do
   * not parse this, this should only be used for things like testing, debugging and logging.
   * <p>
   * This method return {@code null} if there is no opening hours, see {@link #hasOpeningHours()}.
   */
  @Nullable
  default String openingHoursToString() {
    if (!hasOpeningHours()) {
      return null;
    }
    // The earliest-departure-time(after 00:00) and latest-arrival-time(before edt+1d). This
    // assumes the access/egress is a continuous period without gaps withing 24 hours from the
    // opening. We ignore the access/egress duration. This is ok for test, debugging and logging.
    int edt = earliestDepartureTime(0);
    int lat = latestArrivalTime(edt + SECONDS_IN_A_DAY);

    if (edt == TIME_NOT_SET || lat == TIME_NOT_SET) {
      return "closed";
    }
    // Opening hours are specified for the departure, not arrival
    int ldt = lat - durationInSeconds();
    return "Open(" + TimeUtils.timeToStrCompact(edt) + " " + TimeUtils.timeToStrCompact(ldt) + ")";
  }

  /*
       ACCESS/TRANSFER/EGRESS PATH CONTAINING MULTIPLE LEGS

       The methods below should be only overridden when a RaptorAccessEgress contains information
       about public services, which were generated outside the RAPTOR algorithm. Examples of such
       schemes include flexible transit service and TNC. They should not be used for regular
       access/transfer/egress.
    */

  /**
   * Some services involving multiple legs are not handled by the RAPTOR algorithm and need to be
   * inserted into the algorithm at a specific place of the algorithm. The number-of-rides must be
   * accounted for in order to get the number of transfers correct. The number-of-transfers is part
   * of the criteria used to keep an optimal result.
   * <p>
   * Note! The number returned should include all "rides" in the access leg resulting in an extra
   * transfer, including boarding the first Raptor scheduled trip. There is no need to account for
   * riding your own bicycle or scooter, and a rental bike is debatable. The guideline is that if
   * there is a transfer involved that is equivalent to the "human cost" to a normal transit
   * transfer, then it should be counted. If not, you should account for it using the cost function
   * instead.
   * <p>
   * Examples/guidelines:
   * <p>
   * <pre>
   * Access/egress  | num-of-rides | Description
   *     walk       |      0       | Plain walking leg
   *  bicycle+walk  |      0       | Use bicycle to get to stop
   * rental-bicycle |      0       | Picking up the bike and returning it is is best
   *                |              | accounted using time and cost penalties, not transfers.
   *     taxi       |     0/1      | Currently 0 in OTP(car), but this is definitely discussable.
   *     flex       |      1       | Walking leg followed by a flex transit leg
   * walk-flex-walk |      1       | Walking , then flex transit and then walking again
   *   flex-flex    |      2       | Two flex transit legs after each other
   * </pre>
   * {@code flex} is used as a placeholder for any type of on-board public service.
   *
   * @return the number transfers including the first boarding in the RAPTOR algorithm.
   */
  default int numberOfRides() {
    return 0;
  }

  default boolean hasRides() {
    return numberOfRides() > 0;
  }

  /**
   * Is this {@link RaptorAccessEgress} is connected to the given {@code stop} directly by
   * <b>transit</b>? For access and egress paths we allow plugging in flexible transit and other
   * means of transport, which might include one or more legs onboard a vehicle. This method should
   * return {@code true} if the leg connecting to the given stop arrives `onBoard` a public
   * transport or riding another kind of service like a taxi.
   * <p>
   * This information is used to generate transfers from that stop to other stops only when this
   * method returns true.
   */
  default boolean stopReachedOnBoard() {
    return false;
  }

  /**
   * Is this {@link RaptorAccessEgress} is connected to the given {@code stop} directly by
   * <b>walking</b>(or other street mode)? This should be {@code true} if the access/egress
   * is NOT reached on-board.
   * @see #stopReachedOnBoard()
   */
  default boolean stopReachedByWalking() {
    return !stopReachedOnBoard();
  }

  /**
   * Is this access or egress without duration.
   * This commonly refers to:
   * An empty access where you board transit directly at the origin
   * An empty egress where you alight transit directly at the destination
   * @return true if the duration is 0;
   */
  default boolean isFree() {
    return durationInSeconds() == 0;
  }

  /** Call this from toString or {@link #asString(boolean, boolean, String)}*/
  default String defaultToString() {
    return asString(true, true, null);
  }

  /** Call this from toString or {@link #defaultToString()} */
  default String asString(boolean includeStop, boolean includeCost, @Nullable String summary) {
    StringBuilder buf = new StringBuilder();
    if (isFree()) {
      buf.append("Free");
    } else if (hasRides()) {
      buf.append(stopReachedOnBoard() ? "Flex" : "Flex+Walk");
    } else {
      // This is not always walking, but inside Raptor we do not care if this is
      // biking, walking or car - any on street is treated the same. So, for
      // short easy reading in Raptor tests we use "Walk" instead of "On-Street"
      // which would be more precise.
      buf.append("Walk");
    }
    buf.append(' ').append(DurationUtils.durationToStr(durationInSeconds()));
    if (includeCost && generalizedCost() > 0) {
      buf.append(' ').append(OtpNumberFormat.formatCostCenti(generalizedCost()));
    }
    if (hasRides()) {
      buf.append(' ').append(numberOfRides()).append('x');
    }
    if (hasOpeningHours()) {
      buf.append(' ').append(openingHoursToString());
    }
    if (summary != null) {
      buf.append(' ').append(summary);
    }
    if (includeStop) {
      buf.append(" ~ ").append(stop());
    }
    return buf.toString();
  }
}
