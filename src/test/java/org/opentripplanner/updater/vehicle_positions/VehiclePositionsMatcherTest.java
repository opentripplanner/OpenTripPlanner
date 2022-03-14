package org.opentripplanner.updater.vehicle_positions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;

public class VehiclePositionsMatcherTest {

    String feedId = "feed1";

    @Test
    public void matchRealtimePositionsToTrip() {

        var service = new RealtimeVehiclePositionService();

        var tripId = "trip1";
        var scopedTripId = new FeedScopedId(feedId, tripId);

        var trip = new Trip(scopedTripId);

        var stopPattern =
                new StopPattern(List.of(stopTime(trip, 0), stopTime(trip, 1), stopTime(trip, 2)));

        var pattern = new TripPattern(new FeedScopedId(feedId, tripId), null, stopPattern);

        var tripForId = Map.of(scopedTripId, trip);
        var patternForTrip = Map.of(trip, pattern);

        // an untouched pattern has no vehicle positions
        assertEquals(0, service.getVehiclePositions(pattern).size());

        // Map positions to trips in feed
        VehiclePositionPatternMatcher matcher =
                new VehiclePositionPatternMatcher(
                        feedId,
                        tripForId::get,
                        patternForTrip::get,
                        service
                );

        var pos = VehiclePosition.newBuilder()
                .setTrip(TripDescriptor.newBuilder().setTripId(tripId).build())
                .setStopId("stop-1")
                .build();

        var positions = List.of(pos);

        // Execute the same match-to-pattern step as the runner
        matcher.applyVehiclePositionUpdates(positions);

        // ensure that gtfs-rt was matched to an OTP pattern correctly
        assertEquals(1, service.getVehiclePositions(pattern).size());

        // if we have an empty list of updates then clear the positions from the previous update
        matcher.applyVehiclePositionUpdates(List.of());
        assertEquals(0, service.getVehiclePositions(pattern).size());
    }

    @Test
    public void clearOldTrips() {

        var service = new RealtimeVehiclePositionService();

        var tripId1 = "trip1";
        var tripId2 = "trip2";
        var scopedTripId1 = new FeedScopedId(feedId, tripId1);
        var scopedTripId2 = new FeedScopedId(feedId, tripId2);

        var trip1 = new Trip(scopedTripId1);
        var trip2 = new Trip(scopedTripId2);

        var stopPattern1 =
                new StopPattern(List.of(stopTime(trip1, 0), stopTime(trip1, 1), stopTime(trip1, 2)));

        var stopPattern2 =
                new StopPattern(List.of(stopTime(trip1, 0), stopTime(trip1, 1), stopTime(trip2, 2)));

        var pattern1 = new TripPattern(new FeedScopedId(feedId, tripId1), null, stopPattern1);
        var pattern2 = new TripPattern(new FeedScopedId(feedId, tripId2), null, stopPattern2);

        var tripForId = Map.of(
                scopedTripId1, trip1,
                scopedTripId2, trip2
        );

        var patternForTrip = Map.of(
                trip1, pattern1,
                trip2, pattern2
        );

        // an untouched pattern has no vehicle positions
        assertEquals(0, service.getVehiclePositions(pattern1).size());

        // Map positions to trips in feed
        VehiclePositionPatternMatcher matcher =
                new VehiclePositionPatternMatcher(
                        feedId,
                        tripForId::get,
                        patternForTrip::get,
                        service
                );

        var pos1 = VehiclePosition.newBuilder()
                .setTrip(TripDescriptor.newBuilder().setTripId(tripId1).build())
                .setStopId("stop-1")
                .build();

        var pos2 = VehiclePosition.newBuilder()
                .setTrip(TripDescriptor.newBuilder().setTripId(tripId2).build())
                .setStopId("stop-1")
                .build();

        var positions = List.of(pos1, pos2);

        // Execute the same match-to-pattern step as the runner
        matcher.applyVehiclePositionUpdates(positions);

        // ensure that gtfs-rt was matched to an OTP pattern correctly
        assertEquals(1, service.getVehiclePositions(pattern1).size());
        assertEquals(1, service.getVehiclePositions(pattern2).size());

        matcher.applyVehiclePositionUpdates(List.of(pos1));
        assertEquals(1, service.getVehiclePositions(pattern1).size());
        // because there are no more updates for pattern2 we remove all positions
        assertEquals(0, service.getVehiclePositions(pattern2).size());
    }


    private StopTime stopTime(Trip trip, int seq) {
        var stopTime = new StopTime();
        stopTime.setTrip(trip);
        stopTime.setStopSequence(seq);

        var stop = Stop.stopForTest("stop-" + seq, 0, 0);
        stopTime.setStop(stop);

        return stopTime;
    }

}
