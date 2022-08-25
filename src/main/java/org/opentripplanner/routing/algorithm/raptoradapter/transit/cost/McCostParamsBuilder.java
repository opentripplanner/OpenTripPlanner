package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.BitSet;
import java.util.Set;
import java.util.function.DoubleFunction;
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
  private BitSet unpreferredPatterns;
  private DoubleFunction<Double> unpreferredCost;

  public McCostParamsBuilder() {
    this(McCostParams.DEFAULTS);
  }

  private McCostParamsBuilder(McCostParams other) {
    this.boardCost = other.boardCost();
    this.transferCost = other.transferCost();
    this.transitReluctanceFactors = other.transitReluctanceFactors();
    this.waitReluctanceFactor = other.waitReluctanceFactor();
    this.accessibilityRequest = other.accessibilityRequirements();
    this.unpreferredPatterns = other.unpreferredPatterns();
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

  public BitSet unpreferredPatterns() {
    return unpreferredPatterns;
  }

  public McCostParamsBuilder unpreferredPatterns(BitSet patterns) {
    this.unpreferredPatterns = patterns;
    return this;
  }

  public DoubleFunction<Double> unpreferredCost() {
    return unpreferredCost;
  }

  public McCostParamsBuilder unpreferredCost(DoubleFunction<Double> unpreferredCost) {
    this.unpreferredCost = unpreferredCost;
    return this;
  }

  public McCostParams build() {
    return new McCostParams(this);
  }
}
