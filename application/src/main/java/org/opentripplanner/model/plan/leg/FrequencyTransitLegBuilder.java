package org.opentripplanner.model.plan.leg;

public class FrequencyTransitLegBuilder
  extends ScheduledTransitLegBuilder<FrequencyTransitLegBuilder> {

  private int frequencyHeadwayInSeconds;

  public FrequencyTransitLegBuilder() {}

  public FrequencyTransitLegBuilder(FrequencyTransitLeg original) {
    super(original);
    frequencyHeadwayInSeconds = original.headway();
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
