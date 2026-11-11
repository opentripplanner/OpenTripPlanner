# SIRI-ET Google PubSub Updater

Support for consuming SIRI-ET messages over a Google Cloud PubSub subscription. Similarly to the
SIRI-ET HTTP updater, this updater is developed to support the Nordic SIRI profile which is a subset
of the SIRI specification.

## Contact Info

Entur, Norway https://entur.no/

## Documentation

This updater consumes SIRI real time information over an asynchronous publisher/subscriber feed
provided by a Google Cloud PubSub topic.

For more documentation see the
[Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and the
[Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI-ET Google PubSub updater you need to add it to the updaters section of the
`router-config.json`.

### SIRI-ET via Google PubSub

<!-- siri-et-google-pubsub-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                            |    Type    | Summary                                                                                     |  Req./Opt. | Default Value | Since |
|-------------------------------------------------------------|:----------:|---------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-et-google-pubsub-updater"                      |   `enum`   | The type of the updater.                                                                    | *Required* |               |  1.5  |
| [dataInitializationUrl](#u__12__dataInitializationUrl)      |  `string`  | URL used to download over HTTP the recent history of SIRI-ET messages.                      | *Optional* |               |  2.1  |
| feedId                                                      |  `string`  | The ID of the feed to apply the updates to.                                                 | *Optional* |               |  2.1  |
| [initialGetDataTimeout](#u__12__initialGetDataTimeout)      | `duration` | Timeout for retrieving the recent history of SIRI-ET messages.                              | *Optional* | `"PT30S"`     |  2.1  |
| producerMetrics                                             |  `boolean` | If failure, success, and warning metrics should be collected per producer.                  | *Optional* | `false`       |  2.7  |
| [reconnectPeriod](#u__12__reconnectPeriod)                  | `duration` | Wait this amount of time before trying to reconnect to the PubSub subscription.             | *Optional* | `"PT30S"`     |  2.1  |
| [subscriptionProjectName](#u__12__subscriptionProjectName)  |  `string`  | The Google Cloud project that hosts the PubSub subscription.                                | *Required* |               |  2.1  |
| topicName                                                   |  `string`  | The name of the PubSub topic that publishes the updates.                                    | *Required* |               |  2.1  |
| topicProjectName                                            |  `string`  | The Google Cloud project that hosts the PubSub topic that publishes the updates.            | *Required* |               |  2.1  |
| [fuzzyTripMatching](#u__12__fuzzyTripMatching)              |  `object`  | Whether and how the fuzzy trip matcher should be used. A boolean or an object with options. | *Optional* |               |  2.1  |
|    [timeTolerance](#u__12__fuzzyTripMatching_timeTolerance) | `duration` | The maximum difference between the scheduled and the aimed times.                           | *Optional* | `"PT0S"`      |  2.11 |


##### Parameter details

<h4 id="u__12__dataInitializationUrl">dataInitializationUrl</h4>

**Since version:** `2.1` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[12] 

URL used to download over HTTP the recent history of SIRI-ET messages.

Optionally the updater can download the recent history of SIRI-ET messages from this URL.
If this parameter is set, the updater will be marked as initialized (primed) only when
the message history is fully downloaded and applied.


<h4 id="u__12__initialGetDataTimeout">initialGetDataTimeout</h4>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30S"`   
**Path:** /updaters/[12] 

Timeout for retrieving the recent history of SIRI-ET messages.

When trying to fetch the message history over HTTP, the updater will wait this amount
of time for the connection to be established.
If the connection times out, the updater will retry indefinitely with exponential backoff.


<h4 id="u__12__reconnectPeriod">reconnectPeriod</h4>

**Since version:** `2.1` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30S"`   
**Path:** /updaters/[12] 

Wait this amount of time before trying to reconnect to the PubSub subscription.

In case of a network error, the updater will try periodically to reconnect to the
Google PubSub subscription.


<h4 id="u__12__subscriptionProjectName">subscriptionProjectName</h4>

**Since version:** `2.1` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[12] 

The Google Cloud project that hosts the PubSub subscription.

During startup, the updater creates a PubSub subscription that listens
to the PubSub topic that publishes SIRI-ET updates.
This parameter specifies in which Google Cloud project the subscription will be created.
The topic and the subscription can be hosted in two different projects.


<h4 id="u__12__fuzzyTripMatching">fuzzyTripMatching</h4>

**Since version:** `2.1` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[12] 

Whether and how the fuzzy trip matcher should be used. A boolean or an object with options.

The fuzzy trip matcher identifies the scheduled trip of a SIRI-ET message by its last stop,
line, service date and the aimed times of the first and last call. Use it for producers whose
messages do not contain usable trip ids.

The parameter accepts a boolean or an object:

- Missing or `false`: no fuzzy trip matching.
- `true`: fuzzy trip matching where the scheduled times must be equal to the aimed times, to
  the second. This is the same as `{ "timeTolerance" : "0s" }`.
- An object: fuzzy trip matching with the given options, for example
  `{ "timeTolerance" : "59s" }`.

If more than one trip matches, the message is rejected.


<h4 id="u__12__fuzzyTripMatching_timeTolerance">timeTolerance</h4>

**Since version:** `2.11` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /updaters/[12]/fuzzyTripMatching 

The maximum difference between the scheduled and the aimed times.

A scheduled trip matches if its scheduled departure time at the first stop and its scheduled
arrival time at the last stop differ from the SIRI aimed times by at most this duration. Use
this if the producer sends aimed times in whole minutes, while the schedule contains seconds.
The tolerance must be in whole seconds and less than 30 minutes. A larger tolerance increases
the chance that more than one trip matches, in which case the message is rejected.




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
