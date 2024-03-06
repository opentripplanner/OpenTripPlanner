package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Set;
import org.opentripplanner.ext.vehicleparking.bikely.BikelyUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

public class VehicleParkingUpdaterConfig {

  public static VehicleParkingUpdaterParameters create(String updaterRef, NodeAdapter c) {
    var sourceType = c
      .of("sourceType")
      .since(V2_2)
      .summary("The source of the vehicle updates.")
      .asEnum(VehicleParkingSourceType.class);
    var feedId = c
      .of("feedId")
      .since(V2_2)
      .summary("The name of the data source.")
      .description("This will end up in the API responses as the feed id of of the parking lot.")
      .asString();
    return switch (sourceType) {
      case HSL_PARK -> new HslParkUpdaterParameters(
        updaterRef,
        c
          .of("facilitiesFrequencySec")
          .since(V2_2)
          .summary("How often the facilities should be updated.")
          .asInt(3600),
        c.of("facilitiesUrl").since(V2_2).summary("URL of the facilities.").asString(null),
        feedId,
        sourceType,
        c
          .of("utilizationsFrequencySec")
          .since(V2_2)
          .summary("How often the utilization should be updated.")
          .asInt(600),
        c.of("utilizationsUrl").since(V2_2).summary("URL of the utilization data.").asString(null),
        getTimeZone(c),
        c.of("hubsUrl").since(V2_2).summary("Hubs URL").asString(null)
      );
      case PARK_API, BICYCLE_PARK_API -> new ParkAPIUpdaterParameters(
        updaterRef,
        c.of("url").since(V2_2).summary("URL of the resource.").asString(null),
        feedId,
        c
          .of("frequency")
          .since(V2_2)
          .summary("How often to update the source.")
          .asDuration(Duration.ofMinutes(1)),
        HttpHeadersConfig.headers(c, V2_2),
        new ArrayList<>(
          c.of("tags").since(V2_2).summary("Tags to add to the parking lots.").asStringSet(Set.of())
        ),
        sourceType,
        getTimeZone(c)
      );
      case BIKELY -> new BikelyUpdaterParameters(
        updaterRef,
        c.of("url").since(V2_3).summary("URL of the locations endpoint.").asUri(null),
        feedId,
        c
          .of("frequency")
          .since(V2_3)
          .summary("How often to update the source.")
          .asDuration(Duration.ofMinutes(1)),
        HttpHeadersConfig.headers(c, V2_3)
      );
    };
  }

  private static ZoneId getTimeZone(NodeAdapter c) {
    return c
      .of("timeZone")
      .since(V2_2)
      .summary("The time zone of the feed.")
      .description("Used for converting abstract opening hours into concrete points in time.")
      .asZoneId(null);
  }
}
