package org.opentripplanner.service.vehiclerental.street;

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
    super(station.longitude(), station.latitude());
    this.station = station;
  }

  @Override
  public I18NString getName() {
    return station.name();
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("vehicle rental station " + station.id());
  }

  public VehicleRentalPlace getStation() {
    return station;
  }

  public void setStation(VehicleRentalPlace station) {
    this.station = station;
  }
}
