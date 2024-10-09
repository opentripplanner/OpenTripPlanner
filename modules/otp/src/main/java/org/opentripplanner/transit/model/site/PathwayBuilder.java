package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@SuppressWarnings("UnusedReturnValue")
public class PathwayBuilder extends AbstractEntityBuilder<Pathway, PathwayBuilder> {

  private PathwayMode pathwayMode;

  private StationElement<?, ?> fromStop;

  private StationElement<?, ?> toStop;

  private String signpostedAs;

  private String reverseSignpostedAs;

  private int traversalTime;

  private double length;

  private int stairCount;

  private double slope;

  private boolean isBidirectional;

  PathwayBuilder(FeedScopedId id) {
    super(id);
  }

  PathwayBuilder(Pathway original) {
    super(original);
    this.pathwayMode = original.getPathwayMode();
    this.fromStop = original.getFromStop();
    this.toStop = original.getToStop();
    this.signpostedAs = original.getSignpostedAs();
    this.reverseSignpostedAs = original.getReverseSignpostedAs();
    this.traversalTime = original.getTraversalTime();
    this.length = original.getLength();
    this.stairCount = original.getStairCount();
    this.slope = original.getSlope();
    this.isBidirectional = original.isBidirectional();
  }

  public PathwayMode pathwayMode() {
    return pathwayMode;
  }

  public PathwayBuilder withPathwayMode(PathwayMode pathwayMode) {
    this.pathwayMode = pathwayMode;
    return this;
  }

  public StationElement<?, ?> fromStop() {
    return fromStop;
  }

  public PathwayBuilder withFromStop(StationElement<?, ?> fromStop) {
    this.fromStop = fromStop;
    return this;
  }

  public StationElement<?, ?> toStop() {
    return toStop;
  }

  public PathwayBuilder withToStop(StationElement<?, ?> toStop) {
    this.toStop = toStop;
    return this;
  }

  public String signpostedAs() {
    return signpostedAs;
  }

  public PathwayBuilder withSignpostedAs(String name) {
    this.signpostedAs = name;
    return this;
  }

  public String reverseSignpostedAs() {
    return reverseSignpostedAs;
  }

  public PathwayBuilder withReverseSignpostedAs(String reversedName) {
    this.reverseSignpostedAs = reversedName;
    return this;
  }

  public int traversalTime() {
    return traversalTime;
  }

  public PathwayBuilder withTraversalTime(int traversalTime) {
    this.traversalTime = traversalTime;
    return this;
  }

  public double length() {
    return length;
  }

  public PathwayBuilder withLength(double length) {
    this.length = length;
    return this;
  }

  public boolean isBidirectional() {
    return isBidirectional;
  }

  public PathwayBuilder withIsBidirectional(boolean bidirectional) {
    isBidirectional = bidirectional;
    return this;
  }

  public int stairCount() {
    return stairCount;
  }

  public PathwayBuilder withStairCount(int stairCount) {
    this.stairCount = stairCount;
    return this;
  }

  public double slope() {
    return slope;
  }

  public PathwayBuilder withSlope(double slope) {
    this.slope = slope;
    return this;
  }

  @Override
  protected Pathway buildFromValues() {
    return new Pathway(this);
  }
}
