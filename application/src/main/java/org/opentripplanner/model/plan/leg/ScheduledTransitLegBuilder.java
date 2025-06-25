package org.opentripplanner.model.plan.leg;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class ScheduledTransitLegBuilder<B extends ScheduledTransitLegBuilder<B>> {

  /**
   * The position in pattern is initialized to a negative value. Negative valued are not allowed,
   * so if not set by the caller the build() method will fail.
   */
  private static final int POS_NOT_SET = -1;

  private TripTimes tripTimes;
  private TripPattern tripPattern;
  private int boardStopIndexInPattern = POS_NOT_SET;
  private int alightStopIndexInPattern = POS_NOT_SET;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private LocalDate serviceDate;
  private ZoneId zoneId;
  private TripOnServiceDate tripOnServiceDate;
  private ConstrainedTransfer transferFromPreviousLeg;
  private ConstrainedTransfer transferToNextLeg;
  private int generalizedCost;
  private Set<TransitAlert> alerts = Set.of();
  private Double distanceMeters;

  // Sandbox fields
  private Float accessibilityScore;
  private Emission emissionPerPerson;
  private List<FareOffer> fareOffers = List.of();

  public ScheduledTransitLegBuilder() {}

  public ScheduledTransitLegBuilder(ScheduledTransitLeg original) {
    tripTimes = original.tripTimes();
    tripPattern = original.tripPattern();
    boardStopIndexInPattern = original.boardStopPosInPattern();
    alightStopIndexInPattern = original.alightStopPosInPattern();
    startTime = original.startTime();
    endTime = original.endTime();
    serviceDate = original.serviceDate();
    tripOnServiceDate = original.tripOnServiceDate();
    transferFromPreviousLeg = original.transferFromPrevLeg();
    transferToNextLeg = original.transferToNextLeg();
    generalizedCost = original.generalizedCost();
    zoneId = original.zoneId();
    alerts = original.listTransitAlerts();
    distanceMeters = original.distanceMeters();
    fareOffers = original.fareOffers();

    // Sandbox fields
    accessibilityScore = original.accessibilityScore();
    emissionPerPerson = original.emissionPerPerson();
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

  public Float accessibilityScore() {
    return accessibilityScore;
  }

  public Emission emissionPerPerson() {
    return emissionPerPerson;
  }

  public B withEmissionPerPerson(Emission emissionPerPerson) {
    this.emissionPerPerson = emissionPerPerson;
    return instance();
  }

  public List<FareOffer> fareOffers() {
    return fareOffers;
  }

  public B withFareProducts(List<FareOffer> fareProducts) {
    this.fareOffers = Objects.requireNonNull(fareProducts);
    return instance();
  }

  public ScheduledTransitLeg build() {
    return new ScheduledTransitLeg(this);
  }

  final B instance() {
    return (B) this;
  }
}
