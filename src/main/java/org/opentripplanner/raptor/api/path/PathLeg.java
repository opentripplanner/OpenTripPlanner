package org.opentripplanner.raptor.api.path;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * A leg in a Raptor path. The legs are linked together from the first leg {@link AccessPathLeg}, to
 * the last leg {@link EgressPathLeg}. There must be at least one {@link TransitPathLeg}. Transit
 * legs can follow each* other or be connected by one {@link TransferPathLeg}. Note! Access and
 * egress path legs may contain more than one "OTP leg", but inside raptor these are threaded as one
 * leg; hence also just one leg returned by Raptor.
 * <p/>
 * This interface contain utility methods to _cast_ a leg into the concrete sub-type:
 * <pre>
 * if(leg.isTransitLeg()) {
 *     trip = leg.asTransitLeg().trip();
 * }
 * </pre>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface PathLeg<T extends RaptorTripSchedule> {
  /**
   * The time when the leg start/depart from the leg origin. For transit the time do NOT include
   * boardSlack.
   */
  int fromTime();

  /**
   * The stop place where the leg start/depart from.
   *
   * @throws IllegalArgumentException if leg does not start at a stop, like an access leg.
   */
  default int fromStop() {
    throw new IllegalStateException("Leg does not start fro a stop: " + this);
  }

  /**
   * The time when the leg end/arrive at the leg destination. For transit the time do NOT include
   * alight-slack.
   */
  int toTime();

  /**
   * The stop where the leg end/arrive at the leg destination.
   *
   * @throws IllegalArgumentException if leg does not end at a stop, like an egress leg.
   */
  default int toStop() {
    throw new IllegalStateException("Leg does not end at a stop: " + this);
  }

  /**
   * Number of seconds to travel this leg. This does not include slack/wait time.
   */
  default int duration() {
    return toTime() - fromTime();
  }

  /**
   * The computed generalized-cost for this path leg.
   * <p>
   * {@code -1} is returned if no cost exist.
   * <p>
   * The unit is centi-seconds (Raptor cost unit)
   */
  int c1();

  /**
   * The c1 for this leg plus all legs following it.
   * <p>
   * {@code -1} is returned if no cost is computed by raptor.
   * <p>
   * The unit is centi-seconds (Raptor cost unit)
   */
  default int c1Total() {
    if (c1() < 0) {
      return c1();
    }
    return stream().mapToInt(PathLeg::c1).sum();
  }

  /**
   * @return {@code true} if access leg, if not {@code false}.
   */
  default boolean isAccessLeg() {
    return false;
  }

  /**
   * Utility method performing a cast to {@link AccessPathLeg}, use with care:
   * <pre>
   * if(it.isAccessLeg()} {
   *     AccessPathLeg&lt;T&gt; transit = it.asAccessLeg();
   *     ...
   * </pre>
   */
  default AccessPathLeg<T> asAccessLeg() {
    return (AccessPathLeg<T>) this;
  }

  /**
   * @return {@code true} if transit leg, if not {@code false}.
   */
  default boolean isTransitLeg() {
    return false;
  }

  /**
   * Utility method performing a cast to {@link TransitPathLeg}, use with care:
   * <pre>
   * if(it.isTransitLeg()} {
   *     TransitPathLeg&lt;T&gt; transit = it.asTransitLeg();
   *     ...
   * </pre>
   */
  default TransitPathLeg<T> asTransitLeg() {
    return (TransitPathLeg<T>) this;
  }

  /**
   * @return {@code true} if transfer leg, if not {@code false}.
   */
  default boolean isTransferLeg() {
    return false;
  }

  /**
   * Utility method performing a cast to {@link TransferPathLeg}, use with care:
   * <pre>
   * if(it.isTransferLeg()} {
   *     TransferPathLeg&lt;T&gt; transfer = it.asTransferLeg();
   *     ...
   * </pre>
   */
  default TransferPathLeg<T> asTransferLeg() {
    return (TransferPathLeg<T>) this;
  }

  /**
   * @return {@code true} if egress leg, if not {@code false}.
   */
  default boolean isEgressLeg() {
    return false;
  }

  /**
   * Utility method performing a cast to {@link EgressPathLeg}, use with care:
   * <pre>
   * if(it.isEgressLeg()} {
   *     EgressPathLeg&lt;T&gt; egress = it.asEgressLeg();
   *     ...
   * </pre>
   */
  default EgressPathLeg<T> asEgressLeg() {
    return (EgressPathLeg<T>) this;
  }

  /**
   * <ul>
   * <li>An access leg is always followed by a transit leg.
   * <li>A transit leg can be followed by a new transit leg, an transfer leg or an egress leg.
   * <li>A transfer leg can only be followed by a transit leg.
   * <li>An egress leg is always the last leg and this method will throw a {@link UnsupportedOperationException}.
   * Use the {@link #isEgressLeg()} to check if the last leg is reached.
   * </ul>
   *
   * @return Next leg in path.
   */
  PathLeg<T> nextLeg();

  default String asString(int toStop) {
    return asString() + " ~ " + toStop;
  }

  default String asString() {
    return (
      TimeUtils.timeToStrCompact(fromTime()) +
      "-" +
      TimeUtils.timeToStrCompact(toTime()) +
      "(" +
      DurationUtils.durationToStr(duration()) +
      ")"
    );
  }

  /**
   * Return the next transit leg in the path after this one, if no more transit exist before
   * reaching the destination {@code null} is returned.
   */
  @Nullable
  default TransitPathLeg<T> nextTransitLeg() {
    PathLeg<T> leg = nextLeg();
    while (!leg.isEgressLeg()) {
      if (leg.isTransitLeg()) {
        return leg.asTransitLeg();
      }
      leg = leg.nextLeg();
    }
    return null;
  }

  default Stream<PathLeg<T>> stream() {
    return StreamSupport.stream(iterator().spliterator(), false);
  }

  default Iterable<PathLeg<T>> iterator() {
    return () ->
      new Iterator<>() {
        private PathLeg<T> currentLeg = PathLeg.this;

        @Override
        public boolean hasNext() {
          return currentLeg != null;
        }

        @Override
        public PathLeg<T> next() {
          var temp = currentLeg;
          currentLeg = currentLeg.isEgressLeg() ? null : currentLeg.nextLeg();
          return temp;
        }
      };
  }
}
