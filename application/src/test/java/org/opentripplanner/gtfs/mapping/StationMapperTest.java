package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.transit.model.site.StopTransferPriority;

public class StationMapperTest {

  private static final String FEED_ID = "A";
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
      new IdFactory(FEED_ID),
      new TranslationHelper(),
      recommendedPriority
    );
    var stationWithRecommendedTransfer = recommendedTransferMapper.map(STATION);
    assertEquals(recommendedPriority, stationWithRecommendedTransfer.getPriority());
  }
}
