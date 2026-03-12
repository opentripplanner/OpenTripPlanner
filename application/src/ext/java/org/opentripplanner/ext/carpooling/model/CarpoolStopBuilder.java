package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.IntSupplier;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;

public class CarpoolStopBuilder extends AbstractEntityBuilder<CarpoolStop, CarpoolStopBuilder> {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private final IntSupplier indexCounter;
  private I18NString name;
  private I18NString description;
  private I18NString url;
  private WgsCoordinate coordinate;
  private Geometry geometry;

  private CarpoolStopType carpoolStopType;
  private ZonedDateTime expectedArrivalTime;
  private ZonedDateTime aimedArrivalTime;
  private ZonedDateTime expectedDepartureTime;
  private ZonedDateTime aimedDepartureTime;
  private int sequenceNumber;
  private int passengerDelta;

  CarpoolStopBuilder(FeedScopedId id, IntSupplier indexCounter) {
    super(id);
    this.indexCounter = Objects.requireNonNull(indexCounter);
  }

  CarpoolStopBuilder(CarpoolStop original) {
    super(original);
    this.indexCounter = original::getIndex;
    // Optional fields
    this.name = original.getName();
    this.description = original.getDescription();
    this.url = original.getUrl();
    this.coordinate = original.getCoordinate();
    this.geometry = original.getGeometry();
    this.sequenceNumber = original.getSequenceNumber();

    this.carpoolStopType = original.getCarpoolStopType();
    this.expectedArrivalTime = original.getExpectedArrivalTime();
    this.aimedArrivalTime = original.getAimedArrivalTime();
    this.expectedDepartureTime = original.getExpectedDepartureTime();
    this.aimedDepartureTime = original.getAimedDepartureTime();
    this.passengerDelta = original.getPassengerDelta();
  }

  @Override
  protected CarpoolStop buildFromValues() {
    return new CarpoolStop(this);
  }

  public CarpoolStopBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public CarpoolStopBuilder withDescription(I18NString description) {
    this.description = description;
    return this;
  }

  public CarpoolStopBuilder withUrl(I18NString url) {
    this.url = url;
    return this;
  }

  public CarpoolStopBuilder withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    this.geometry = toGeometry(coordinate);
    return this;
  }

  public CarpoolStopBuilder withCarpoolStopType(CarpoolStopType carpoolStopType) {
    this.carpoolStopType = carpoolStopType;
    return this;
  }

  public CarpoolStopBuilder withExpectedArrivalTime(ZonedDateTime expectedArrivalTime) {
    this.expectedArrivalTime = expectedArrivalTime;
    return this;
  }

  public CarpoolStopBuilder withAimedArrivalTime(ZonedDateTime aimedArrivalTime) {
    this.aimedArrivalTime = aimedArrivalTime;
    return this;
  }

  public CarpoolStopBuilder withExpectedDepartureTime(ZonedDateTime expectedDepartureTime) {
    this.expectedDepartureTime = expectedDepartureTime;
    return this;
  }

  public CarpoolStopBuilder withAimedDepartureTime(ZonedDateTime aimedDepartureTime) {
    this.aimedDepartureTime = aimedDepartureTime;
    return this;
  }

  public CarpoolStopBuilder withSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
    return this;
  }

  public CarpoolStopBuilder withPassengerDelta(int passengerDelta) {
    this.passengerDelta = passengerDelta;
    return this;
  }

  int createIndex() {
    return indexCounter.getAsInt();
  }

  public I18NString name() {
    return name;
  }

  public I18NString description() {
    return description;
  }

  public I18NString url() {
    return url;
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  public Geometry geometry() {
    return geometry;
  }

  public CarpoolStopType carpoolStopType() {
    return carpoolStopType;
  }

  public ZonedDateTime expectedArrivalTime() {
    return expectedArrivalTime;
  }

  public ZonedDateTime aimedArrivalTime() {
    return aimedArrivalTime;
  }

  public ZonedDateTime expectedDepartureTime() {
    return expectedDepartureTime;
  }

  public ZonedDateTime aimedDepartureTime() {
    return aimedDepartureTime;
  }

  public int sequenceNumber() {
    return sequenceNumber;
  }

  public int passengerDelta() {
    return passengerDelta;
  }

  private Geometry toGeometry(WgsCoordinate coordinate) {
    return GEOMETRY_FACTORY.createPoint(coordinate.asJtsCoordinate());
  }
}
