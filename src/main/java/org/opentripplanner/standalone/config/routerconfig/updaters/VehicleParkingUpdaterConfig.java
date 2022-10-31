package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.kml.KmlUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class VehicleParkingUpdaterConfig {

  private static final Map<String, DataSourceType> CONFIG_MAPPING = new HashMap<>();

  static {
    CONFIG_MAPPING.put("hsl-park", DataSourceType.HSL_PARK);
    CONFIG_MAPPING.put("kml", DataSourceType.KML);
    CONFIG_MAPPING.put("park-api", DataSourceType.PARK_API);
    CONFIG_MAPPING.put("bicycle-park-api", DataSourceType.BICYCLE_PARK_API);
  }

  public static VehicleParkingUpdaterParameters create(String updaterRef, NodeAdapter c) {
    var sourceType = c
      .of("sourceType")
      .since(NA)
      .summary("The source of the vehicle updates.")
      .asEnum(DataSourceType.class);
    var feedId = c
      .of("feedId")
      .since(NA)
      .summary("The name of the data source.")
      .description("This will end up in the API responses as the feed id of of the parking lot.")
      .asString(null);
    var timeZone = c
      .of("timeZone")
      .since(NA)
      .summary("The time zone of the feed.")
      .description("Used for converting abstract opening hours into concrete points in time.")
      .asZoneId(null);
    switch (sourceType) {
      case HSL_PARK:
        return new HslParkUpdaterParameters(
          updaterRef,
          c
            .of("facilitiesFrequencySec")
            .since(NA)
            .summary("How often the facilities should be updated.")
            .asInt(3600),
          c.of("facilitiesUrl").since(NA).summary("URL of the facilities.").asString(null),
          feedId,
          sourceType,
          c
            .of("utilizationsFrequencySec")
            .since(NA)
            .summary("How often the utilization should be updated.")
            .asInt(600),
          c.of("utilizationsUrl").since(NA).summary("URL of the utilization data.").asString(null),
          timeZone,
          c.of("hubsUrl").since(NA).summary("Hubs URL").asString(null)
        );
      case KML:
        return new KmlUpdaterParameters(
          updaterRef,
          c.of("url").since(NA).summary("URL of the KML file.").asString(null),
          feedId,
          c.of("namePrefix").since(NA).summary("Prefix for the names.").asString(null),
          c.of("frequencySec").since(NA).summary("How often to update the parking lots.").asInt(60),
          c.of("zip").since(NA).summary("Whether the resource is zip-compressed.").asBoolean(false),
          sourceType
        );
      case PARK_API:
      case BICYCLE_PARK_API:
        return new ParkAPIUpdaterParameters(
          updaterRef,
          c.of("url").since(NA).summary("URL of the resource.").asString(null),
          feedId,
          c.of("frequencySec").since(NA).summary("How often to update the source.").asInt(60),
          c.of("headers").since(NA).summary("HTTP headers to add.").asStringMap(),
          new ArrayList<>(
            c.of("tags").since(NA).summary("Tags to add to the parking lots.").asStringSet(Set.of())
          ),
          sourceType,
          timeZone
        );
      default:
        throw new OtpAppException("The updater source type is unhandled: " + sourceType);
    }
  }
}
