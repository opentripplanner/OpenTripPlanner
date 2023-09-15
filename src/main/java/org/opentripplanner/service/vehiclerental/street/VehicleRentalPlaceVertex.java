package org.opentripplanner.service.vehiclerental.street;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;

/**
 * A vertex for a rental vehicle or station. It is connected to the streets by a
 * {@link StreetVehicleRentalLink}. To allow transitions on and
 * off a vehicle, it has {@link VehicleRentalEdge} loop edges.
 */
public class VehicleRentalPlaceVertex extends Vertex {

  private VehicleRentalPlace station;

  public VehicleRentalPlaceVertex(VehicleRentalPlace station) {
    super(station.getLongitude(), station.getLatitude());
    this.station = station;
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return station.getName();
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("vehicle rental station " + station.getId());
  }

  public VehicleRentalPlace getStation() {
    return station;
  }

  public void setStation(VehicleRentalPlace station) {
    this.station = station;
  }
}
