package org.opentripplanner.standalone.config.sandbox;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.ext.transmodelapi.TransmodelAPIParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

/**
 * @see TransmodelAPIParameters for documentation of parameters
 */
public class TransmodelAPIConfig implements TransmodelAPIParameters {

    private final boolean hideFeedId;
    private final Collection<String> tracingHeaderTags;

    public TransmodelAPIConfig(NodeAdapter node) {
        hideFeedId = node.asBoolean("hideFeedId", false);
        tracingHeaderTags = node.asTextSet("tracingHeaderTags", Set.of());
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
