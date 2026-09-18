package org.opentripplanner.gtfs.config;

import org.opentripplanner.transit.model.site.StopTransferPriority;

public class GtfsDefaultParametersBuilder {

  private StopTransferPriority stationTransferPreference;
  private boolean discardMinTransferTimes;
  private boolean blockBasedInterlining;
  private int maxInterlineDistance;
  private boolean taxiProvider;

  public GtfsDefaultParametersBuilder(GtfsDefaultParameters original) {
    this.stationTransferPreference = original.stationTransferPreference();
    this.discardMinTransferTimes = original.discardMinTransferTimes();
    this.blockBasedInterlining = original.blockBasedInterlining();
    this.maxInterlineDistance = original.maxInterlineDistance();
    this.taxiProvider = original.taxiProvider();
  }

  public GtfsDefaultParametersBuilder withStationTransferPreference(
    StopTransferPriority stationTransferPreference
  ) {
    this.stationTransferPreference = stationTransferPreference;
    return this;
  }

  StopTransferPriority stationTransferPreference() {
    return stationTransferPreference;
  }

  public GtfsDefaultParametersBuilder withDiscardMinTransferTimes(boolean value) {
    this.discardMinTransferTimes = value;
    return this;
  }

  boolean discardMinTransferTimes() {
    return discardMinTransferTimes;
  }

  public GtfsDefaultParametersBuilder withBlockBasedInterlining(boolean value) {
    this.blockBasedInterlining = value;
    return this;
  }

  boolean blockBasedInterlining() {
    return blockBasedInterlining;
  }

  public GtfsDefaultParametersBuilder withMaxInterlineDistance(int value) {
    this.maxInterlineDistance = value;
    return this;
  }

  int maxInterlineDistance() {
    return maxInterlineDistance;
  }

  public GtfsDefaultParametersBuilder withTaxiProvider(boolean value) {
    this.taxiProvider = value;
    return this;
  }

  boolean taxiProvider() {
    return taxiProvider;
  }

  public GtfsDefaultParameters build() {
    return new GtfsDefaultParameters(this);
  }
}
