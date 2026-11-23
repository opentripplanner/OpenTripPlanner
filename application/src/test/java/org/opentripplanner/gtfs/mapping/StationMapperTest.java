package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopAreaElement;
import org.opentripplanner.transit.model.site.StopTransferPriority;

public class StationMapperTest {

  private static final String FEED_ID = "A";
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);
  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId(FEED_ID, "1");

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final String NAME = "Name";

  private static final Stop STATION = new Stop();

  static {
    STATION.setId(AGENCY_AND_ID);
    STATION.setLat(LAT);
    STATION.setLon(LON);
    STATION.setName(NAME);
    STATION.setLocationType(1);
  }

  @Test
  public void testTransferPriority() {
    var recommendedPriority = StopTransferPriority.RECOMMENDED;
    StationMapper recommendedTransferMapper = new StationMapper(
      ID_FACTORY,
      new TranslationHelper(),
      recommendedPriority,
      new FareAreaMapper(ID_FACTORY)
    );
    var stationWithRecommendedTransfer = recommendedTransferMapper.map(STATION);
    assertEquals(recommendedPriority, stationWithRecommendedTransfer.getPriority());
  }

  @Test
  void testFareAreas() {
    var area = new Area();
    area.setId(new AgencyAndId(FEED_ID, "area-1"));
    area.setName("Area One");
    var element = new StopAreaElement();
    element.setStop(STATION);
    element.setArea(area);
    var fareAreaMapper = new FareAreaMapper(ID_FACTORY);
    fareAreaMapper.map(List.of(element));

    var subject = new StationMapper(
      ID_FACTORY,
      new TranslationHelper(),
      StopTransferPriority.ALLOWED,
      fareAreaMapper
    );
    var station = subject.map(STATION);

    assertThat(station.getFareAreas()).containsExactly(fareAreaMapper.map(area));
    assertEquals("A:area-1", station.getFareAreas().getFirst().getId().toString());
    assertEquals("Area One", station.getFareAreas().getFirst().getName());
  }

  @Test
  void testWithoutFareAreas() {
    var subject = new StationMapper(
      ID_FACTORY,
      new TranslationHelper(),
      StopTransferPriority.ALLOWED,
      new FareAreaMapper(ID_FACTORY)
    );

    assertThat(subject.map(STATION).getFareAreas()).isEmpty();
  }
}
