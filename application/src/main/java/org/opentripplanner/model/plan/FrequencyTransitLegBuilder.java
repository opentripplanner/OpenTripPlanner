package org.opentripplanner.model.plan;

public class FrequencyTransitLegBuilder
  extends ScheduledTransitLegBuilder<FrequencyTransitLegBuilder> {

  private int frequencyHeadwayInSeconds;

  public FrequencyTransitLegBuilder() {}

  public FrequencyTransitLegBuilder(FrequencyTransitLeg original) {
    super(original);
    frequencyHeadwayInSeconds = original.getHeadway();
  }

  public FrequencyTransitLegBuilder withFrequencyHeadwayInSeconds(int frequencyHeadwayInSeconds) {
    this.frequencyHeadwayInSeconds = frequencyHeadwayInSeconds;
    return instance();
  }

  public int frequencyHeadwayInSeconds() {
    return frequencyHeadwayInSeconds;
  }

  @Override
  public FrequencyTransitLeg build() {
    return new FrequencyTransitLeg(this);
  }
}
