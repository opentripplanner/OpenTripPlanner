package org.opentripplanner.ext.vectortiles.layers.stops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class StopsLayerTest {

  private RegularStop stop;

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
}
