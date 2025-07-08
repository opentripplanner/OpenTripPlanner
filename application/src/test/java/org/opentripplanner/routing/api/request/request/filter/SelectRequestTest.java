package org.opentripplanner.routing.api.request.request.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class SelectRequestTest {

  static final MainAndSubMode TC_BUS = new MainAndSubMode(TransitMode.BUS);
  static final MainAndSubMode TC_FERRY = new MainAndSubMode(TransitMode.FERRY);
  static final MainAndSubMode TC_RAIL = new MainAndSubMode(TransitMode.RAIL);
  static final MainAndSubMode TC_TRAM = new MainAndSubMode(TransitMode.TRAM);
  static final MainAndSubMode TC_LOCAL_BUS = new MainAndSubMode(
    TransitMode.BUS,
    SubMode.of("LOCAL")
  );
  static final MainAndSubMode TC_LOCAL_TRAM = new MainAndSubMode(
    TransitMode.TRAM,
    SubMode.of("LOCAL")
  );

  @Test
  void testModesToString() {
    assertEquals("(transportModes: EMPTY)", modesSelect().toString());
    assertEquals("(transportModes: [BUS])", modesSelect(TC_BUS).toString());
    assertEquals(
      "(transportModes: [BUS::LOCAL, FERRY])",
      modesSelect(TC_FERRY, TC_LOCAL_BUS).toString()
    );
    assertEquals(
      "(transportModes: ALL)",
      SelectRequest.of().withTransportModes(MainAndSubMode.all()).build().toString()
    );
    assertEquals("(transportModes: NOT [FERRY])", notModesSelect(TC_FERRY).toString());
    assertEquals(
      "(transportModes: NOT [BUS, FERRY, TRAM])",
      notModesSelect(TC_BUS, TC_TRAM, TC_FERRY).toString()
    );
    assertEquals(
      "(transportModes: [AIRPLANE, CABLE_CAR, CARPOOL, COACH, FUNICULAR, GONDOLA, MONORAIL, SUBWAY, TAXI, TROLLEYBUS])",
      notModesSelect(TC_BUS, TC_FERRY, TC_TRAM, TC_RAIL).toString()
    );
    var list = new ArrayList<>(
      MainAndSubMode.notMainModes(List.of(TC_BUS, TC_TRAM, TC_LOCAL_TRAM))
    );
    list.add(TC_LOCAL_BUS);
    assertEquals(
      "(transportModes: [AIRPLANE, BUS::LOCAL, CABLE_CAR, CARPOOL, COACH, FERRY, FUNICULAR, GONDOLA, MONORAIL, RAIL, SUBWAY, TAXI, TROLLEYBUS])",
      SelectRequest.of().withTransportModes(list).build().toString()
    );
  }

  static SelectRequest modesSelect(MainAndSubMode... modes) {
    return SelectRequest.of().withTransportModes(Arrays.asList(modes)).build();
  }

  static SelectRequest notModesSelect(MainAndSubMode... modes) {
    return SelectRequest.of()
      .withTransportModes(MainAndSubMode.notMainModes(Arrays.asList(modes)))
      .build();
  }
}
