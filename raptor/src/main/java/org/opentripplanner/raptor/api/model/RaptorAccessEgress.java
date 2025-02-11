package org.opentripplanner.raptor.api.model;

import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Encapsulate information about an access or egress path. We do not distinguish between
 * the access (origin to first stop) or egress (last stop to destination),
 * to Raptor - all these are the same thing.
 */
public interface RaptorAccessEgress {
  /**
   * Raptor may decorate access/egress passed into Raptor. Use this method to get the original
   * instance of a given {@code type} type passed into Raptor. The first element matching the the
   * given {@code type} in the chain of delegates (see {@link AbstractAccessEgressDecorator}) is
   * returned.
   * <p>
   * This method is primarily for use outside Raptor to get the base access-egress instant to
   * access the EXTENDED state of the base type - state not part of the Raptor interface. This is
   * useful in the caller to get access to additional information stored in the base type.
   * <p>
   * Be careful, calling methods part of the {@link RaptorAccessEgress} interface on the returned
   * value will no longer be decorated - not be the value used by Raptor.
   *
   * @throws IllegalStateException if the given {@code parentType} does not exist in the chain
   *  of delegates including the first and last element.
   */
  default <T extends RaptorAccessEgress> Optional<T> findOriginal(Class<T> type) {
    return AbstractAccessEgressDecorator.findType(this, type);
  }

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
  int c1();

  /**
   * The time duration to walk or travel the path in seconds. This is not the entire duration from
   * the journey origin, but just:
   * <ul>
   *     <li>Access: journey origin to first stop.
   *     <li>Egress: last stop to journey destination.
   * </ul>
   */
  int durationInSeconds();

  /**
   * Raptor can add an optional time-penalty to a access/egress to make it less favourable compared
   * with other access/egress/transit options (paths). The penalty is a virtual extra duration of
   * time added inside Raptor when comparing time. The penalty does not propagate the c1 or c2 cost
   * values. This feature is useful when you want to limit the access/egress and the access/egress
   * is FASTER than the preferred option.
   * <p>
   * For example, for Park&Ride, driving all the way to the
   * destination is very often the best option when looking at the time criteria. When an
   * increasing time-penalty is applied to a car access/egress, then driving become less
   * favorable. This also improves performance, since we usually add a very high cost to
   * driving - making all park&ride access legs optimal - forcing Raptor to compute a path for
   * every option. The short drives are optimal on cost, and the long are optimal on time. In the
   * case of park&ride the time-penalty enables Raptor to choose one of the shortest access/egress
   * paths over the longer ones.
   * <p>
   * Another example is FLEX, where we in many use-cases want regular transit to win if there is
   * an offer. Only in the case where the FLEX is the only solution we want it to be presented.
   * To achieve this, we must add an extra duration to the time of the FLEX access/egress - it does
   * not help to just add extra cost - which makes both FLEX optimal on time and transit optimal on
   * cost. Keeping a large number of optimal access paths has a negative impact on performance as well.
   * <p>
   *
   * The unit is seconds and the default value is {@link RaptorConstants#TIME_NOT_SET}.
   */
  default int timePenalty() {
    return RaptorConstants.TIME_NOT_SET;
  }

  default boolean hasTimePenalty() {
    return timePenalty() != RaptorConstants.TIME_NOT_SET;
  }

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
   * This method should return {@code true} if, and only if the instance has restricted
   * opening-hours.
   */
  boolean hasOpeningHours();

  /**
   * Return the opening hours in a short human-readable way for the departure at the origin. Do
   * not parse this. This should only be used for things like testing, debugging and logging.
   * <p>
   * This method return {@code null} if there are no opening hours, see {@link #hasOpeningHours()}.
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
    int lat = latestArrivalTime(edt + (int) ChronoUnit.DAYS.getDuration().toSeconds());

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
    if (includeCost && c1() > 0) {
      buf.append(' ').append(RaptorValueFormatter.formatC1(c1()));
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
