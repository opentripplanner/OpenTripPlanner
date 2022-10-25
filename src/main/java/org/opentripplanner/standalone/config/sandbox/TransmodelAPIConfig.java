package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.ext.transmodelapi.TransmodelAPIParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * @see TransmodelAPIParameters for documentation of parameters
 */
public class TransmodelAPIConfig implements TransmodelAPIParameters {

  private final boolean hideFeedId;
  private final Collection<String> tracingHeaderTags;

  public TransmodelAPIConfig(NodeAdapter node) {
    hideFeedId = node.of("hideFeedId").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false);
    tracingHeaderTags =
      node.of("tracingHeaderTags").withDoc(NA, /*TODO DOC*/"TODO").asStringList(Set.of());
  }

  @Override
  public boolean hideFeedId() {
    return hideFeedId;
  }

  @Override
  public Collection<String> tracingHeaderTags() {
    return tracingHeaderTags;
  }
}
