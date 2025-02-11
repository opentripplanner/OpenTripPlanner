package org.opentripplanner.raptorlegacy._data.transit;

import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;

import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.street.model.vertex.StreetLocation;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * Utility class for creating access/egress for unit-tests.
 */
public class TestAccessEgress {

  public static final int ZERO = 0;
  public static final boolean STOP_REACHED_ON_BOARD = true;
  public static final boolean STOP_REACHED_ON_FOOT = false;
  public static final double DEFAULT_WALK_RELUCTANCE = 2.0;

  public static RoutingAccessEgress free(int stop) {
    return new Builder(stop, 0).withFree().build();
  }

  /**
   * @deprecated A stop cannot be both free and have a cost - This is not a valid
   *             access/egress.
   */
  @Deprecated
  public static RoutingAccessEgress free(int stop, int cost) {
    return new Builder(stop, 0).withFree().withCost(cost).build();
  }

  public static RoutingAccessEgress walk(int stop, int durationInSeconds) {
    return new Builder(stop, durationInSeconds).build();
  }

  public static RoutingAccessEgress walk(int stop, int durationInSeconds, int cost) {
    return new Builder(stop, durationInSeconds).withCost(cost).build();
  }

  public static RoutingAccessEgress flexWithOnBoard(int stop, int durationInSeconds, int cost) {
    return new Builder(stop, durationInSeconds)
      .withCost(cost)
      .withNRides(1)
      .stopReachedOnBoard()
      .build();
  }

  /** Create a new flex access and arrive stop onBoard. */
  public static RoutingAccessEgress flex(int stop, int durationInSeconds, int nRides, int cost) {
    IntUtils.requireInRange(nRides, 1, 100);
    return new Builder(stop, durationInSeconds)
      .stopReachedOnBoard()
      .withNRides(nRides)
      .withCost(cost)
      .build();
  }

  public static int walkCost(int durationInSeconds) {
    return walkCost(durationInSeconds, DEFAULT_WALK_RELUCTANCE);
  }

  public static int walkCost(int durationInSeconds, double reluctance) {
    return toRaptorCost(durationInSeconds * reluctance);
  }

  /**
   * Do not use the builder, use the static factory methods. Only use the builder if you need to
   * override the {@link TestAccessEgress class}.
   */
  protected static class Builder {

    int stop;
    int durationInSeconds;
    int generalizedCost;
    int numberOfRides = ZERO;
    boolean stopReachedOnBoard = STOP_REACHED_ON_FOOT;
    private boolean free = false;

    Builder(int stop, int durationInSeconds) {
      this.stop = stop;
      this.durationInSeconds = durationInSeconds;
      this.generalizedCost = walkCost(durationInSeconds);
    }

    Builder withFree() {
      this.free = true;
      this.durationInSeconds = 0;
      return this;
    }

    Builder withCost(int generalizedCost) {
      this.generalizedCost = generalizedCost;
      return this;
    }

    Builder withNRides(int numberOfRides) {
      this.numberOfRides = numberOfRides;
      return this;
    }

    Builder stopReachedOnBoard() {
      this.stopReachedOnBoard = STOP_REACHED_ON_BOARD;
      return this;
    }

    RoutingAccessEgress build() {
      var stopId = "Stop:" + stop;
      var lastState = new State(
        new StreetLocation(stopId, Coordinates.BOSTON, I18NString.of(stopId)),
        StreetSearchRequest.of().build()
      );
      return new DefaultAccessEgress(
        stop,
        durationInSeconds,
        generalizedCost,
        TimeAndCost.ZERO,
        lastState
      ) {
        // TODO - Use the domain type FlexAccessEgressAdapter here instead [if numberOfRides > 0]
        @Override
        public int numberOfRides() {
          return numberOfRides;
        }

        @Override
        public boolean stopReachedOnBoard() {
          return stopReachedOnBoard;
        }
      };
    }
  }
}
