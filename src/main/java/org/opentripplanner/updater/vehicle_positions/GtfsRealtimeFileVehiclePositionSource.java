package org.opentripplanner.updater.vehicle_positions;

import com.google.common.base.MoreObjects;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for reading GTFS-rt vehicle positions from a local file and loading into memory.
 */
public class GtfsRealtimeFileVehiclePositionSource implements VehiclePositionSource {

    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeFileVehiclePositionSource.class);

    /**
     * File to read GTFS-RT data from
     */
    private final File file;

    public GtfsRealtimeFileVehiclePositionSource(File file) {
        this.file = file;
    }

    /**
     * Parses raw GTFS-RT data into vehicle positions
     */
    public List<VehiclePosition> getPositions() {
        try (InputStream is = new FileInputStream(file)) {
            return this.getPositions(is);
        }
        catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed at {}:", file, e);
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("file", file)
                .toString();
    }
}
