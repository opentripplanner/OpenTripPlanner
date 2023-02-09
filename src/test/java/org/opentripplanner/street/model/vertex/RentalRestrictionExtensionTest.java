package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.model.vertex.RentalRestrictionExtension.BusinessAreaBorder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RentalRestrictionExtensionTest {

  static String network = "tier-oslo";
  RentalRestrictionExtension a = new BusinessAreaBorder("a");
  RentalRestrictionExtension b = new BusinessAreaBorder("b");
  RentalRestrictionExtension c = new RentalRestrictionExtension.GeofencingZoneExtension(
    new GeofencingZone(new FeedScopedId(network, "a-park"), null, true, false)
  );

  @Test
  void addToBase() {
    var newA = RentalRestrictionExtension.NO_RESTRICTION.add(a);
    assertSame(a, newA);
    assertEquals(1, newA.toList().size());
  }

  @Test
  void addToItself() {
    var unchanged = a.add(a);
    assertSame(a, unchanged);
  }

  @Test
  void add() {
    var composite = a.add(b);
    assertInstanceOf(RentalRestrictionExtension.Composite.class, composite);
  }

  @Test
  void differentType() {
    var composite = a.add(c);
    assertInstanceOf(RentalRestrictionExtension.Composite.class, composite);
  }

  @Test
  void composite() {
    var composite = a.add(b);
    assertInstanceOf(RentalRestrictionExtension.Composite.class, composite);
    var newComposite = composite.add(c);
    assertInstanceOf(RentalRestrictionExtension.Composite.class, newComposite);

    var c1 = (RentalRestrictionExtension.Composite) newComposite;
    var exts = c1.toList();
    assertEquals(3, exts.size());

    var c2 = (RentalRestrictionExtension.Composite) c1.add(a);
    assertEquals(3, c2.toList().size());
    // convert to sets so the order doesn't matter
    assertEquals(Set.of(a, b, c), Set.copyOf(c2.toList()));
  }
}
