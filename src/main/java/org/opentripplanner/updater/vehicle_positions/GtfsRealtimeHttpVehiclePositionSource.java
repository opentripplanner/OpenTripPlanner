package org.opentripplanner.updater.vehicle_positions;

import com.google.common.base.MoreObjects;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for downloading GTFS-rt vehicle positions from a URL and loading into memory.
 */
public class GtfsRealtimeHttpVehiclePositionSource
        implements VehiclePositionSource {

    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeHttpVehiclePositionSource.class);

    /**
     * FeedId the GTFS-RT feed is associated with
     */
    private final String feedId;

    /**
     * URL to grab GTFS-RT feed from
     */
    private final String url;

    public GtfsRealtimeHttpVehiclePositionSource(String feedId, String url) {
        this.feedId = feedId;
        this.url = url;
    }

    /**
     * Parses raw GTFS-RT data into vehicle positions
     */
    public List<VehiclePosition> getPositions() {
        try (
                InputStream is = HttpUtils.getData(
                        url,
                        Map.of(
                                "Accept",
                                "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*"
                        )
                )
        ) {

            return this.getPositions(is);
        }
        catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed from {}:", url, e);
        }
        return Collections.emptyList();
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("feedId", feedId)
                .add("url", url)
                .toString();
    }
}
