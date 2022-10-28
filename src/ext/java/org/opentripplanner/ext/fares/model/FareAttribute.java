/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.ext.fares.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class FareAttribute
  extends AbstractTransitEntity<FareAttribute, FareAttributeBuilder> {

  private FeedScopedId agency;
  private final float price;

  private final String currencyType;

  private final int paymentMethod;

  private final Integer transfers;

  private final Integer transferDuration;

  /** youthPrice is an extension to the GTFS spec to support Seattle fare types. */
  private final float youthPrice;

  /** seniorPrice is an extension to the GTFS spec to support Seattle fare types. */
  private final float seniorPrice;

  /** This is a proposed extension to the GTFS spec */
  private final Integer journeyDuration;

  FareAttribute(FareAttributeBuilder builder) {
    super(builder.getId());
    this.price = builder.price();
    this.currencyType = builder.currencyType();
    this.paymentMethod = builder.paymentMethod();
    this.transfers = builder.transfers();
    this.transferDuration = builder.transferDuration();
    this.youthPrice = builder.youthPrice();
    this.seniorPrice = builder.seniorPrice();
    this.journeyDuration = builder.journeyDuration();
    this.agency = builder.agency();
  }

  public static FareAttributeBuilder of(FeedScopedId id) {
    return new FareAttributeBuilder(id);
  }

  public boolean isAgencySet() {
    return agency != null;
  }

  public FeedScopedId getAgency() {
    return agency;
  }

  public void setAgency(FeedScopedId agency) {
    this.agency = agency;
  }

  public float getPrice() {
    return price;
  }

  public String getCurrencyType() {
    return currencyType;
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

  public float getYouthPrice() {
    return youthPrice;
  }

  public float getSeniorPrice() {
    return seniorPrice;
  }

  @Override
  public boolean sameAs(@Nonnull FareAttribute other) {
    return (
      getId().equals(other.getId()) &&
      price == other.getPrice() &&
      Objects.equals(currencyType, other.getCurrencyType()) &&
      paymentMethod == other.getPaymentMethod() &&
      Objects.equals(transfers, other.getTransfers()) &&
      Objects.equals(transferDuration, other.getTransferDuration()) &&
      youthPrice == other.getYouthPrice() &&
      seniorPrice == other.getSeniorPrice() &&
      Objects.equals(journeyDuration, other.getJourneyDuration())
    );
  }

  @Nonnull
  @Override
  public FareAttributeBuilder copy() {
    return new FareAttributeBuilder(this);
  }
}
