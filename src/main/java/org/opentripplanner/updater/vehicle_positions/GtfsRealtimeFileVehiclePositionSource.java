package org.opentripplanner.updater.vehicle_positions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Responsible for reading GTFS-rt vehicle positions from a local file and loading into memory.
 */
public class GtfsRealtimeFileVehiclePositionSource implements VehiclePositionSource, JsonConfigurable {
    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeFileVehiclePositionSource.class);

    /**
     * File to read GTFS-RT data from
     */
    private File file;
    /**
     * FeedId the GTFS-RT feed is associated with
     */
    private String feedId;

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        this.feedId = config.path("feedId").asText();
        this.file = new File(config.path("file").asText(""));
    }

    /**
     * Parses raw GTFS-RT data into vehicle positions
     */
    public List<VehiclePosition> getPositions() {
        try (InputStream is = new FileInputStream(file)) {
            return this.getPositions(is);
        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed at {}:", file, e);
        }
        return Collections.emptyList();
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }
}
