package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.NOOP;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;

public class StopTimeMapperTest {

  private static final String FEED_ID = "FEED";
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);

  public static final String CSV =
    """
    trip_id,arrival_time,departure_time,stop_id,stop_sequence,shape_dist_traveled,pickup_type,drop_off_type,stop_headsign
    1.1,00:05:00,00:05:00,A,1,3.4,3,2,Head Sign
    1.1,00:10:00,00:10:00,B,2,,1,1,
    1.1,00:20:00,00:20:00,C,3,,0,0,
    1.2,00:20:00,00:20:00,A,1,,0,0,
    1.2,00:30:00,00:30:00,B,2,,0,0,
    1.2,00:40:00,00:40:00,C,3,,0,0,
    1.3,08:00:00,08:00:00,A,1,,2,2,
    1.3,08:10:00,08:20:00,B,2,,0,0,
    1.3,08:30:00,08:30:00,C,3,,0,0,
    """;
  public static final Route ROUTE = TimetableRepositoryForTest.route("r1").build();

  private final BookingRuleMapper bookingRuleMapper = new BookingRuleMapper(ID_FACTORY);
  private final TranslationHelper translationHelper = new TranslationHelper();
  private final StopTimeMapper subject = new StopTimeMapper(
    ID_FACTORY,
    builder(),
    bookingRuleMapper,
    translationHelper
  );

  @Test
  public void testMap() throws IOException {
    var stopTimes = subject.map(new TestCsvSource(CSV)).toList();

    assertThat(stopTimes).hasSize(9);
    var result = stopTimes.getFirst();
    assertEquals(300, result.getArrivalTime());
    assertEquals(300, result.getDepartureTime());
    assertEquals(PickDrop.COORDINATE_WITH_DRIVER, result.getPickupType());
    assertEquals(PickDrop.CALL_AGENCY, result.getDropOffType());
    assertEquals(3.4d, result.getShapeDistTraveled(), 0.0001d);
    assertNotNull(result.getStop());
    assertEquals("Head Sign", result.getStopHeadsign().toString());
    assertEquals(1, result.getStopSequence());
    assertEquals(-999, result.getTimepoint());
    assertNotNull(result.getTrip());
  }

  private OtpTransitServiceBuilder builder() {
    var siteRepositoryBuilder = SiteRepository.of();
    var builder = new OtpTransitServiceBuilder(siteRepositoryBuilder.build(), NOOP);
    List.of("A", "B", "C").forEach(id -> {
      var s = builder
        .siteRepository()
        .regularStop(ID_FACTORY.createNullableId(id))
        .withName(I18NString.of(id))
        .withCoordinate(0, 0)
        .build();
      builder.siteRepository().withRegularStop(s);
    });

    List.of("1.1", "1.2", "1.3").forEach(id -> {
      builder.getTripsById().add(Trip.of(ID_FACTORY.createNullableId(id)).withRoute(ROUTE).build());
    });

    return builder;
  }
}
