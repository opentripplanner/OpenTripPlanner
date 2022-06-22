package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Map;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Mutable version of the {@link McCostParams}.
 */
@SuppressWarnings("UnusedReturnValue")
public class McCostParamsBuilder {

  private int boardCost;
  private int transferCost;
  private double[] transitReluctanceFactors;
  private double waitReluctanceFactor;
  private WheelchairAccessibilityRequest accessibilityRequest;
  private Map<FeedScopedId, Integer> routePenalties;

  public McCostParamsBuilder() {
    this(McCostParams.DEFAULTS);
  }

  private McCostParamsBuilder(McCostParams other) {
    this.boardCost = other.boardCost();
    this.transferCost = other.transferCost();
    this.transitReluctanceFactors = other.transitReluctanceFactors();
    this.waitReluctanceFactor = other.waitReluctanceFactor();
    this.accessibilityRequest = other.accessibilityRequirements();
    this.routePenalties = other.routePenaltyMap();
  }

  public int boardCost() {
    return boardCost;
  }

  public McCostParamsBuilder boardCost(int boardCost) {
    this.boardCost = boardCost;
    return this;
  }

  public int transferCost() {
    return transferCost;
  }

  public McCostParamsBuilder transferCost(int transferCost) {
    this.transferCost = transferCost;
    return this;
  }

  public double[] transitReluctanceFactors() {
    return transitReluctanceFactors;
  }

  public McCostParamsBuilder transitReluctanceFactors(double[] transitReluctanceFactors) {
    this.transitReluctanceFactors = transitReluctanceFactors;
    return this;
  }

  public double waitReluctanceFactor() {
    return waitReluctanceFactor;
  }

  public McCostParamsBuilder waitReluctanceFactor(double waitReluctanceFactor) {
    this.waitReluctanceFactor = waitReluctanceFactor;
    return this;
  }

  public WheelchairAccessibilityRequest wheelchairAccessibility() {
    return accessibilityRequest;
  }

  public McCostParamsBuilder wheelchairAccessibility(WheelchairAccessibilityRequest mode) {
    accessibilityRequest = mode;
    return this;
  }

  public Map<FeedScopedId, Integer> routePenalties() {
    return this.routePenalties;
  }

  public McCostParamsBuilder routePenalties(Map<FeedScopedId, Integer> routePenalties) {
    this.routePenalties = routePenalties;
    return this;
  }

  public McCostParams build() {
    return new McCostParams(this);
  }
}
