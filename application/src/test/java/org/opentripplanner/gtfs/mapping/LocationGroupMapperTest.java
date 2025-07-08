package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.LocationGroup;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.SiteRepository;

class LocationGroupMapperTest {

  private static final String NAME = "A GROUP";
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);

  @Test
  void map() {
    var mapper = defaultMapper();

    var lg = new LocationGroup();
    lg.setId(id("group-1"));
    lg.setName(NAME);

    lg.addLocation(stop());
    var groupStop = mapper.map(lg);
    assertEquals(NAME, groupStop.getName().toString());
    assertEquals(
      Set.of(new FeedScopedId(FEED_ID, "stop-1")),
      groupStop.getChildLocations().stream().map(StopLocation::getId).collect(Collectors.toSet())
    );
  }

  @Test
  void mapNullableName() {
    var mapper = defaultMapper();

    var lg = new LocationGroup();
    lg.setId(id("group-1"));

    lg.addLocation(stop());
    var groupStop = mapper.map(lg);
    assertEquals("Location group", groupStop.getName().toString());
  }

  private static LocationGroupMapper defaultMapper() {
    var builder = SiteRepository.of();
    return new LocationGroupMapper(
      ID_FACTORY,
      new StopMapper(ID_FACTORY, new TranslationHelper(), id -> null, builder),
      new LocationMapper(ID_FACTORY, builder, DataImportIssueStore.NOOP),
      builder
    );
  }

  private static Stop stop() {
    var stop = new Stop();
    stop.setId(id("stop-1"));
    stop.setLat(1);
    stop.setLon(1);
    return stop;
  }

  private static AgencyAndId id(String id) {
    return new AgencyAndId("1", id);
  }
}
