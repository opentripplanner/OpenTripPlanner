package org.opentripplanner.updater.vehicle_rental.GBFSMappings;

import java.util.List;

/**
 * Response class for the station_status.json file.
 * See https://github.com/NABSA/gbfs/blob/v1.0/gbfs.md#station_statusjson
 */
public class StationStatus extends BaseGtfsResponse {
    public StationStatusData data;

    public static class StationStatusData {
        public List<DockingStationStatusInformation> stations;
    }

    public static class DockingStationStatusInformation {
        public String station_id;
        public Integer num_bikes_available;
        public Integer num_bikes_disabled;
        public Integer num_docks_available;
        public Integer num_docks_disabled;
        public Integer is_installed;
        public Integer is_renting;
        public Integer is_returning;
        public Long last_reported;
    }
}
