package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.LocationGroup;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.StopModel;

class LocationGroupMapperTest {

  private static final String NAME = "A GROUP";

  @Test
  void map() {
    var builder = StopModel.of();
    var mapper = new LocationGroupMapper(
      new StopMapper(new TranslationHelper(), id -> null, builder),
      new LocationMapper(builder),
      builder
    );

    var lg = new LocationGroup();
    lg.setId(id("group-1"));
    lg.setName(NAME);

    var stop = new Stop();
    stop.setId(id("stop-1"));
    stop.setLat(1);
    stop.setLon(1);

    lg.addLocation(stop);
    var groupStop = mapper.map(lg);
    assertEquals(NAME, groupStop.getName().toString());
    assertEquals(
      Set.of(new FeedScopedId("1", "stop-1")),
      groupStop.getChildLocations().stream().map(StopLocation::getId).collect(Collectors.toSet())
    );
  }

  @Nonnull
  private static AgencyAndId id(String id) {
    return new AgencyAndId("1", id);
  }
}
