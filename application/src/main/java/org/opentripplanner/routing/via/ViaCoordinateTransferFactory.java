package org.opentripplanner.routing.via;

import java.util.List;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;

/**
 * This class is a factory for creating {@link ViaCoordinateTransfer}s. It is injected into
 * the transit router using dependency injection.
 */
public interface ViaCoordinateTransferFactory {
  List<ViaCoordinateTransfer> createViaTransfers(
    RouteRequest request,
    String label,
    WgsCoordinate coordinate
  );
}
