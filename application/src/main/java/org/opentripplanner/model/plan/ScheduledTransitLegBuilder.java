package org.opentripplanner.model.plan;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class ScheduledTransitLegBuilder<B extends ScheduledTransitLegBuilder<B>> {

  private TripTimes tripTimes;
  private TripPattern tripPattern;
  private int boardStopIndexInPattern;
  private int alightStopIndexInPattern;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private LocalDate serviceDate;
  private ZoneId zoneId;
  private TripOnServiceDate tripOnServiceDate;
  private ConstrainedTransfer transferFromPreviousLeg;
  private ConstrainedTransfer transferToNextLeg;
  private int generalizedCost;
  private Float accessibilityScore;
  private Set<TransitAlert> alerts = Set.of();
  private Double distanceMeters;
  private List<FareProductUse> fareProducts = List.of();

  public ScheduledTransitLegBuilder() {}

  public ScheduledTransitLegBuilder(ScheduledTransitLeg original) {
    tripTimes = original.getTripTimes();
    tripPattern = original.getTripPattern();
    boardStopIndexInPattern = original.getBoardStopPosInPattern();
    alightStopIndexInPattern = original.getAlightStopPosInPattern();
    startTime = original.getStartTime();
    endTime = original.getEndTime();
    serviceDate = original.getServiceDate();
    tripOnServiceDate = original.getTripOnServiceDate();
    transferFromPreviousLeg = original.getTransferFromPrevLeg();
    transferToNextLeg = original.getTransferToNextLeg();
    generalizedCost = original.getGeneralizedCost();
    accessibilityScore = original.accessibilityScore();
    zoneId = original.getZoneId();
    alerts = original.getTransitAlerts();
    distanceMeters = original.getDistanceMeters();
    fareProducts = original.fareProducts();
  }

  public B withTripTimes(TripTimes tripTimes) {
    this.tripTimes = tripTimes;
    return instance();
  }

  public TripTimes tripTimes() {
    return tripTimes;
  }

  public B withTripPattern(TripPattern tripPattern) {
    this.tripPattern = tripPattern;
    return instance();
  }

  public TripPattern tripPattern() {
    return tripPattern;
  }

  public B withBoardStopIndexInPattern(int boardStopIndexInPattern) {
    this.boardStopIndexInPattern = boardStopIndexInPattern;
    return instance();
  }

  public int boardStopIndexInPattern() {
    return boardStopIndexInPattern;
  }

  public B withAlightStopIndexInPattern(int alightStopIndexInPattern) {
    this.alightStopIndexInPattern = alightStopIndexInPattern;
    return instance();
  }

  public int alightStopIndexInPattern() {
    return alightStopIndexInPattern;
  }

  public B withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return instance();
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public B withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return instance();
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public B withServiceDate(LocalDate serviceDate) {
    this.serviceDate = serviceDate;
    return instance();
  }

  public LocalDate serviceDate() {
    return serviceDate;
  }

  public B withZoneId(ZoneId zoneId) {
    this.zoneId = zoneId;
    return instance();
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public B withTripOnServiceDate(TripOnServiceDate tripOnServiceDate) {
    this.tripOnServiceDate = tripOnServiceDate;
    return instance();
  }

  public TripOnServiceDate tripOnServiceDate() {
    return tripOnServiceDate;
  }

  public B withTransferFromPreviousLeg(ConstrainedTransfer transferFromPreviousLeg) {
    this.transferFromPreviousLeg = transferFromPreviousLeg;
    return instance();
  }

  public ConstrainedTransfer transferFromPreviousLeg() {
    return transferFromPreviousLeg;
  }

  public B withTransferToNextLeg(ConstrainedTransfer transferToNextLeg) {
    this.transferToNextLeg = transferToNextLeg;
    return instance();
  }

  public ConstrainedTransfer transferToNextLeg() {
    return transferToNextLeg;
  }

  public B withGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
    return instance();
  }

  public int generalizedCost() {
    return generalizedCost;
  }

  public B withAccessibilityScore(Float accessibilityScore) {
    this.accessibilityScore = accessibilityScore;
    return instance();
  }

  public Float accessibilityScore() {
    return accessibilityScore;
  }

  public B withAlerts(Set<TransitAlert> alerts) {
    this.alerts = Objects.requireNonNull(alerts);
    return instance();
  }

  public Set<TransitAlert> alerts() {
    return alerts;
  }

  public B withDistanceMeters(double distance) {
    this.distanceMeters = distance;
    return instance();
  }

  public Double distanceMeters() {
    return distanceMeters;
  }

  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  public B withFareProducts(List<FareProductUse> fareProducts) {
    this.fareProducts = Objects.requireNonNull(fareProducts);
    return instance();
  }

  public ScheduledTransitLeg build() {
    return new ScheduledTransitLeg(this);
  }

  final B instance() {
    return (B) this;
  }
}
