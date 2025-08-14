package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.list;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.map;
import static org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode.LOCAL;
import static org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode.REGIONAL_RAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;

class SelectRequestMapperTest {

  private static final SelectRequestMapper MAPPER = new SelectRequestMapper(
    new DefaultFeedIdMapper()
  );

  @Test
  void mapFullSelectRequest() throws JsonProcessingException {
    var result = MAPPER.mapSelectRequest(
      map(
        entry("lines", list("F:Line:1", "F:Line:2")),
        entry("authorities", list("F:Auth:1", "F:Auth:1")),
        entry("groupOfLines", list("F:GOL:1")),
        entry(
          "transportModes",
          list(
            map(entry("transportMode", BUS)),
            map(
              entry("transportMode", RAIL),
              entry("transportSubModes", List.of(LOCAL, REGIONAL_RAIL))
            )
          )
        )
      )
    );
    assertEquals(
      "(transportModes: [BUS, RAIL::local, RAIL::regionalRail], agencies: [F:Auth:1, F:Auth:1], routes: [F:Line:1, F:Line:2])",
      result.toString()
    );
  }

  @Test
  void mapEmptySelectRequest() throws JsonProcessingException {
    var result = MAPPER.mapSelectRequest(map());
    assertEquals("(transportModes: EMPTY)", result.toString());
  }
}
