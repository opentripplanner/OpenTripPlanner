package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.time;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.realtimeresolver.RealtimeResolver;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class StopsLayerTest {

  private RegularStop stop;
  private RegularStop stop2;

  @BeforeEach
  public void setUp() {
    var nameTranslations = TranslatedString.getI18NString(
      new HashMap<>() {
        {
          put(null, "name");
          put("de", "nameDE");
        }
      },
      false,
      false
    );
    var descTranslations = TranslatedString.getI18NString(
      new HashMap<>() {
        {
          put(null, "desc");
          put("de", "descDE");
        }
      },
      false,
      false
    );
    stop =
      StopModel
        .of()
        .regularStop(new FeedScopedId("F", "name"))
        .withName(nameTranslations)
        .withDescription(descTranslations)
        .withCoordinate(50, 10)
        .build();
    stop2 =
      StopModel
        .of()
        .regularStop(new FeedScopedId("F", "name"))
        .withName(nameTranslations)
        .withDescription(descTranslations)
        .withCoordinate(51, 10)
        .build();
  }

  @Test
  public void digitransitStopPropertyMapperTest() {
    var deduplicator = new Deduplicator();
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    transitModel.index();
    var transitService = new DefaultTransitService(transitModel);

    DigitransitStopPropertyMapper mapper = DigitransitStopPropertyMapper.create(
      transitService,
      new Locale("en-US")
    );

    Map<String, Object> map = new HashMap<>();
    mapper.map(stop).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
  }

  @Test
  public void digitransitStopPropertyMapperTranslationTest() {
    var deduplicator = new Deduplicator();
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    transitModel.index();
    var transitService = new DefaultTransitService(transitModel);

    DigitransitStopPropertyMapper mapper = DigitransitStopPropertyMapper.create(
      transitService,
      new Locale("de")
    );

    Map<String, Object> map = new HashMap<>();
    mapper.map(stop).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("nameDE", map.get("name"));
    assertEquals("descDE", map.get("desc"));
  }

  @Test
  public void digitransitRealtimeStopPropertyMapperTest() {
    var deduplicator = new Deduplicator();
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    transitModel.initTimeZone(ZoneIds.HELSINKI);
    transitModel.index();
    var alertService = new TransitAlertServiceImpl(transitModel);
    var transitService = new DefaultTransitService(transitModel) {
      @Override
      public TransitAlertService getTransitAlertService() {
        return alertService;
      }
    };

    Route route = TransitModelForTest.route("route").build();
    var itinerary = newItinerary(Place.forStop(stop), time("11:00"))
      .bus(route, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .build();
    var startDate = ZonedDateTime.now(ZoneIds.HELSINKI).minusDays(1).toEpochSecond();
    var endDate = ZonedDateTime.now(ZoneIds.HELSINKI).plusDays(1).toEpochSecond();
    var alert = TransitAlert
      .of(stop.getId())
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
    assertEquals(true, map.get("noServiceAlert"));
  }
}
