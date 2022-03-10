package org.opentripplanner.updater.vehicle_positions;

import com.google.common.collect.Multimap;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.vehicle_positions.RealtimeVehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for converting vehicle positions in memory to exportable ones, and associating each
 * position with a pattern.
 */
public class VehiclePositionPatternMatcher {

    private static final Logger LOG =
            LoggerFactory.getLogger(VehiclePositionPatternMatcher.class);

    /**
     * Set of trip IDs we've seen, so if we stop seeing them we know to remove them from the
     * pattern
     */
    private final Set<String> seenTripIds = new HashSet<>();

    private final Supplier<Multimap<String, TripPattern>> getPatternsForFeedId;
    private final Supplier<Map<FeedScopedId, Trip>> getTripForId;
    private final Supplier<Map<Trip, TripPattern>> getPatternForTrip;

    public VehiclePositionPatternMatcher(
            Supplier<Multimap<String, TripPattern>> getPatternsForFeedId,
            Supplier<Map<FeedScopedId, Trip>> getTripForId,
            Supplier<Map<Trip, TripPattern>> getPatternForTrip
    ) {
        this.getPatternsForFeedId = getPatternsForFeedId;
        this.getTripForId = getTripForId;
        this.getPatternForTrip = getPatternForTrip;
    }

    /**
     * Clear seen trip IDs
     */
    public void wipeSeenTripIds() {
        seenTripIds.clear();
    }

    /**
     * Iterates over a pattern's vehicle positions and removes all not seen in the seenTripIds set
     *
     * @param feedId FeedId whose pattern's should be "cleaned"
     */
    public void cleanPatternVehiclePositions(String feedId) {
        for (TripPattern pattern : getPatternsForFeedId.get().get(feedId)) {
            pattern.removeVehiclePositionIf(key -> !seenTripIds.contains(key));
        }
    }

    /**
     * Attempts to match each vehicle position to a pattern, then adds each to a pattern
     *
     * @param vehiclePositions List of vehicle positions to match to patterns
     * @param feedId           Feed id of vehicle positions to assist in pattern-matching
     */
    public void applyVehiclePositionUpdates(List<VehiclePosition> vehiclePositions, String feedId) {
        int numberOfMatches = 0;
        for (VehiclePosition vehiclePosition : vehiclePositions) {
            if (!vehiclePosition.hasTrip()) {
                LOG.warn("Realtime vehicle positions without trip IDs are not yet supported.");
                continue;
            }

            String tripId = vehiclePosition.getTrip().getTripId();
            Trip trip = getTripForId.get().get(new FeedScopedId(feedId, tripId));
            if (trip == null) {
                LOG.warn("Unable to find trip ID in feed '{}' for vehicle position with trip ID {}", feedId, tripId);
                continue;
            }

            TripPattern pattern = getPatternForTrip.get().get(trip);
            if (pattern == null) {
                LOG.warn(
                        "Unable to match OTP pattern ID for vehicle position with trip ID {}",
                        tripId
                );
                continue;
            }

            // Add trip to seen trip set
            seenTripIds.add(tripId);

            // Add position to pattern
            RealtimeVehiclePosition newPosition = parseVehiclePosition(
                    vehiclePosition,
                    pattern.getStops()
            );
            newPosition.patternId = pattern.getId().toString();

            pattern.addVehiclePosition(tripId, newPosition);
            numberOfMatches++;
        }

        if (!vehiclePositions.isEmpty() && numberOfMatches == 0) {
            LOG.error(
                    "Could not match any vehicle positions for feedId '{}'. Are you sure that the updater using the correct feedId?",
                    feedId
            );
        }
    }

    /**
     * Converts GtfsRealtime vehicle position to the OTP RealtimeVehiclePosition which can be used
     * by the API.
     *
     * @param vehiclePosition    GtfsRealtime vehicle position
     * @param stopsOnVehicleTrip Collection of stops method will try to match next arriving stop ID
     *                           to
     * @return OTP RealtimeVehiclePosition
     */
    private RealtimeVehiclePosition parseVehiclePosition(
            VehiclePosition vehiclePosition,
            List<StopLocation> stopsOnVehicleTrip
    ) {
        RealtimeVehiclePosition newPosition = new RealtimeVehiclePosition();
        if (vehiclePosition.hasPosition()) {
            var position = vehiclePosition.getPosition();
            newPosition.lat = position.getLatitude();
            newPosition.lon = position.getLongitude();
            newPosition.speed = position.getSpeed();
            newPosition.heading = position.getBearing();
        }

        if (vehiclePosition.hasVehicle()) {
            newPosition.label = vehiclePosition.getVehicle().getLabel();
            if (newPosition.label == null) {
                newPosition.label = vehiclePosition.getVehicle().getLicensePlate();
            }
            newPosition.vehicleId = vehiclePosition.getVehicle().getId();
        }

        if (vehiclePosition.hasCurrentStatus()) {
            newPosition.stopStatus = vehiclePosition.getCurrentStatus();
        }

        if (vehiclePosition.hasCongestionLevel()) {
            newPosition.congestionLevel = vehiclePosition.getCongestionLevel();
        }

        if (vehiclePosition.hasTimestamp()) {
            newPosition.seconds = vehiclePosition.getTimestamp();
        }

        if (vehiclePosition.hasStopId()) {
            List<StopLocation> matchedStops = stopsOnVehicleTrip
                    .stream()
                    .filter(stop -> stop.getId().getId().equals(vehiclePosition.getStopId()))
                    .collect(Collectors.toList());
            if (matchedStops.size() == 1) {
                newPosition.nextStop = matchedStops.get(0);
            }
        }

        if (vehiclePosition.hasCurrentStopSequence()) {
            newPosition.nextStopSequenceId = vehiclePosition.getCurrentStopSequence();
        }

        return newPosition;
    }
}
