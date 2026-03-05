package org.opentripplanner.routing.via;

import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This class is a factory for creating {@link ViaCoordinateTransfer}s. It is injected into
 * the transit router using dependency injection.
 */
public interface ViaCoordinateTransferFactory {
  List<ViaCoordinateTransfer> createViaTransfers(
    RouteRequest request,
    Vertex viaVertex,
    WgsCoordinate coordinate
  );
}
