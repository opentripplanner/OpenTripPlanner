package org.opentripplanner.inspector.vector.rental;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Selects all rental places.
 * <p>
 * Note that the envelope query is relatively inefficient as it doesn't use an index to speed
 * up the queries. I'm unsure if it matters though.
 */
public class RentalLayerBuilder extends LayerBuilder<VehicleRentalPlace> {

  private final VehicleRentalService service;

  public RentalLayerBuilder(VehicleRentalService service, LayerParameters layerParameters) {
    super(new RentalPropertyMapper(), layerParameters.name(), layerParameters.expansionFactor());
    this.service = service;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope env) {
    return service
      .getVehicleRentalPlacesForEnvelope(env)
      .stream()
      .map(place -> {
        Geometry geometry = GeometryUtils.getGeometryFactory()
          .createPoint(new Coordinate(place.getLongitude(), place.getLatitude()));
        geometry.setUserData(place);
        return geometry;
      })
      .toList();
  }
}
