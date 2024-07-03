# Siri-ET Google PubSub Updater

Support for consuming SIRI-ET messages over a Google Cloud PubSub subscription.
Similarly to the SIRI-ET HTTP updater, this updater is developed to support the Nordic SIRI profile
which is a subset of the SIRI specification.

## Contact Info
Entur, Norway
https://entur.no/

## Documentation

This updater consumes SIRI real time information over an asynchronous publisher/subscriber feed
provided by a Google Cloud PubSub topic.

For more documentation see
the [Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI-ET Google PubSub updater you need to add it to the updaters section
of the `router-config.json`.

### Siri-ET via Google PubSub

<!-- siri-et-google-pubsub-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                           |    Type    | Summary                                                                          |  Req./Opt. | Default Value | Since |
|------------------------------------------------------------|:----------:|----------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-et-google-pubsub-updater"                     |   `enum`   | The type of the updater.                                                         | *Required* |               |  1.5  |
| [dataInitializationUrl](#u__13__dataInitializationUrl)     |  `string`  | URL used to download over HTTP the recent history of SIRI-ET messages.           | *Optional* |               |  2.1  |
| feedId                                                     |  `string`  | The ID of the feed to apply the updates to.                                      | *Optional* |               |  2.1  |
| fuzzyTripMatching                                          |  `boolean` | If the trips should be matched fuzzily.                                          | *Optional* | `false`       |  2.1  |
| [initialGetDataTimeout](#u__13__initialGetDataTimeout)     | `duration` | Timeout for retrieving the recent history of SIRI-ET messages.                   | *Optional* | `"PT30S"`     |  2.1  |
| [reconnectPeriod](#u__13__reconnectPeriod)                 | `duration` | Wait this amount of time before trying to reconnect to the PubSub subscription.  | *Optional* | `"PT30S"`     |  2.1  |
| [subscriptionProjectName](#u__13__subscriptionProjectName) |  `string`  | The Google Cloud project that hosts the PubSub subscription.                     | *Required* |               |  2.1  |
| topicName                                                  |  `string`  | The name of the PubSub topic that publishes the updates.                         | *Required* |               |  2.1  |
| topicProjectName                                           |  `string`  | The Google Cloud project that hosts the PubSub topic that publishes the updates. | *Required* |               |  2.1  |


##### Parameter details

<h4 id="u__13__dataInitializationUrl">dataInitializationUrl</h4>

**Since version:** `2.1` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[13] 

URL used to download over HTTP the recent history of SIRI-ET messages.

Optionally the updater can download the recent history of SIRI-ET messages from this URL.
If this parameter is set, the updater will be marked as initialized (primed) only when
the message history is fully downloaded and applied.


<h4 id="u__13__initialGetDataTimeout">initialGetDataTimeout</h4>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30S"`   
**Path:** /updaters/[13] 

Timeout for retrieving the recent history of SIRI-ET messages.

When trying to fetch the message history over HTTP, the updater will wait this amount
of time for the connection to be established.
If the connection times out, the updater will retry indefinitely with exponential backoff.


<h4 id="u__13__reconnectPeriod">reconnectPeriod</h4>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30S"`   
**Path:** /updaters/[13] 

Wait this amount of time before trying to reconnect to the PubSub subscription.

In case of a network error, the updater will try periodically to reconnect to the
Google PubSub subscription.


<h4 id="u__13__subscriptionProjectName">subscriptionProjectName</h4>

**Since version:** `2.1` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[13] 

The Google Cloud project that hosts the PubSub subscription.

During startup, the updater creates a PubSub subscription that listens
to the PubSub topic that publishes SIRI-ET updates.
This parameter specifies in which Google Cloud project the subscription will be created.
The topic and the subscription can be hosted in two different projects.




##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-et-google-pubsub-updater",
      "feedId" : "feed_id",
      "reconnectPeriod" : "5s",
      "initialGetDataTimeout" : "1m20s",
      "topicProjectName" : "google_pubsub_topic_project_name",
      "subscriptionProjectName" : "google_pubsub_subscription_project_name",
      "topicName" : "estimated_timetables",
      "dataInitializationUrl" : "https://example.com/some/path",
      "fuzzyTripMatching" : true
    }
  ]
}
```

<!-- siri-et-google-pubsub-updater END -->

