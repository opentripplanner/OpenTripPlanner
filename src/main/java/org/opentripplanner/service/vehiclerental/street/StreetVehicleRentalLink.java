package org.opentripplanner.service.vehiclerental.street;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * This represents the connection between a street vertex and a bike rental station vertex.
 */
public class StreetVehicleRentalLink extends Edge {

  private final VehicleRentalPlaceVertex vehicleRentalPlaceVertex;

  private StreetVehicleRentalLink(StreetVertex fromv, VehicleRentalPlaceVertex tov) {
    super(fromv, tov);
    vehicleRentalPlaceVertex = tov;
  }

  private StreetVehicleRentalLink(VehicleRentalPlaceVertex fromv, StreetVertex tov) {
    super(fromv, tov);
    vehicleRentalPlaceVertex = fromv;
  }

  public static StreetVehicleRentalLink createStreetVehicleRentalLink(
    StreetVertex fromv,
    VehicleRentalPlaceVertex tov
  ) {
    return connectToGraph(new StreetVehicleRentalLink(fromv, tov));
  }

  public static StreetVehicleRentalLink createStreetVehicleRentalLink(
    VehicleRentalPlaceVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new StreetVehicleRentalLink(fromv, tov));
  }

  public String toString() {
    return "StreetVehicleRentalLink(" + fromv + " -> " + tov + ")";
  }

  @Override
  @Nonnull
  public State[] traverse(State s0) {
    // Disallow traversing two StreetBikeRentalLinks in a row.
    // This prevents the router from using bike rental stations as shortcuts to get around
    // turn restrictions.
    if (s0.getBackEdge() instanceof StreetVehicleRentalLink) {
      return State.empty();
    }

    var preferences = s0.getPreferences().rental(s0.getRequest().mode());
    // preferences will be null while finding nearest places with WALK mode
    if (
      preferences != null && vehicleRentalPlaceVertex.getStation().networkIsNotAllowed(preferences)
    ) {
      return State.empty();
    }

    StateEditor s1 = s0.edit(this);
    //assume bike rental stations are more-or-less on-street
    s1.incrementWeight(1);
    s1.setBackMode(null);
    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return vehicleRentalPlaceVertex.getName();
  }
}
