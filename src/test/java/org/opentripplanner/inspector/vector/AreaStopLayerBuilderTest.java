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
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.StopModelBuilder;
import org.opentripplanner.transit.service.TransitModel;

class AreaStopLayerBuilderTest {

  private static final Coordinate[] COORDINATES = {
    new Coordinate(0, 0),
    new Coordinate(0, 1),
    new Coordinate(1, 1),
    new Coordinate(1, 0),
    new Coordinate(0, 0),
  };
  private static final FeedScopedId ID = new FeedScopedId("FEED", "ID");
  private static final I18NString NAME = new NonLocalizedString("Test stop");

  private final StopModelBuilder stopModelBuilder = StopModel.of();

  private final AreaStop areaStop = stopModelBuilder
    .areaStop(ID)
    .withName(NAME)
    .withGeometry(GeometryUtils.getGeometryFactory().createPolygon(COORDINATES))
    .build();

  @Test
  void map() {
    var subject = new DebugClientAreaStopPropertyMapper(
      new DefaultTransitService(new TransitModel()),
      Locale.ENGLISH
    );

    var properties = subject.map(areaStop);

    assertEquals(2, properties.size());
    assertTrue(properties.contains(new KeyValue("id", ID.toString())));
    assertTrue(properties.contains(new KeyValue("name", NAME.toString())));
  }
}
