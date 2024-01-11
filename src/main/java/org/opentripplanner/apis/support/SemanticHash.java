package org.opentripplanner.apis.support;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

@SuppressWarnings("UnstableApiUsage")
public class SemanticHash {

  /**
   * Static-only utils
   */
  private SemanticHash() {}

  /**
   * In most cases we want to use identity equality for Trips. However, in some cases we want a way
   * to consistently identify trips across versions of a GTFS feed, when the feed publisher cannot
   * ensure stable trip IDs. Therefore we define some additional hash functions. Hash collisions are
   * theoretically possible, so these identifiers should only be used to detect when two trips are
   * the same with a high degree of probability. An example application is avoiding double-booking
   * of a particular bus trip for school field trips. Using Murmur hash function. see
   * <a href="http://programmers.stackexchange.com/a/145633">here</a> for comparison.
   *
   * @param trip a trip object within this pattern, or null to hash the pattern itself independent
   *             any specific trip.
   * @return the semantic hash of a Trip in this pattern as a printable String.
   * <p>
   * TODO deal with frequency-based trips
   */
  public static String forTripPattern(TripPattern tripPattern, Trip trip) {
    HashFunction murmur = Hashing.murmur3_32();
    BaseEncoding encoder = BaseEncoding.base64Url().omitPadding();
    StringBuilder sb = new StringBuilder(50);
    sb.append(encoder.encode(forStopPattern(tripPattern, murmur).asBytes()));
    if (trip != null) {
      TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes == null) {
        return null;
      }
      sb.append(':');
      sb.append(encoder.encode(forTripTimes(tripTimes, murmur).asBytes()));
    }
    return sb.toString();
  }

  /**
   * In most cases we want to use identity equality for StopPatterns. There is a single StopPattern
   * instance for each semantic StopPattern, and we don't want to calculate complicated hashes or
   * equality values during normal execution. However, in some cases we want a way to consistently
   * identify trips across versions of a GTFS feed, when the feed publisher cannot ensure stable
   * trip IDs. Therefore we define some additional hash functions.
   */
  private static HashCode forStopPattern(TripPattern tripPattern, HashFunction hashFunction) {
    Hasher hasher = hashFunction.newHasher();
    var stops = tripPattern.getStops();
    int size = stops.size();
    for (StopLocation stop : stops) {
      // Truncate the lat and lon to 6 decimal places in case they move slightly between
      // feed versions
      hasher.putLong((long) (stop.getLat() * 1000000));
      hasher.putLong((long) (stop.getLon() * 1000000));
    }
    // Use hops rather than stops because boarding at stop 0 and alight at last stop are
    // not important and have changed between OTP versions.
    for (int hop = 0; hop < size - 1; hop++) {
      hasher.putInt(tripPattern.getBoardType(hop).ordinal());
      hasher.putInt(tripPattern.getAlightType(hop + 1).ordinal());
    }
    return hasher.hash();
  }

  /**
   * Hash the scheduled arrival/departure times. Used in creating stable IDs for trips across GTFS
   * feed versions. Use hops rather than stops because:
   * <ol>
   * <li>arrival at stop zero and departure from last stop are irrelevant</li>
   * <li>this hash function needs to stay stable when users switch from 0.10.x to 1.0</li>
   * </ol>
   */
  private static HashCode forTripTimes(TripTimes tripTimes, final HashFunction hashFunction) {
    final Hasher hasher = hashFunction.newHasher();
    for (int hop = 0; hop < tripTimes.getNumStops() - 1; hop++) {
      hasher.putInt(tripTimes.getScheduledDepartureTime(hop));
      hasher.putInt(tripTimes.getScheduledArrivalTime(hop + 1));
    }
    return hasher.hash();
  }
}
