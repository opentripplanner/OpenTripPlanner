package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.util.Locale;
import java.util.function.Consumer;

public class RoutingPreferencesBuilder {

  private final RoutingPreferences original;
  private TransitPreferences transit = null;
  private TransferPreferences transfer = null;
  private WalkPreferences walk = null;
  private StreetPreferences street = null;
  private WheelchairPreferences wheelchair = null;
  private BikePreferences bike = null;
  private CarPreferences car = null;
  private ScooterPreferences scooter = null;
  private SystemPreferences system = null;
  private ItineraryFilterPreferences itineraryFilter = null;
  private Locale locale = null;

  public RoutingPreferencesBuilder(RoutingPreferences original) {
    this.original = original;
  }

  public RoutingPreferences original() {
    return original;
  }

  public TransitPreferences transit() {
    return transit == null ? original.transit() : transit;
  }

  public RoutingPreferencesBuilder withTransit(Consumer<TransitPreferences.Builder> body) {
    this.transit = ifNotNull(this.transit, original.transit()).copyOf().apply(body).build();
    return this;
  }

  public TransferPreferences transfer() {
    return transfer == null ? original.transfer() : transfer;
  }

  public RoutingPreferencesBuilder withTransfer(Consumer<TransferPreferences.Builder> body) {
    this.transfer = ifNotNull(this.transfer, original.transfer()).copyOf().apply(body).build();
    return this;
  }

  public WalkPreferences walk() {
    return walk == null ? original.walk() : walk;
  }

  public RoutingPreferencesBuilder withWalk(Consumer<WalkPreferences.Builder> body) {
    this.walk = ifNotNull(this.walk, original.walk()).copyOf().apply(body).build();
    return this;
  }

  public StreetPreferences street() {
    return street == null ? original.street() : street;
  }

  public RoutingPreferencesBuilder withStreet(Consumer<StreetPreferences.Builder> body) {
    this.street = ifNotNull(this.street, original.street()).copyOf().apply(body).build();
    return this;
  }

  public WheelchairPreferences wheelchair() {
    return wheelchair == null ? original.wheelchair() : wheelchair;
  }

  public RoutingPreferencesBuilder withWheelchair(WheelchairPreferences wheelchair) {
    this.wheelchair = wheelchair;
    return this;
  }

  public RoutingPreferencesBuilder withWheelchair(Consumer<WheelchairPreferences.Builder> body) {
    this.wheelchair = ifNotNull(this.wheelchair, original.wheelchair())
      .copyOf()
      .apply(body)
      .build();
    return this;
  }

  public BikePreferences bike() {
    return bike == null ? original.bike() : bike;
  }

  public RoutingPreferencesBuilder withBike(Consumer<BikePreferences.Builder> body) {
    this.bike = ifNotNull(this.bike, original.bike()).copyOf().apply(body).build();
    return this;
  }

  public CarPreferences car() {
    return car == null ? original.car() : car;
  }

  public RoutingPreferencesBuilder withCar(Consumer<CarPreferences.Builder> body) {
    this.car = ifNotNull(this.car, original.car()).copyOf().apply(body).build();
    return this;
  }

  public ScooterPreferences scooter() {
    return scooter == null ? original.scooter() : scooter;
  }

  public RoutingPreferencesBuilder withScooter(Consumer<ScooterPreferences.Builder> body) {
    this.scooter = ifNotNull(this.scooter, original.scooter()).copyOf().apply(body).build();
    return this;
  }

  public SystemPreferences system() {
    return system == null ? original.system() : system;
  }

  public RoutingPreferencesBuilder withSystem(Consumer<SystemPreferences.Builder> body) {
    this.system = ifNotNull(this.system, original.system()).copyOf().apply(body).build();
    return this;
  }

  public ItineraryFilterPreferences itineraryFilter() {
    return itineraryFilter == null ? original.itineraryFilter() : itineraryFilter;
  }

  public RoutingPreferencesBuilder withItineraryFilter(
    Consumer<ItineraryFilterPreferences.Builder> body
  ) {
    this.itineraryFilter = ifNotNull(this.itineraryFilter, original.itineraryFilter())
      .copyOf()
      .apply(body)
      .build();
    return this;
  }

  public Locale locale() {
    return locale == null ? original.locale() : locale;
  }

  public RoutingPreferencesBuilder withLocale(Locale locale) {
    this.locale = ifNotNull(locale, original.locale());
    return this;
  }

  public RoutingPreferencesBuilder apply(Consumer<RoutingPreferencesBuilder> body) {
    body.accept(this);
    return this;
  }

  public RoutingPreferences build() {
    var value = new RoutingPreferences(this);
    return original.equals(value) ? original : value;
  }
}
