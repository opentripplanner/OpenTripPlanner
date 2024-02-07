package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.Location;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopArea;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.transit.service.StopModel;

class StopAreaMapperTest {

  private static final String NAME = "Floxjam";
  private static final AgencyAndId AREA_ID = agencyAndId("flox");

  @Test
  void map() {
    var stopModel = StopModel.of();
    var stopMapper = new StopMapper(new TranslationHelper(), ignored -> null, stopModel);
    var locationMapper = new LocationMapper(stopModel);
    var mapper = new StopAreaMapper(stopMapper, locationMapper, stopModel);

    var area = new Area();
    area.setId(AREA_ID);
    area.setName(NAME);

    var stop1 = stop("stop1");
    var stop2 = stop("stop2");
    var location = location("location");

    var stopArea = new StopArea();
    stopArea.setArea(area);
    stopArea.addLocation(stop1);
    stopArea.addLocation(stop2);
    stopArea.addLocation(location);
    var areaStop = mapper.map(stopArea);

    assertEquals(NAME, areaStop.getName().toString());
    var stopIds = areaStop
      .getChildLocations()
      .stream()
      .map(l -> l.getId().toString())
      .collect(Collectors.toSet());
    assertEquals(Set.of("1:location", "1:stop1", "1:stop2"), stopIds);
  }

  private static Stop stop(String id) {
    var stop = new Stop();
    stop.setId(agencyAndId(id));
    stop.setLat(1);
    stop.setLon(2);
    stop.setName("A stop");
    return stop;
  }

  private static Location location(String id) {
    var stop = new Location();
    stop.setId(agencyAndId(id));
    stop.setName("A stop");
    stop.setGeometry(Polygons.toGeoJson(Polygons.BERLIN));
    return stop;
  }

  @Nonnull
  private static AgencyAndId agencyAndId(String id) {
    return new AgencyAndId("1", id);
  }
}
