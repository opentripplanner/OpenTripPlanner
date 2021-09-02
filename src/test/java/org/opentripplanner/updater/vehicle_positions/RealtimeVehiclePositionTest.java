package org.opentripplanner.updater.vehicle_positions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.index.model.RealtimeVehiclePosition;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotEquals;

public class RealtimeVehiclePositionTest extends GtfsTest {

    private JsonNode generateConfig(String fileName) throws IOException {
        String jsonString = "{\"feedId\":\"" + this.getFeedName() + "\",\"file\":\"src/test/resources/" + fileName + "\"}";
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonString);
    }

    private int getRealtimeVehicleCountForPatterns(Collection<TripPattern> patterns) {
        AtomicInteger realtimeVehicleCount = new AtomicInteger();

        patterns.forEach(pattern -> {
            List<RealtimeVehiclePosition> vehiclePositions = pattern.getVehiclePositions();
            realtimeVehicleCount.addAndGet(vehiclePositions.size());
        });

        return realtimeVehicleCount.get();
    }

    @Test
    public void testRealtimeTest() throws Exception {
        // Initialize updater
        GtfsRealtimeFileVehiclePositionSource vehiclePositionSource = new GtfsRealtimeFileVehiclePositionSource();

        // Create config
        JsonNode config = this.generateConfig("kcm_rt_gtfs.pb");

        // Process realtime feed
        vehiclePositionSource.configure(this.graph, config);
        List<GtfsRealtime.VehiclePosition> positions = vehiclePositionSource.getPositions();

        // Ensure feed was parsed
        Assert.assertNotNull(positions);

        // Map positions to trips in feed
        VehiclePositionPatternMatcher vehiclePositionPatternMatcher = new VehiclePositionPatternMatcher(this.graph);
        vehiclePositionPatternMatcher.applyVehiclePositionUpdates(positions, this.feedId.getId());

        // Get all patterns in feed
        Collection<TripPattern> patterns = this.graph.index.patternsForFeedId.get(this.feedId.getId());

        // Count number of vehicles across all patterns
        int realtimeVehicleCount = this.getRealtimeVehicleCountForPatterns(patterns);

        Assert.assertEquals(627, realtimeVehicleCount);

        // Update positions
        JsonNode updated_config = this.generateConfig("kcm_rt_gtfs_2.pb");

        // Process realtime feed
        vehiclePositionSource.configure(this.graph, updated_config);
        List<GtfsRealtime.VehiclePosition> updated_positions = vehiclePositionSource.getPositions();

        // Ensure feed was parsed and that it was correctly different
        assertNotEquals(positions.size(), updated_positions.size());

        // In the test data, updated_positions is smaller
        for (int i = 0; i < updated_positions.size(); i++) {
            Assert.assertNotEquals(positions.get(i), updated_positions.get(i));
        }

        // Execute the same update step as the runner
        vehiclePositionPatternMatcher.wipeSeenTripIds();
        vehiclePositionPatternMatcher.applyVehiclePositionUpdates(updated_positions, this.feedId.getId());

        // Ensure that without cleaning the number of active vehicles is wrong
        int updatedRealtimeVehicleCount = getRealtimeVehicleCountForPatterns(patterns);
        Assert.assertEquals(1016, updatedRealtimeVehicleCount);

        // "clean" patterns
        vehiclePositionPatternMatcher.cleanPatternVehiclePositions(this.feedId.getId());

        // Ensure that after cleaning it is correct
        updatedRealtimeVehicleCount = getRealtimeVehicleCountForPatterns(patterns);
        Assert.assertEquals(updated_positions.size(), updatedRealtimeVehicleCount);
    }

    @Override
    public final String getFeedName() {
        return "kcm_rt_gtfs.zip";
    }
}
