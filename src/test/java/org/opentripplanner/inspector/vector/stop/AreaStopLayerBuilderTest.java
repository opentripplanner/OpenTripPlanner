package org.opentripplanner.inspector.vector.stop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.StopModelBuilder;

class AreaStopLayerBuilderTest {

  private static final FeedScopedId ID = new FeedScopedId("FEED", "ID");
  private static final I18NString NAME = I18NString.of("Test stop");

  private final StopModelBuilder stopModelBuilder = StopModel.of();

  private final AreaStop areaStop = stopModelBuilder
    .areaStop(ID)
    .withName(NAME)
    .withGeometry(Polygons.BERLIN)
    .build();

  @Test
  void map() {
    var subject = new StopLocationPropertyMapper(Locale.ENGLISH);

    var properties = subject.map(areaStop);

    assertTrue(properties.contains(new KeyValue("id", ID.toString())));
    assertTrue(properties.contains(new KeyValue("name", NAME.toString())));
  }
}
