package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_6;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * @see TransmodelAPIParameters for documentation of parameters
 */
public class TransmodelAPIConfig implements TransmodelAPIParameters {

  private final boolean hideFeedId;
  private final Collection<String> tracingHeaderTags;
  private final int maxNumberOfResultFields;

  public TransmodelAPIConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_1)
      .summary("Configuration for the Transmodel GraphQL API.")
      .asObject();

    hideFeedId = c
      .of("hideFeedId")
      .summary("Hide the FeedId in all API output, and add it to input.")
      .description(
        "Only turn this feature on if you have unique ids across all feeds, without the " +
        "feedId prefix."
      )
      .asBoolean(false);
    tracingHeaderTags = c
      .of("tracingHeaderTags")
      .summary("Used to group requests when monitoring OTP.")
      .asStringList(Set.of());

    maxNumberOfResultFields = c
      .of("maxNumberOfResultFields")
      .since(V2_6)
      .summary("The maximum number of fields in a GraphQL result")
      .description(
        "Enforce rate limiting based on query complexity; Queries that return too much data are" +
        " cancelled."
      )
      .asInt(1_000_000);
  }

  @Override
  public boolean hideFeedId() {
    return hideFeedId;
  }

  @Override
  public Collection<String> tracingHeaderTags() {
    return tracingHeaderTags;
  }

  @Override
  public int maxNumberOfResultFields() {
    return maxNumberOfResultFields;
  }
}
