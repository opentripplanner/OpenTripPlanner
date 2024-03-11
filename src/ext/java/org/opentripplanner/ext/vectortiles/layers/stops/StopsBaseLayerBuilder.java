package org.opentripplanner.ext.vectortiles.layers.stops;

import static java.util.Map.entry;

import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.service.TransitService;

public class StopsBaseLayerBuilder extends StopsLayerBuilder {

  public StopsBaseLayerBuilder(
    TransitService service,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      service,
      Map.ofEntries(
        entry(MapperType.Digitransit, new DigitransitStopPropertyMapper(service, locale)),
        entry(
          MapperType.DigitransitRealtime,
          new DigitransitRealtimeStopPropertyMapper(service, locale)
        )
      ),
      layerParameters
    );
  }
}
