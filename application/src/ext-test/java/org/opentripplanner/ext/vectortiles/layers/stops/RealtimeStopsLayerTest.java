package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
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
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.ext.realtimeresolver.RealtimeResolver;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

public class RealtimeStopsLayerTest {

  private RegularStop stop;
  private RegularStop stop2;

  @BeforeEach
  public void setUp() {
    var name = I18NString.of("name");
    var desc = I18NString.of("desc");
    stop = SiteRepository.of()
      .regularStop(id("name"))
      .withName(name)
      .withDescription(desc)
      .withCoordinate(50, 10)
      .withTimeZone(ZoneIds.HELSINKI)
      .build();
    stop2 = SiteRepository.of()
      .regularStop(id("name"))
      .withName(name)
      .withDescription(desc)
      .withCoordinate(51, 10)
      .withTimeZone(ZoneIds.HELSINKI)
      .build();
  }

  @Test
  void realtimeStopLayer() {
    var timetableRepository = new TransitRepository(new SiteRepository());
    timetableRepository.initTimeZone(ZoneIds.HELSINKI);
    timetableRepository.index();
    var transitService = new DefaultTransitService(timetableRepository);
    var transitAlertService = new TransitAlertServiceImpl();

    Route route = TransitRepositoryForTest.route("route").build();
    var itinerary = newItinerary(Place.forStop(stop), time("11:00"))
      .bus(route, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .build();
    var startDate = ZonedDateTime.now(ZoneIds.HELSINKI).minusDays(1).toInstant();
    var endDate = ZonedDateTime.now(ZoneIds.HELSINKI).plusDays(1).toInstant();
    var calendar = AlertCalendar.of(TimePeriod.of(startDate, endDate));
    var alert = TransitAlert.of(id("alert-1"))
      .addEntity(new EntitySelector.Stop(stop.getId()))
      .withCalendar(calendar)
      .withEffect(AlertEffect.NO_SERVICE)
      .withSeverity(AlertSeverity.WARNING)
      .build();
    var severeAlert = TransitAlert.of(id("alert-2"))
      .addEntity(new EntitySelector.Stop(stop.getId()))
      .withCalendar(calendar)
      .withEffect(AlertEffect.REDUCED_SERVICE)
      .withSeverity(AlertSeverity.WARNING)
      .build();
    var infoAlert = TransitAlert.of(id("alert-3"))
      .addEntity(new EntitySelector.Stop(stop.getId()))
      .withCalendar(calendar)
      .withEffect(AlertEffect.MODIFIED_SERVICE)
      .withSeverity(AlertSeverity.INFO)
      .build();

    var expiredStartDate = ZonedDateTime.now(ZoneIds.HELSINKI).minusDays(3).toInstant();
    var expiredEndDate = ZonedDateTime.now(ZoneIds.HELSINKI).minusDays(2).toInstant();
    var expiredCalendar = AlertCalendar.of(TimePeriod.of(expiredStartDate, expiredEndDate));
    var expiredAlert = TransitAlert.of(id("alert-4"))
      .addEntity(new EntitySelector.Stop(stop.getId()))
      .withCalendar(expiredCalendar)
      .withEffect(AlertEffect.DETOUR)
      .withSeverity(AlertSeverity.SEVERE)
      .build();

    transitAlertService.setAlerts(List.of(alert, severeAlert, infoAlert, expiredAlert));

    // TODO Why is these 2 lines here - the test works without them?
    var itineraries = List.of(itinerary);
    itineraries = RealtimeResolver.populateLegsWithRealtime(
      itineraries,
      transitService,
      transitAlertService
    );

    DigitransitRealtimeStopPropertyMapper mapper = new DigitransitRealtimeStopPropertyMapper(
      transitService,
      transitAlertService,
      new Locale("en-US")
    );

    Map<String, Object> map = new HashMap<>();
    mapper.map(stop).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
    assertEquals(true, map.get("closedByServiceAlert"));
    assertEquals("WARNING", map.get("mostSevereAlertSeverityLevel"));
    assertEquals("NO_SERVICE,REDUCED_SERVICE", map.get("mostSevereAlertsEffects"));
    assertEquals(false, map.get("servicesRunningOnServiceDate"));
  }
}
