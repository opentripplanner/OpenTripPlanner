package org.opentripplanner.ext.trias.trias;

import static jakarta.xml.bind.Marshaller.*;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import de.vdv.ojp20.OJP;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.trias.id.UseFeedIdResolver;
import org.opentripplanner.ext.trias.mapping.StopEventResponseMapper;
import org.opentripplanner.ext.trias.service.CallAtStop;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

class OjpMapperTest {

  private static final String ROUTE_ID = "r1";
  private static final LocalDate SERVICE_DATE = LocalDate.of(2025, 2, 10);
  private static final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();
  private static final TimetableRepositoryForTest TEST_MODEL = new TimetableRepositoryForTest(
    siteRepositoryBuilder
  );
  private static final RegularStop STOP_1 = TEST_MODEL.stop("s1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("s2").build();
  private static final Route ROUTE = TimetableRepositoryForTest.route(id(ROUTE_ID)).build();
  private static final String TRIP_ID = "t1";
  private static final Trip TRIP = TimetableRepositoryForTest.trip(TRIP_ID).build();

  private static final String START_TIME = "07:30:00";
  private static final TripTimes TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    TEST_MODEL.stopTimesEvery5Minutes(5, TRIP, START_TIME),
    new Deduplicator()
  );
  private static final TripPattern TRIP_PATTERN = TimetableRepositoryForTest.tripPattern(
    "tp1",
    ROUTE
  )
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_1, STOP_2))
    .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(TRIP_TIMES))
    .build();
  private static final TripTimeOnDate TRIP_TIMES_ON_DATE = new TripTimeOnDate(
    TRIP_TIMES,
    0,
    TRIP_PATTERN,
    SERVICE_DATE,
    SERVICE_DATE.atStartOfDay(ZoneIds.BERLIN).toInstant()
  );

  private static final ZonedDateTime timestamp = OffsetDateTime.parse(
    "2025-02-10T14:24:02+01:00"
  ).atZoneSameInstant(ZoneIds.BERLIN);
  private static final Function<String, Optional<String>> RESOLVE_FEED_LANG = feedId ->
    Optional.of("de");
  private static final Duration WALK_TIME = Duration.ofMinutes(10);

  @Test
  void test() throws JAXBException {
    var mapper = new StopEventResponseMapper(
      Set.of(),
      ZoneIds.BERLIN,
      new UseFeedIdResolver(),
      RESOLVE_FEED_LANG
    );

    var ojp = mapper.mapCalls(List.of(new CallAtStop(TRIP_TIMES_ON_DATE, WALK_TIME)), timestamp);

    var context = JAXBContext.newInstance(OJP.class);
    var marshaller = context.createMarshaller();

    // Format the XML output
    marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);

    // Convert Java object to XML string
    StringWriter xmlWriter = new StringWriter();
    marshaller.marshal(ojp, xmlWriter);

    // Print the XML output
    System.out.println(xmlWriter);
  }

  @Test
  void ojpToTrias() {
    var mapper = new StopEventResponseMapper(
      Set.of(),
      ZoneIds.BERLIN,
      new UseFeedIdResolver(),
      RESOLVE_FEED_LANG
    );
    var ojp = mapper.mapCalls(List.of(new CallAtStop(TRIP_TIMES_ON_DATE, WALK_TIME)), timestamp);
    OjpToTriasTransformer.ojpToTrias(ojp, new PrintWriter(System.out));
  }
}
