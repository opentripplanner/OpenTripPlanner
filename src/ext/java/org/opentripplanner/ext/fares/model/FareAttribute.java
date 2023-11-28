/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.ext.fares.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class FareAttribute
  extends AbstractTransitEntity<FareAttribute, FareAttributeBuilder> {

  private FeedScopedId agency;
  private final Money price;

  private final int paymentMethod;

  private final Integer transfers;

  private final Integer transferDuration;

  /** This is a proposed extension to the GTFS spec */
  private final Integer journeyDuration;

  FareAttribute(FareAttributeBuilder builder) {
    super(builder.getId());
    this.price = Objects.requireNonNull(builder.price());
    this.paymentMethod = builder.paymentMethod();
    this.transfers = builder.transfers();
    this.transferDuration = builder.transferDuration();
    this.journeyDuration = builder.journeyDuration();
    this.agency = builder.agency();
  }

  public static FareAttributeBuilder of(FeedScopedId id) {
    return new FareAttributeBuilder(id);
  }

  public FeedScopedId getAgency() {
    return agency;
  }

  public void setAgency(FeedScopedId agency) {
    this.agency = agency;
  }

  public Money getPrice() {
    return price;
  }

  public int getPaymentMethod() {
    return paymentMethod;
  }

  public boolean isTransfersSet() {
    return transfers != null;
  }

  public Integer getTransfers() {
    return transfers;
  }

  public boolean isTransferDurationSet() {
    return transferDuration != null;
  }

  public Integer getTransferDuration() {
    return transferDuration;
  }

  public boolean isJourneyDurationSet() {
    return journeyDuration != null;
  }

  public Integer getJourneyDuration() {
    return journeyDuration;
  }

  @Override
  public boolean sameAs(@Nonnull FareAttribute other) {
    return (
      getId().equals(other.getId()) &&
      getPrice().equals(other.getPrice()) &&
      paymentMethod == other.getPaymentMethod() &&
      Objects.equals(transfers, other.getTransfers()) &&
      Objects.equals(transferDuration, other.getTransferDuration()) &&
      Objects.equals(journeyDuration, other.getJourneyDuration())
    );
  }

  @Nonnull
  @Override
  public FareAttributeBuilder copy() {
    return new FareAttributeBuilder(this);
  }
}
