package org.opentripplanner.standalone.config.updaters;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_positions.VehiclePositionsUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class VehiclePositionsUpdaterConfig {

    private static final Map<String, DataSourceType> CONFIG_MAPPING = new HashMap<>();

    static {
        CONFIG_MAPPING.put("gtfs-http", DataSourceType.GTFS_RT_VEHICLE_POSITIONS);
    }

    private static DataSourceType mapStringToSourceType(String typeKey) {
        DataSourceType type = CONFIG_MAPPING.get(typeKey);
        if (type == null) {
            throw new OtpAppException("The updater source type is unknown: " + typeKey);
        }
        return type;
    }

    public static VehiclePositionsUpdaterParameters create(String updaterRef, NodeAdapter c) {
        var sourceType = mapStringToSourceType(c.asText("sourceType"));
        var feedId = c.asText("feedId");
        var frequencySec = c.asInt("frequencySec", 60);

        switch (sourceType) {
            case GTFS_RT_VEHICLE_POSITIONS:
                var url = c.asUri("url");
                return new VehiclePositionsUpdaterParameters(updaterRef, feedId, url, frequencySec);
            default:
                throw new OtpAppException("The updater source type is unhandled: " + sourceType);
        }
    }
}
