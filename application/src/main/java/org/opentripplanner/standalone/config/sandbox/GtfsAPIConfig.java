package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.apis.gtfs.GtfsAPIParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * @see GtfsAPIParameters for documentation of parameters
 */
public class GtfsAPIConfig implements GtfsAPIParameters {

  private final Collection<String> tracingHeaderTags;

  public GtfsAPIConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_8)
      .summary("Configuration for the GTFS GraphQL API.")
      .asObject();

    tracingHeaderTags = c
      .of("tracingHeaderTags")
      .summary("Used to group requests when monitoring OTP.")
      .asStringList(Set.of());
  }

  @Override
  public Collection<String> tracingHeaderTags() {
    return tracingHeaderTags;
  }
}
