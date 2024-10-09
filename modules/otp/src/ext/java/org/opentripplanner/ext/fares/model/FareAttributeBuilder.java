package org.opentripplanner.ext.fares.model;

import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareAttributeBuilder
  extends AbstractEntityBuilder<FareAttribute, FareAttributeBuilder> {

  private FeedScopedId agency;
  private Money price;

  private int paymentMethod;

  private Integer transfers;

  private Integer transferDuration;

  private Integer journeyDuration;

  FareAttributeBuilder(FeedScopedId id) {
    super(id);
  }

  FareAttributeBuilder(FareAttribute original) {
    super(original.getId());
    this.agency = original.getAgency();
    this.price = original.getPrice();
    this.paymentMethod = original.getPaymentMethod();
    this.transfers = original.getTransfers();
    this.transferDuration = original.getTransferDuration();
    this.journeyDuration = original.getJourneyDuration();
  }

  public FeedScopedId agency() {
    return agency;
  }

  public FareAttributeBuilder setAgency(FeedScopedId agency) {
    this.agency = agency;
    return this;
  }

  public Money price() {
    return price;
  }

  public FareAttributeBuilder setPrice(Money price) {
    this.price = price;
    return this;
  }

  public int paymentMethod() {
    return paymentMethod;
  }

  public FareAttributeBuilder setPaymentMethod(int paymentMethod) {
    this.paymentMethod = paymentMethod;
    return this;
  }

  public Integer transfers() {
    return transfers;
  }

  public FareAttributeBuilder setTransfers(int transfers) {
    this.transfers = transfers;
    return this;
  }

  public Integer transferDuration() {
    return transferDuration;
  }

  public FareAttributeBuilder setTransferDuration(int transferDuration) {
    this.transferDuration = transferDuration;
    return this;
  }

  public Integer journeyDuration() {
    return journeyDuration;
  }

  public FareAttributeBuilder setJourneyDuration(int journeyDuration) {
    this.journeyDuration = journeyDuration;
    return this;
  }

  @Override
  protected FareAttribute buildFromValues() {
    return new FareAttribute(this);
  }
}
