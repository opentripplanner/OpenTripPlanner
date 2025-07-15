package org.opentripplanner.model.plan.walkstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.walkstep.AbsoluteDirection.EAST;
import static org.opentripplanner.model.plan.walkstep.AbsoluteDirection.NORTH;
import static org.opentripplanner.model.plan.walkstep.AbsoluteDirection.SOUTHWEST;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;

public class WalkStepTest {

  @Test
  public void testRelativeDirection() {
    WalkStepBuilder builder = new WalkStepBuilder()
      .withDirectionText(new NonLocalizedString("Any"))
      .withStartLocation(new WgsCoordinate(3.0, 4.0))
      .withNameIsDerived(false)
      .withAngle(0.0)
      .withWalkingBike(false)
      .withArea(false);

    double angle1 = degreesToRadians(0);
    double angle2 = degreesToRadians(90);

    builder.withDirections(angle1, angle2, false);
    var step = builder.build();
    Assertions.assertEquals(RelativeDirection.RIGHT, step.getRelativeDirection());
    assertEquals(Optional.of(EAST), step.getAbsoluteDirection());

    angle1 = degreesToRadians(0);
    angle2 = degreesToRadians(5);

    builder.withDirections(angle1, angle2, false);
    step = builder.build();
    assertEquals(RelativeDirection.CONTINUE, step.getRelativeDirection());
    assertEquals(Optional.of(NORTH), step.getAbsoluteDirection());

    angle1 = degreesToRadians(0);
    angle2 = degreesToRadians(240);

    builder.withDirections(angle1, angle2, false);
    step = builder.build();
    assertEquals(RelativeDirection.HARD_LEFT, step.getRelativeDirection());
    assertEquals(Optional.of(SOUTHWEST), step.getAbsoluteDirection());
  }

  private double degreesToRadians(double deg) {
    return (deg * Math.PI) / 180;
  }
}
