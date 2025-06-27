package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * @see GtfsApiParameters for documentation of parameters
 */
public class GtfsApiConfig implements GtfsApiParameters {

  private final Collection<String> tracingTags;

  public GtfsApiConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_8)
      .summary("Configuration for the GTFS GraphQL API.")
      .asObject();

    tracingTags = c
      .of("tracingTags")
      .summary("Used to group requests based on headers or query parameters when monitoring OTP.")
      .asStringList(Set.of());
  }

  @Override
  public Collection<String> tracingTags() {
    return tracingTags;
  }
}
