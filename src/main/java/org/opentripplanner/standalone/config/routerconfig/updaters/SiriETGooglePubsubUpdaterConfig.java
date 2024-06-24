package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.ext.siri.updater.google.SiriETGooglePubsubUpdaterParameters.INITIAL_GET_DATA_TIMEOUT;
import static org.opentripplanner.ext.siri.updater.google.SiriETGooglePubsubUpdaterParameters.RECONNECT_PERIOD;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.ext.siri.updater.google.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriETGooglePubsubUpdaterConfig {

  public static SiriETGooglePubsubUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETGooglePubsubUpdaterParameters(
      configRef,
      c
        .of("feedId")
        .since(NA)
        .summary("The ID of the feed to apply the updates to.")
        .asString(null),
      c
        .of("subscriptionProjectName")
        .since(NA)
        .summary("The Google Cloud project that hosts the PubSub subscription.")
        .description(
          """
        During startup, the updater creates a PubSub subscription that listens
        to the PubSub topic that publishes SIRI-ET updates.
        This parameter specifies in which Google Cloud project the subscription will be created.
        The topic and the subscription can be hosted in two different projects.
        """
        )
        .asString(),
      c
        .of("topicProjectName")
        .since(NA)
        .summary("The Google Cloud project that hosts the PubSub topic that publishes the updates.")
        .asString(),
      c
        .of("topicName")
        .since(NA)
        .summary("The name of the PubSub topic that publishes the updates.")
        .asString(),
      c
        .of("dataInitializationUrl")
        .since(NA)
        .summary("URL used to download over HTTP the recent history of SIRI-ET messages.")
        .description(
          """
          Optionally the updater can download the recent history of SIRI-ET messages from this URL.
          If this parameter is set, the updater will be marked as initialized (primed) only when
          the message history is fully downloaded and applied.
          """
        )
        .asString(null),
      c
        .of("reconnectPeriod")
        .since(NA)
        .summary("Wait this amount of time before trying to reconnect to the PubSub subscription.")
        .description(
          """
            In case of a network error, the updater will try periodically to reconnect to the
            Google PubSub subscription.
            """
        )
        .asDuration(RECONNECT_PERIOD),
      c
        .of("initialGetDataTimeout")
        .since(NA)
        .summary("Timeout for retrieving the recent history of SIRI-ET messages.")
        .description(
          """
          When trying to fetch the message history over HTTP, the updater will wait this amount
          of time for the connection to be established.
          If the connection times out, the updater will retry indefinitely with exponential backoff.
          """
        )
        .asDuration(INITIAL_GET_DATA_TIMEOUT),
      c
        .of("fuzzyTripMatching")
        .since(NA)
        .summary("If the trips should be matched fuzzily.")
        .asBoolean(false)
    );
  }
}
