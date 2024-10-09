package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.layers.TestTransitService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class StopsLayerTest {

  private static final I18NString NAME_TRANSLATIONS = TranslatedString.getI18NString(
    new HashMap<>() {
      {
        put(null, "name");
        put("de", "nameDE");
      }
    },
    false,
    false
  );
  private static final I18NString DESC_TRANSLATIONS = TranslatedString.getI18NString(
    new HashMap<>() {
      {
        put(null, "desc");
        put("de", "descDE");
      }
    },
    false,
    false
  );

  private static final Station STATION = Station
    .of(id("station1"))
    .withCoordinate(WgsCoordinate.GREENWICH)
    .withName(I18NString.of("A Station"))
    .build();
  private static final RegularStop STOP = StopModel
    .of()
    .regularStop(new FeedScopedId("F", "name"))
    .withName(NAME_TRANSLATIONS)
    .withDescription(DESC_TRANSLATIONS)
    .withCoordinate(50, 10)
    .withParentStation(STATION)
    .build();

  @Test
  public void digitransitStopPropertyMapperTest() {
    var deduplicator = new Deduplicator();
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    transitModel.index();
    var transitService = new TestTransitService(transitModel);

    DigitransitStopPropertyMapper mapper = DigitransitStopPropertyMapper.create(
      transitService,
      new Locale("en-US")
    );

    Map<String, Object> map = new HashMap<>();
    mapper.map(STOP).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
    assertEquals("[{\"gtfsType\":100}]", map.get("routes"));
    assertEquals(STATION.getId().toString(), map.get("parentStation"));
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
    mapper.map(STOP).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("nameDE", map.get("name"));
    assertEquals("descDE", map.get("desc"));
  }
}
