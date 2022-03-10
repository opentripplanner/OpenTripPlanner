package org.opentripplanner.updater.vehicle_positions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMultimap;
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

public class VehiclePositionsMatcherTest {

    String feedId = "feed1";

    @Test
    public void shouldMatchRealtimePositionsToTrip() {
        var tripId = "trip1";
        var scopedTripId = new FeedScopedId(feedId, tripId);

        var trip = new Trip(scopedTripId);

        var stopPattern =
                new StopPattern(List.of(stopTime(trip, 0), stopTime(trip, 1), stopTime(trip, 2)));

        var pattern = new TripPattern(new FeedScopedId(feedId, tripId), null, stopPattern);

        var patternsForFeedId = ImmutableMultimap.of(
                feedId,
                pattern
        );

        var tripForId = Map.of(scopedTripId, trip);
        var patternForTrip = Map.of(trip, pattern);

        // an untouched pattern has no vehicle positions
        assertEquals(0, pattern.getVehiclePositions().size());

        // Map positions to trips in feed
        VehiclePositionPatternMatcher matcher =
                new VehiclePositionPatternMatcher(
                        () -> patternsForFeedId,
                        () -> tripForId,
                        () -> patternForTrip
                );

        var pos = VehiclePosition.newBuilder()
                .setTrip(TripDescriptor.newBuilder().setTripId(tripId).build())
                .setStopId("stop-1")
                .build();

        var positions = List.of(pos);

        // Execute the same match-to-pattern step as the runner
        matcher.applyVehiclePositionUpdates(positions, feedId);

        // ensure that gtfs-rt was matched to an OTP pattern correctly
        assertEquals(1, pattern.getVehiclePositions().size());

        matcher.wipeSeenTripIds();

        // apply empty vehicle positions
        matcher.applyVehiclePositionUpdates(List.of(), feedId);

        // "clean" patterns
        matcher.cleanPatternVehiclePositions(feedId);

        assertEquals(0, pattern.getVehiclePositions().size());

        // Ensure that after cleaning it is correct, and that the number of vehicles matched to
        // patterns equals the number of vehicles in the feed
        //updatedRealtimeVehicleCount = getRealtimeVehicleCountForPatterns(patterns);
        //Assert.assertEquals(updated_positions.size(), updatedRealtimeVehicleCount);
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
