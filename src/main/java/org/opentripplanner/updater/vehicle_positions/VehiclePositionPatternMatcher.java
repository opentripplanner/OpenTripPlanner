package org.opentripplanner.updater.vehicle_positions;

import com.google.common.collect.Sets;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition.StopStatus;
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

    private final String feedId;
    private final RealtimeVehiclePositionService service;

    private final Function<FeedScopedId, Trip> getTripForId;
    private final Function<Trip, TripPattern> getPatternForTrip;

    private Set<TripPattern> patternsInPreviousUpdate = Set.of();

    public VehiclePositionPatternMatcher(
            String feedId,
            Function<FeedScopedId, Trip> getTripForId,
            Function<Trip, TripPattern> getPatternForTrip,
            RealtimeVehiclePositionService service
    ) {
        this.feedId = feedId;
        this.getTripForId = getTripForId;
        this.getPatternForTrip = getPatternForTrip;
        this.service = service;
    }

    /**
     * Attempts to match each vehicle position to a pattern, then adds each to a pattern
     *
     * @param vehiclePositions List of vehicle positions to match to patterns
     */
    public void applyVehiclePositionUpdates(List<VehiclePosition> vehiclePositions) {

        var positions = vehiclePositions.stream()
                .map(vehiclePosition -> toRealtimeVehiclePosition(feedId, vehiclePosition))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t.first))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue().stream().map(t -> t.second).collect(Collectors.toList())
                ));

        positions.forEach(service::setVehiclePositions);
        Set<TripPattern> patternsInCurrentUpdate = positions.keySet();

        // if there was a position in the previous update but not in the current one, we assume
        // that the pattern has no more vehicle positions.
        var toDelete = Sets.difference(patternsInPreviousUpdate, patternsInCurrentUpdate);
        toDelete.forEach(service::clearVehiclePositions);
        patternsInPreviousUpdate = patternsInCurrentUpdate;

        if (!vehiclePositions.isEmpty() && patternsInCurrentUpdate.isEmpty()) {
            LOG.error(
                    "Could not match any vehicle positions for feedId '{}'. Are you sure that the updater is using the correct feedId?",
                    feedId
            );
        }
    }

    private T2<TripPattern, RealtimeVehiclePosition> toRealtimeVehiclePosition(
            String feedId,
            VehiclePosition vehiclePosition
    ) {
        if (!vehiclePosition.hasTrip()) {
            LOG.warn(
                    "Realtime vehicle positions without trip IDs are not yet supported.");
            return null;
        }

        String tripId = vehiclePosition.getTrip().getTripId();
        Trip trip = getTripForId.apply(new FeedScopedId(feedId, tripId));
        if (trip == null) {
            LOG.warn(
                    "Unable to find trip ID in feed '{}' for vehicle position with trip ID {}",
                    feedId, tripId
            );
            return null;
        }

        TripPattern pattern = getPatternForTrip.apply(trip);
        if (pattern == null) {
            LOG.warn(
                    "Unable to match OTP pattern ID for vehicle position with trip ID {}",
                    tripId
            );
            return null;
        }

        // Add position to pattern
        var newPosition = mapVehiclePosition(
                vehiclePosition,
                pattern.getStops()
        );

        return new T2<>(pattern, newPosition);
    }

    /**
     * Converts GtfsRealtime vehicle position to the OTP RealtimeVehiclePosition which can be used
     * by the API.
     *
     * @param vehiclePosition    GtfsRealtime vehicle position
     * @param stopsOnVehicleTrip Collection of stops method will try to match next arriving stop ID
     *                           to
     */
    public static RealtimeVehiclePosition mapVehiclePosition(
            VehiclePosition vehiclePosition,
            List<StopLocation> stopsOnVehicleTrip
    ) {
        var newPosition = RealtimeVehiclePosition.builder();

        if (vehiclePosition.hasPosition()) {
            var position = vehiclePosition.getPosition();
            newPosition.setLat(position.getLatitude())
                    .setLon(position.getLongitude());

            if (position.hasSpeed()) {
                newPosition.setSpeed(position.getSpeed());
            }
            if (position.hasBearing()) {
                newPosition.setHeading(position.getBearing());
            }
        }

        if (vehiclePosition.hasVehicle()) {
            var vehicle = vehiclePosition.getVehicle();
            newPosition.setVehicleId(vehicle.getId())
                    .setLabel(Optional.ofNullable(vehicle.getLabel())
                            .orElse(vehicle.getLicensePlate()));
        }

        if (vehiclePosition.hasCurrentStatus()) {
            newPosition.setStopStatus(toModel(vehiclePosition.getCurrentStatus()));
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

        return newPosition.build();
    }

    private static StopStatus toModel(VehicleStopStatus currentStatus) {
        switch (currentStatus) {
            case IN_TRANSIT_TO:
                return StopStatus.IN_TRANSIT_TO;
            case INCOMING_AT:
                return StopStatus.INCOMING_AT;
            case STOPPED_AT:
                return StopStatus.STOPPED_AT;
            default:
                return null;
        }
    }
}
