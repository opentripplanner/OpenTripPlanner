/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class FareAttribute extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    private FeedScopedId id;

    private float price;

    private String currencyType;

    private int paymentMethod;

    private int transfers = MISSING_VALUE;

    private int transferDuration = MISSING_VALUE;

    /** youthPrice is an extension to the GTFS spec to support Seattle fare types. */
    private float youthPrice;

    /** seniorPrice is an extension to the GTFS spec to support Seattle fare types. */
    private float seniorPrice;

    /** This is a proposed extension to the GTFS spec */
    private int journeyDuration = MISSING_VALUE;

    public FareAttribute() {
    }

    public FareAttribute(FareAttribute fa) {
        this.id = fa.id;
        this.price = fa.price;
        this.currencyType = fa.currencyType;
        this.paymentMethod = fa.paymentMethod;
        this.transfers = fa.transfers;
        this.transferDuration = fa.transferDuration;
        this.journeyDuration = fa.journeyDuration;
    }

    @Override
    public FeedScopedId getId() {
        return id;
    }

    @Override
    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getCurrencyType() {
        return currencyType;
    }

    public void setCurrencyType(String currencyType) {
        this.currencyType = currencyType;
    }

    public int getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(int paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isTransfersSet() {
        return transfers != MISSING_VALUE;
    }

    public int getTransfers() {
        return transfers;
    }

    public void setTransfers(int transfers) {
        this.transfers = transfers;
    }

    public void clearTransfers() {
        this.transfers = MISSING_VALUE;
    }

    public boolean isTransferDurationSet() {
        return transferDuration != MISSING_VALUE;
    }

    public int getTransferDuration() {
        return transferDuration;
    }

    public void setTransferDuration(int transferDuration) {
        this.transferDuration = transferDuration;
    }

    public void clearTransferDuration() {
        this.transferDuration = MISSING_VALUE;
    }

    public boolean isJourneyDurationSet() {
        return journeyDuration != MISSING_VALUE;
    }

    public int getJourneyDuration() {
        return journeyDuration;
    }

    public void setJourneyDuration(int journeyDuration) {
        this.journeyDuration = journeyDuration;
    }

    public void clearJourneyDuration() {
        this.journeyDuration = MISSING_VALUE;
    }

    public String toString() {
        return "<FareAttribute " + getId() + ">";
    }

    public float getYouthPrice() {
        return youthPrice;
    }

    public void setYouthPrice(float youthPrice) {
        this.youthPrice = youthPrice;
    }

    public float getSeniorPrice() {
        return seniorPrice;
    }

    public void setSeniorPrice(float seniorPrice) {
        this.seniorPrice = seniorPrice;
    }

}
