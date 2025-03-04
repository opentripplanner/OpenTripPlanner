package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.realtimeresolver.RealtimeResolver;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class RealtimeStopsLayerTest {

  private RegularStop stop;
  private RegularStop stop2;

  @BeforeEach
  public void setUp() {
    var name = I18NString.of("name");
    var desc = I18NString.of("desc");
    stop = SiteRepository.of()
      .regularStop(new FeedScopedId("F", "name"))
      .withName(name)
      .withDescription(desc)
      .withCoordinate(50, 10)
      .withTimeZone(ZoneIds.HELSINKI)
      .build();
    stop2 = SiteRepository.of()
      .regularStop(new FeedScopedId("F", "name"))
      .withName(name)
      .withDescription(desc)
      .withCoordinate(51, 10)
      .withTimeZone(ZoneIds.HELSINKI)
      .build();
  }

  @Test
  void realtimeStopLayer() {
    var deduplicator = new Deduplicator();
    var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
    timetableRepository.initTimeZone(ZoneIds.HELSINKI);
    timetableRepository.index();
    var transitService = new DefaultTransitService(timetableRepository) {
      final TransitAlertService alertService = new TransitAlertServiceImpl(timetableRepository);

      @Override
      public TransitAlertService getTransitAlertService() {
        return alertService;
      }
    };

    Route route = TimetableRepositoryForTest.route("route").build();
    var itinerary = newItinerary(Place.forStop(stop), time("11:00"))
      .bus(route, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .build();
    var startDate = ZonedDateTime.now(ZoneIds.HELSINKI).minusDays(1).toEpochSecond();
    var endDate = ZonedDateTime.now(ZoneIds.HELSINKI).plusDays(1).toEpochSecond();
    var alert = TransitAlert.of(stop.getId())
      .addEntity(new EntitySelector.Stop(stop.getId()))
      .addTimePeriod(new TimePeriod(startDate, endDate))
      .withEffect(AlertEffect.NO_SERVICE)
      .build();
    transitService.getTransitAlertService().setAlerts(List.of(alert));

    var itineraries = List.of(itinerary);
    RealtimeResolver.populateLegsWithRealtime(itineraries, transitService);

    DigitransitRealtimeStopPropertyMapper mapper = new DigitransitRealtimeStopPropertyMapper(
      transitService,
      new Locale("en-US")
    );

    Map<String, Object> map = new HashMap<>();
    mapper.map(stop).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
    assertEquals(true, map.get("closedByServiceAlert"));
    assertEquals(false, map.get("servicesRunningOnServiceDate"));
  }
}
