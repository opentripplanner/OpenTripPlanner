package org.opentripplanner.service.vehiclerental.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetModelFactory;

class StreetVehicleRentalLinkTest {

  @Test
  void createBidirectionalLinks() {
    var rentalVertex = StreetModelFactory.rentalVertex(RentalFormFactor.BICYCLE);
    var streetVertex = intersectionVertex(59.9, 10.7);

    var links = StreetVehicleRentalLink.createBidirectionalLinks(rentalVertex, streetVertex);

    assertEquals(2, links.size());

    // First link: rental → street
    var rentalToStreet = links.get(0);
    assertInstanceOf(StreetVehicleRentalLink.class, rentalToStreet);
    assertEquals(rentalVertex, rentalToStreet.getFromVertex());
    assertEquals(streetVertex, rentalToStreet.getToVertex());

    // Second link: street → rental
    var streetToRental = links.get(1);
    assertInstanceOf(StreetVehicleRentalLink.class, streetToRental);
    assertEquals(streetVertex, streetToRental.getFromVertex());
    assertEquals(rentalVertex, streetToRental.getToVertex());
  }
}
