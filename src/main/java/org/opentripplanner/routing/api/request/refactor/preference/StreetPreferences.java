package org.opentripplanner.routing.api.request.refactor.preference;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.api.request.StreetMode;

// Direct street search
public class StreetPreferences {
  /**
   * This is the maximum duration for access/egress street searches. This is a performance limit and
   * should therefore be set high. Results close to the limit are not guaranteed to be optimal.
   * Use filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  Duration maxAccessEgressDuration = Duration.ofMinutes(45);
  /**
   * Override the settings in maxAccessEgressDuration for specific street modes. This is done
   * because some street modes searches are much more resource intensive than others.
   */
  Map<StreetMode, Duration> maxAccessEgressDurationForMode = new HashMap<>();
  /**
   * This is the maximum duration for a direct street search. This is a performance limit and should
   * therefore be set high. Results close to the limit are not guaranteed to be optimal.
   * Use filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  Duration maxDirectStreetDuration = Duration.ofHours(4);
  /**
   * Override the settings in maxDirectStreetDuration for specific street modes. This is done
   * because some street modes searches are much more resource intensive than others.
   */
  Map<StreetMode, Duration> maxDirectStreetDurationForMode = new HashMap<>();
  /** Multiplicative factor on expected turning time. */
  double turnReluctance = 1.0;
  /**
   * How long does it take to get an elevator, on average (actually, it probably should be a bit
   * *more* than average, to prevent optimistic trips)? Setting it to "seems like forever," while
   * accurate, will probably prevent OTP from working correctly.
   */
  // TODO: how long does it /really/ take to get an elevator?
  int elevatorBoardTime = 90;
  /** What is the cost of boarding an elevator? */
  int elevatorBoardCost = 90;
  /** How long does it take to advance one floor on an elevator? */
  int elevatorHopTime = 20;
  /**
   * Which path comparator to use
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
   */
  @Deprecated
  String pathComparator = null;
}
