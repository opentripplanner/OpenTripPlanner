package org.opentripplanner.routing.algorithm.raptoradapter.transit.request.transfercache;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.BikeRequest;
import org.opentripplanner.street.search.request.CarRequest;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.WalkRequest;
import org.opentripplanner.street.search.request.WheelchairRequest;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.utils.tostring.ToStringBuilder;

class RaptorRequestTransferCacheKey {

  private final List<List<Transfer>> transfersByStopIndex;
  private final StreetSearchRequest request;
  private final StreetRelevantOptions options;

  public RaptorRequestTransferCacheKey(
    List<List<Transfer>> transfersByStopIndex,
    RouteRequest request
  ) {
    this.transfersByStopIndex = transfersByStopIndex;
    this.request = StreetSearchRequestMapper.mapToTransferRequest(request).build();
    this.options = new StreetRelevantOptions(this.request);
  }

  public List<List<Transfer>> transfersByStopIndex() {
    return transfersByStopIndex;
  }

  public StreetSearchRequest request() {
    return request;
  }

  public StreetRelevantOptions options() {
    return options;
  }

  @Override
  public int hashCode() {
    // transfersByStopIndex is ignored on purpose since it should not change (there is only
    // one instance per graph) and calculating the hashCode() would be expensive
    return options.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RaptorRequestTransferCacheKey cacheKey = (RaptorRequestTransferCacheKey) o;
    // transfersByStopIndex is checked using == on purpose since the instance should not change
    // (there is only one instance per graph)
    return (
      transfersByStopIndex == cacheKey.transfersByStopIndex && options.equals(cacheKey.options)
    );
  }

  /**
   * This contains an extract of the parameters which may influence transfers.
   * <p>
   */
  private static class StreetRelevantOptions {

    private final StreetMode transferMode;
    private final boolean wheelchairEnabled;
    private final WalkRequest walk;
    private final BikeRequest bike;
    private final CarRequest car;
    private final WheelchairRequest wheelchair;
    private final double turnReluctance;

    public StreetRelevantOptions(StreetSearchRequest request) {
      this.transferMode = request.mode();
      this.wheelchairEnabled = request.wheelchairEnabled();

      this.walk = transferMode.includesWalking() ? request.walk() : WalkRequest.DEFAULT;
      this.bike = transferMode.includesBiking() ? request.bike() : BikeRequest.DEFAULT;
      this.car = transferMode.includesDriving() ? request.car() : CarRequest.DEFAULT;
      this.turnReluctance = request.turnReluctance();
      this.wheelchair = request.wheelchairEnabled()
        ? request.wheelchair()
        : WheelchairRequest.DEFAULT;
    }

    @Override
    public String toString() {
      return ToStringBuilder.of(StreetRelevantOptions.class)
        .addEnum("transferMode", transferMode)
        .addBoolIfTrue("wheelchairEnabled", wheelchairEnabled)
        .addObj("walk", walk, WalkRequest.DEFAULT)
        .addObj("bike", bike, BikeRequest.DEFAULT)
        .addObj("car", car, CarRequest.DEFAULT)
        .addNum("turnReluctance", turnReluctance)
        .addObj("wheelchair", wheelchair, WheelchairRequest.DEFAULT)
        .toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(
        transferMode,
        wheelchairEnabled,
        walk,
        bike,
        car,
        turnReluctance,
        wheelchair
      );
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof StreetRelevantOptions that)) {
        return false;
      }
      return (
        transferMode == that.transferMode &&
        wheelchairEnabled == that.wheelchairEnabled &&
        Objects.equals(that.walk, walk) &&
        Objects.equals(that.bike, bike) &&
        Objects.equals(that.car, car) &&
        Objects.equals(that.turnReluctance, turnReluctance) &&
        Objects.equals(that.wheelchair, wheelchair)
      );
    }
  }
}
