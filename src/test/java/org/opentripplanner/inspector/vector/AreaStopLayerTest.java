package org.opentripplanner.inspector.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class AreaStopLayerBuilderTest {

  public static final Coordinate[] COORDINATES = {
    new Coordinate(0, 0),
    new Coordinate(0, 1),
    new Coordinate(1, 1),
    new Coordinate(1, 0),
    new Coordinate(0, 0),
  };
  public static final FeedScopedId ID = new FeedScopedId("FEED", "ID");
  public static final I18NString NAME = new NonLocalizedString("Test stop");

  public static final AreaStop AREA_STOP = AreaStop
    .of(ID)
    .withName(NAME)
    .withGeometry(GeometryUtils.getGeometryFactory().createPolygon(COORDINATES))
    .build();

  @Test
  void map() {
    var subject = new DebugClientAreaStopPropertyMapper(
      new DefaultTransitService(new TransitModel()),
      Locale.ENGLISH
    );

    var properties = subject.map(AREA_STOP);

    assertEquals(2, properties.size());
    assertTrue(properties.contains(new KeyValue("id", ID.toString())));
    assertTrue(properties.contains(new KeyValue("name", NAME.toString())));
  }
}
