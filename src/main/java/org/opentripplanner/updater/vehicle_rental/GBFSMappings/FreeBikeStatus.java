package org.opentripplanner.updater.vehicle_rental.GBFSMappings;

import java.util.List;

/**
 * Response class for the free_bike_status.json file.
 * See https://github.com/NABSA/gbfs/blob/v1.0/gbfs.md#free_bike_statusjson
 */
public class FreeBikeStatus extends BaseGtfsResponse {
    public FreeBikeStatusData data;

    public static class FreeBikeStatusData {
        public List<FreeBike> bikes;
    }

    public static class FreeBike {
        public String bike_id;
        public Double lat;
        public Double lon;
        public Boolean is_reserved;
        public Boolean is_disabled;
        /** This field is only available starting in GBFS 2.1-RC+ */
        public Long last_reported;
    }
}
