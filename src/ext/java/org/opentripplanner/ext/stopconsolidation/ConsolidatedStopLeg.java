package org.opentripplanner.ext.stopconsolidation;

import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model.basic.TransitMode;

public class ConsolidatedStopLeg implements TransitLeg {

  private final I18NString fromName;
  private final I18NString toName;
  private final TransitLeg original;

  public ConsolidatedStopLeg(TransitLeg original, I18NString fromName, I18NString toName) {
    this.original = original;
    this.fromName = fromName;
    this.toName = toName;
  }

  @Override
  public ZonedDateTime getStartTime() {
    return original.getStartTime();
  }

  @Override
  public ZonedDateTime getEndTime() {
    return original.getEndTime();
  }

  @Override
  public double getDistanceMeters() {
    return original.getDistanceMeters();
  }

  @Override
  public Place getFrom() {
    return Place.forStop(original.getFrom().stop, fromName);
  }

  @Override
  public Place getTo() {
    return Place.forStop(original.getFrom().stop, toName);
  }

  @Override
  public LineString getLegGeometry() {
    return original.getLegGeometry();
  }

  @Override
  public int getGeneralizedCost() {
    return original.getGeneralizedCost();
  }

  @Override
  public void setFareProducts(List<FareProductUse> products) {
    original.setFareProducts(products);
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return original.fareProducts();
  }

  @Nonnull
  @Override
  public TransitMode getMode() {
    return original.getMode();
  }
}
