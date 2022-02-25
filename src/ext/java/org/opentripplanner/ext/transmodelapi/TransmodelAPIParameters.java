package org.opentripplanner.ext.transmodelapi;

import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;

import java.util.Collection;

/**
 * Transmodel API parameters. These parameters configure the behaviour of some aspects of the
 * Transmodel GraphQL API
 */
public interface TransmodelAPIParameters {

    /**
     * Should the feed id be left out from input and output mapping of the ID scalars
     */
    boolean hideFeedId();

    /**
     * Which HTTP headers should be used as tags for performance metering in the Actuator API
     *
     * @see MicrometerGraphQLInstrumentation
     */
    Collection<String> tracingHeaderTags();
}
