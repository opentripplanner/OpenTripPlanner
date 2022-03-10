package org.opentripplanner.updater.vehicle_positions;

import static org.junit.Assert.assertNotEquals;

import com.google.transit.realtime.GtfsRealtime;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class RealtimeVehiclePositionTest extends GtfsTest {

    String feedId = "FEED";

    /**
     * Reset the graph upon tests finishing
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        this.graph = new Graph();
    }

    /**
     * This helper method iterates over a collection of patterns, collecting a running total of
     * the number of realtime vehicles observed.
     */
    private int getRealtimeVehicleCountForPatterns(Collection<TripPattern> patterns) {
        AtomicInteger realtimeVehicleCount = new AtomicInteger();

        patterns.forEach(pattern -> {
            var vehiclePositions = pattern.getVehiclePositions();
            realtimeVehicleCount.addAndGet(vehiclePositions.size());
        });

        return realtimeVehicleCount.get();
    }

    /**
     * This tests the vehicle positions updater. First, a normal GTFS feed is imported to match
     * the realtime positions on to. A realtime vehicle position feed is then imported and matched to
     * the trips defined in the non-realtime GTFS feed. Next, an updated vehicle position feed is
     * imported matched, including overwriting previous vehicles.
     */

    @Test
    public void testCanImportRealtimeVehiclePositions() {
        // Initialize updater
        GtfsRealtimeFileVehiclePositionSource vehiclePositionSource = new GtfsRealtimeFileVehiclePositionSource("1", new File("src/test/resources/vehicle-positions/king-county-metro-1.pb"));

        List<GtfsRealtime.VehiclePosition> positions = vehiclePositionSource.getPositions();

        // Ensure feed was parsed
        Assert.assertNotNull(positions);

        // Map positions to trips in feed
        VehiclePositionPatternMatcher vehiclePositionPatternMatcher = new VehiclePositionPatternMatcher(this.graph);
        vehiclePositionPatternMatcher.applyVehiclePositionUpdates(positions, feedId);

        // Get all patterns in feed
        Collection<TripPattern> patterns = this.graph.index.getPatternsForFeedId().get(feedId);

        // Count number of vehicles across all patterns
        int realtimeVehicleCount = this.getRealtimeVehicleCountForPatterns(patterns);

        // Ensure every position in the realtime positions feed was imported
        // and matched to an OTP pattern correctly
        Assert.assertEquals(positions.size(), realtimeVehicleCount);

        // Update positions
        List<GtfsRealtime.VehiclePosition> updated_positions = vehiclePositionSource.getPositions();

        // Ensure new feed was parsed. The updated position feed contains a different number of vehicles,
        // so make sure that they are not the same
        assertNotEquals(positions.size(), updated_positions.size());

        // Execute the same match-to-pattern step as the runner
        vehiclePositionPatternMatcher.wipeSeenTripIds();
        vehiclePositionPatternMatcher.applyVehiclePositionUpdates(updated_positions, feedId);

        // Ensure that without cleaning the number of active vehicles is wrong (the previous positions have not been wiped)
        int updatedRealtimeVehicleCount = getRealtimeVehicleCountForPatterns(patterns);

        // This is the number of vehicle positions across all patterns when the previous updates have
        // not been removed. This is a bad state to be in, as it results in many duplicate vehicles.
        // The number of total vehicle positions in this state is difficult to predict, but given the
        // test data is 1016. After cleaning, the number is predictable again and should be the same
        // as the number of realtime vehicles in the imported feed
        Assert.assertNotEquals(realtimeVehicleCount, updatedRealtimeVehicleCount);
        Assert.assertEquals(1016, updatedRealtimeVehicleCount);

        // "clean" patterns
        vehiclePositionPatternMatcher.cleanPatternVehiclePositions(feedId);

        // Ensure that after cleaning it is correct, and that the number of vehicles matched to
        // patterns equals the number of vehicles in the feed
        updatedRealtimeVehicleCount = getRealtimeVehicleCountForPatterns(patterns);
        Assert.assertEquals(updated_positions.size(), updatedRealtimeVehicleCount);
    }

    @Override
    public final String getFeedName() {
        return "gtfs/kcm_gtfs.zip";
    }
}
