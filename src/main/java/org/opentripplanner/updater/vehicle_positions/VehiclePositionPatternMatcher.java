package org.opentripplanner.updater.vehicle_positions;

import com.google.common.collect.Multimap;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for converting vehicle positions in memory to exportable ones, and associating each
 * position with a pattern.
 */
public class VehiclePositionPatternMatcher {

    private static final Logger LOG =
            LoggerFactory.getLogger(VehiclePositionPatternMatcher.class);

    private final RealtimeVehiclePositionService service;

    private final Supplier<Map<FeedScopedId, Trip>> getTripForId;
    private final Supplier<Map<Trip, TripPattern>> getPatternForTrip;

    public VehiclePositionPatternMatcher(
            Supplier<Map<FeedScopedId, Trip>> getTripForId,
            Supplier<Map<Trip, TripPattern>> getPatternForTrip,
            RealtimeVehiclePositionService service
    ) {
        this.getTripForId = getTripForId;
        this.getPatternForTrip = getPatternForTrip;
        this.service = service;
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
                LOG.warn(
                        "Unable to find trip ID in feed '{}' for vehicle position with trip ID {}",
                        feedId, tripId
                );
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

            // Add position to pattern
            var newPosition = parseVehiclePosition(
                    vehiclePosition,
                    pattern.getStops()
            );

            service.setVehiclePositions(pattern, List.of(newPosition));
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
     */
    public static RealtimeVehiclePosition parseVehiclePosition(
            VehiclePosition vehiclePosition,
            List<StopLocation> stopsOnVehicleTrip
    ) {
        var newPosition = RealtimeVehiclePosition.builder();

        if (vehiclePosition.hasPosition()) {
            var position = vehiclePosition.getPosition();
            newPosition.setLat(position.getLatitude())
                    .setLon(position.getLongitude())
                    .setSpeed(position.getSpeed())
                    .setHeading(position.getBearing());
        }

        if (vehiclePosition.hasVehicle()) {
            var vehicle = vehiclePosition.getVehicle();
            newPosition.setVehicleId(vehicle.getId())
                    .setLabel(Optional.ofNullable(vehicle.getLabel())
                            .orElse(vehicle.getLicensePlate()));
        }

        if (vehiclePosition.hasCurrentStatus()) {
            newPosition.setStopStatus(vehiclePosition.getCurrentStatus());
        }

        if (vehiclePosition.hasCongestionLevel()) {
            newPosition.setCongestionLevel(vehiclePosition.getCongestionLevel());
        }

        if (vehiclePosition.hasTimestamp()) {
            newPosition.setTime(Instant.ofEpochSecond(vehiclePosition.getTimestamp()));
        }

        if (vehiclePosition.hasStopId()) {
            List<StopLocation> matchedStops = stopsOnVehicleTrip
                    .stream()
                    .filter(stop -> stop.getId().getId().equals(vehiclePosition.getStopId()))
                    .collect(Collectors.toList());
            if (matchedStops.size() == 1) {
                newPosition.setNextStop(matchedStops.get(0));
            }
        }

        if (vehiclePosition.hasCurrentStopSequence()) {
            newPosition.setNextStopSequenceId(vehiclePosition.getCurrentStopSequence());
        }

        return newPosition.build();
    }
}
