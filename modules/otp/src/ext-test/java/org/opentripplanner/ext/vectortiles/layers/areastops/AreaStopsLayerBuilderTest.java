package org.opentripplanner.ext.vectortiles.layers.areastops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.StopModelBuilder;
import org.opentripplanner.transit.service.TransitModel;

class AreaStopsLayerBuilderTest {

  private static final FeedScopedId ID = new FeedScopedId("FEED", "ID");
  private static final I18NString NAME = I18NString.of("Test stop");
  private static final String CONFIG =
    """
  {
    "vectorTiles": {
      "layers" : [
        {
          "name": "areaStops",
          "type": "AreaStop",
          "mapper": "OTPRR",
          "maxZoom": 20,
          "minZoom": 14,
          "cacheMaxSeconds": 60,
          "expansionFactor": 0
        }
      ]
    }
  }
  """;
  private static final LayerParameters<VectorTilesResource.LayerType> LAYER_CONFIG = VectorTileConfig
    .mapVectorTilesParameters(newNodeAdapterForTest(CONFIG), "vectorTiles")
    .layers()
    .getFirst();

  private final StopModelBuilder stopModelBuilder = StopModel.of();

  private final AreaStop AREA_STOP = stopModelBuilder
    .areaStop(ID)
    .withName(NAME)
    .withGeometry(Polygons.BERLIN)
    .build();

  private final TransitModel transitModel = new TransitModel(
    stopModelBuilder.withAreaStop(AREA_STOP).build(),
    new Deduplicator()
  );

  @Test
  void getAreaStops() {
    transitModel.index();

    var subject = new AreaStopsLayerBuilder(
      new DefaultTransitService(transitModel),
      LAYER_CONFIG,
      Locale.ENGLISH
    );
    var geometries = subject.getGeometries(AREA_STOP.getGeometry().getEnvelopeInternal());
    assertEquals(List.of(Polygons.BERLIN), geometries);
  }
}
