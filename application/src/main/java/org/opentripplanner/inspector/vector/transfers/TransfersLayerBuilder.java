package org.opentripplanner.inspector.vector.transfers;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.vectortiles.model.LayerType;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.service.TransitService;

public class TransfersLayerBuilder extends LayerBuilder<PathTransfer> {

  private final TransitService transitService;
  private final RegularTransferService transferService;

  public TransfersLayerBuilder(
    TransitService siteRepo,
    RegularTransferService transferService,
    LayerParameters<LayerType> layerParameters
  ) {
    super(
      new PathTransferPropertyMapper(),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.transitService = siteRepo;
    this.transferService = transferService;
  }

  @Override
  protected List<Geometry> findGeometries(Envelope envelope) {
    return transitService
      .findAreaStops(envelope)
      .stream()
      .flatMap(stop -> transferService.findWalkTransfersFromStop(stop).stream())
      .map(p -> {
        var g = (Geometry) p.getGeometry();
        g.setUserData(p);
        return g;
      })
      .toList();
  }
}
