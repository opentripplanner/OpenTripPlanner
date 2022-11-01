package org.opentripplanner.ext.fares.model;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareAttributeBuilder
  extends AbstractEntityBuilder<FareAttribute, FareAttributeBuilder> {

  private FeedScopedId agency;
  private float price;

  private String currencyType;

  private int paymentMethod;

  private Integer transfers;

  private Integer transferDuration;

  private float youthPrice;

  private float seniorPrice;

  private Integer journeyDuration;

  FareAttributeBuilder(FeedScopedId id) {
    super(id);
  }

  FareAttributeBuilder(FareAttribute original) {
    super(original.getId());
    this.agency = original.getAgency();
    this.price = original.getPrice();
    this.currencyType = original.getCurrencyType();
    this.paymentMethod = original.getPaymentMethod();
    this.transfers = original.getTransfers();
    this.transferDuration = original.getTransferDuration();
    this.youthPrice = original.getYouthPrice();
    this.seniorPrice = original.getSeniorPrice();
    this.journeyDuration = original.getJourneyDuration();
  }

  public FeedScopedId agency() {
    return agency;
  }

  public FareAttributeBuilder setAgency(FeedScopedId agency) {
    this.agency = agency;
    return this;
  }

  public float price() {
    return price;
  }

  public FareAttributeBuilder setPrice(float price) {
    this.price = price;
    return this;
  }

  public String currencyType() {
    return currencyType;
  }

  public FareAttributeBuilder setCurrencyType(String currencyType) {
    this.currencyType = currencyType;
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

  public float youthPrice() {
    return youthPrice;
  }

  public FareAttributeBuilder setYouthPrice(float youthPrice) {
    this.youthPrice = youthPrice;
    return this;
  }

  public float seniorPrice() {
    return seniorPrice;
  }

  public FareAttributeBuilder setSeniorPrice(float seniorPrice) {
    this.seniorPrice = seniorPrice;
    return this;
  }

  @Override
  protected FareAttribute buildFromValues() {
    return new FareAttribute(this);
  }
}
