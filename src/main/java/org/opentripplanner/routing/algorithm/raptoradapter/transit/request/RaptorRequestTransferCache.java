package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityPreferences;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RoutingContext;

public class RaptorRequestTransferCache {

  private final LoadingCache<CacheKey, RaptorTransferIndex> transferCache;

  public RaptorRequestTransferCache(int maximumSize) {
    transferCache = CacheBuilder.newBuilder().maximumSize(maximumSize).build(cacheLoader());
  }

  public LoadingCache<CacheKey, RaptorTransferIndex> getTransferCache() {
    return transferCache;
  }

  public RaptorTransferIndex get(
    List<List<Transfer>> transfersByStopIndex,
    RoutingContext routingContext
  ) {
    try {
      return transferCache.get(new CacheKey(transfersByStopIndex, routingContext));
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to get item from transfer cache", e);
    }
  }

  private CacheLoader<CacheKey, RaptorTransferIndex> cacheLoader() {
    return new CacheLoader<>() {
      @Override
      public RaptorTransferIndex load(@javax.annotation.Nonnull CacheKey cacheKey) {
        return RaptorTransferIndex.create(cacheKey.transfersByStopIndex, cacheKey.routingContext);
      }
    };
  }

  private static class CacheKey {

    private final List<List<Transfer>> transfersByStopIndex;
    private final RoutingContext routingContext;
    private final StreetRelevantOptions options;

    private CacheKey(List<List<Transfer>> transfersByStopIndex, RoutingContext routingContext) {
      this.transfersByStopIndex = transfersByStopIndex;
      this.routingContext = routingContext;
      this.options = new StreetRelevantOptions(routingContext.opt);
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
      CacheKey cacheKey = (CacheKey) o;
      // transfersByStopIndex is checked using == on purpose since the instance should not change
      // (there is only one instance per graph)
      return (
        transfersByStopIndex == cacheKey.transfersByStopIndex && options.equals(cacheKey.options)
      );
    }
  }

  /**
   * This contains an extract of the parameters which may influence transfers. The possible values
   * are somewhat limited by rounding in {@link Transfer#prepareTransferRoutingRequest(RoutingRequest, RoutingPreferences)}.
   * <p>
   * TODO VIA: the bikeWalking options are not used.
   * TODO VIA: Should we use StreetPreferences instead?
   */
  private static class StreetRelevantOptions {

    private final StreetMode transferMode;
    private final BicycleOptimizeType optimize;
    private final TimeSlopeSafetyTriangle bikeOptimizeTimeSlopeSafety;
    private final WheelchairAccessibilityPreferences wheelchairAccessibility;
    private final double walkSpeed;
    private final double bikeSpeed;
    private final double walkReluctance;
    private final double stairsReluctance;
    private final double stairsTimeFactor;
    private final double turnReluctance;
    private final int elevatorBoardCost;
    private final int elevatorBoardTime;
    private final int elevatorHopCost;
    private final int elevatorHopTime;
    private final int bikeSwitchCost;
    private final int bikeSwitchTime;

    public StreetRelevantOptions(RouteRequest routingRequest) {
      var preferences = routingRequest.preferences();

      this.transferMode = routingRequest.journey().transfer().mode();

      this.optimize = preferences.bike().optimizeType();
      this.bikeOptimizeTimeSlopeSafety = preferences.bike().optimizeTriangle();
      this.bikeSwitchCost = preferences.bike().switchCost();
      this.bikeSwitchTime = preferences.bike().switchTime();
      this.wheelchairAccessibility = preferences.wheelchairAccessibility().round();

      this.walkSpeed = preferences.walk().speed();
      this.bikeSpeed = preferences.bike().speed();

      this.walkReluctance = preferences.walk().reluctance();
      this.stairsReluctance = preferences.walk().stairsReluctance();
      this.stairsTimeFactor = preferences.walk().stairsTimeFactor();
      this.turnReluctance = preferences.street().turnReluctance();

      this.elevatorBoardCost = preferences.street().elevatorBoardCost();
      this.elevatorBoardTime = preferences.street().elevatorBoardTime();
      this.elevatorHopCost = preferences.street().elevatorHopCost();
      this.elevatorHopTime = preferences.street().elevatorHopTime();
    }

    @Override
    public int hashCode() {
      return Objects.hash(
        transferMode,
        optimize,
        bikeOptimizeTimeSlopeSafety,
        wheelchairAccessibility,
        walkSpeed,
        bikeSpeed,
        walkReluctance,
        stairsReluctance,
        turnReluctance,
        elevatorBoardCost,
        elevatorBoardTime,
        elevatorHopCost,
        elevatorHopTime,
        bikeSwitchCost,
        bikeSwitchTime,
        stairsTimeFactor
      );
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final StreetRelevantOptions that = (StreetRelevantOptions) o;
      return (
        Double.compare(that.walkSpeed, walkSpeed) == 0 &&
        Double.compare(that.bikeSpeed, bikeSpeed) == 0 &&
        Double.compare(that.walkReluctance, walkReluctance) == 0 &&
        Double.compare(that.stairsReluctance, stairsReluctance) == 0 &&
        Double.compare(that.stairsTimeFactor, stairsTimeFactor) == 0 &&
        Double.compare(that.turnReluctance, turnReluctance) == 0 &&
        Objects.equals(that.bikeOptimizeTimeSlopeSafety, bikeOptimizeTimeSlopeSafety) &&
        wheelchairAccessibility.equals(that.wheelchairAccessibility) &&
        elevatorBoardCost == that.elevatorBoardCost &&
        elevatorBoardTime == that.elevatorBoardTime &&
        elevatorHopCost == that.elevatorHopCost &&
        elevatorHopTime == that.elevatorHopTime &&
        bikeSwitchCost == that.bikeSwitchCost &&
        bikeSwitchTime == that.bikeSwitchTime &&
        transferMode == that.transferMode &&
        optimize == that.optimize
      );
    }
  }
}
