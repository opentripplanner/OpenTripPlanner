# SIRI Azure Updater

This is a sandbox extension developed by Skånetrafiken that allows OTP to fetch SIRI ET & SX
messages through _Azure Service Bus_. It also enables OTP to download historical data from en HTTP
endpoint on startup.

## Contact Info

Skånetrafiken, Sweden  
developer.otp@skanetrafiken.se

## Documentation

Documentation available [here](../../examples/skanetrafiken/Readme.md).

## Configuration

To enable the SIRI updater you need to add it to the updaters section of the `router-config.json`.

### SIRI Azure ET Updater

<!-- siri-azure-et-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                            |    Type    | Summary                                                                                     |  Req./Opt. | Default Value       | Since |
|-------------------------------------------------------------|:----------:|---------------------------------------------------------------------------------------------|:----------:|---------------------|:-----:|
| type = "siri-azure-et-updater"                              |   `enum`   | The type of the updater.                                                                    | *Required* |                     |  1.5  |
| [authenticationType](#u__11__authenticationType)            |   `enum`   | Which authentication type to use                                                            | *Optional* | `"sharedaccesskey"` |  2.5  |
| autoDeleteOnIdle                                            | `duration` | The time after which an inactive subscription is removed.                                   | *Optional* | `"PT1H"`            |  2.5  |
| [customMidnight](#u__11__customMidnight)                    |  `integer` | Time on which time breaks into new day.                                                     | *Optional* | `0`                 |  2.2  |
| feedId                                                      |  `string`  | The ID of the feed to apply the updates to.                                                 | *Required* |                     |  2.2  |
| [fullyQualifiedNamespace](#u__11__fullyQualifiedNamespace)  |  `string`  | Service Bus fully qualified namespace used for authentication.                              | *Optional* |                     |  2.5  |
| prefetchCount                                               |  `integer` | The number of messages to fetch from the subscription at a time.                            | *Optional* | `10`                |  2.5  |
| [servicebus-url](#u__11__servicebus_url)                    |  `string`  | Service Bus connection used for authentication.                                             | *Optional* |                     |  2.2  |
| topic                                                       |  `string`  | Service Bus topic to connect to.                                                            | *Required* |                     |  2.2  |
| [fuzzyTripMatching](#u__11__fuzzyTripMatching)              |  `object`  | Whether and how the fuzzy trip matcher should be used. A boolean or an object with options. | *Optional* |                     |  2.2  |
|    [timeTolerance](#u__11__fuzzyTripMatching_timeTolerance) | `duration` | The maximum difference between the scheduled and the aimed times.                           | *Optional* | `"PT0S"`            |  2.11 |
| history                                                     |  `object`  | Configuration for fetching historical data on startup                                       | *Optional* |                     |  2.2  |
|    fromDateTime                                             |  `string`  | Datetime boundary for historical data                                                       | *Optional* | `"-P1D"`            |  2.2  |
|    timeout                                                  |  `integer` | Timeout in milliseconds                                                                     | *Optional* | `300000`            |   na  |
|    url                                                      |  `string`  | Endpoint to fetch from                                                                      | *Optional* |                     |   na  |


##### Parameter details

<h4 id="u__11__authenticationType">authenticationType</h4>

**Since version:** `2.5` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"sharedaccesskey"`   
**Path:** /updaters/[11]   
**Enum values:** `sharedaccesskey` | `federatedidentity`

Which authentication type to use

<h4 id="u__11__customMidnight">customMidnight</h4>

**Since version:** `2.2` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /updaters/[11] 

Time on which time breaks into new day.

It is common that operating day date breaks a little bit later than midnight so that the switch happens when traffic is at the lowest point. Parameter uses 24-hour format. If the switch happens on 4 am then set this field to 4.

<h4 id="u__11__fullyQualifiedNamespace">fullyQualifiedNamespace</h4>

**Since version:** `2.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[11] 

Service Bus fully qualified namespace used for authentication.

Has to be present for authenticationMethod FederatedIdentity.

<h4 id="u__11__servicebus_url">servicebus-url</h4>

**Since version:** `2.2` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[11] 

Service Bus connection used for authentication.

Has to be present for authenticationMethod SharedAccessKey. This should be Primary/Secondary connection string from service bus.

<h4 id="u__11__fuzzyTripMatching">fuzzyTripMatching</h4>

**Since version:** `2.2` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[11] 

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


<h4 id="u__11__fuzzyTripMatching_timeTolerance">timeTolerance</h4>

**Since version:** `2.11` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /updaters/[11]/fuzzyTripMatching 

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
      "type" : "siri-azure-et-updater",
      "topic" : "some_topic",
      "authenticationType" : "SharedAccessKey",
      "fullyQualifiedNamespace" : "fully_qualified_namespace",
      "servicebus-url" : "service_bus_url",
      "feedId" : "feed_id",
      "fuzzyTripMatching" : {
        "timeTolerance" : "59s"
      },
      "customMidnight" : 4,
      "history" : {
        "url" : "endpoint_url",
        "fromDateTime" : "-P1D",
        "timeout" : 300000
      }
    }
  ]
}
```

<!-- siri-azure-et-updater END -->

### SIRI Azure SX Updater

<!-- siri-azure-sx-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                           |    Type    | Summary                                                          |  Req./Opt. | Default Value       | Since |
|------------------------------------------------------------|:----------:|------------------------------------------------------------------|:----------:|---------------------|:-----:|
| type = "siri-azure-sx-updater"                             |   `enum`   | The type of the updater.                                         | *Required* |                     |  1.5  |
| [authenticationType](#u__10__authenticationType)           |   `enum`   | Which authentication type to use                                 | *Optional* | `"sharedaccesskey"` |  2.5  |
| autoDeleteOnIdle                                           | `duration` | The time after which an inactive subscription is removed.        | *Optional* | `"PT1H"`            |  2.5  |
| [customMidnight](#u__10__customMidnight)                   |  `integer` | Time on which time breaks into new day.                          | *Optional* | `0`                 |  2.2  |
| feedId                                                     |  `string`  | The ID of the feed to apply the updates to.                      | *Required* |                     |  2.2  |
| [fullyQualifiedNamespace](#u__10__fullyQualifiedNamespace) |  `string`  | Service Bus fully qualified namespace used for authentication.   | *Optional* |                     |  2.5  |
| prefetchCount                                              |  `integer` | The number of messages to fetch from the subscription at a time. | *Optional* | `10`                |  2.5  |
| [servicebus-url](#u__10__servicebus_url)                   |  `string`  | Service Bus connection used for authentication.                  | *Optional* |                     |  2.2  |
| topic                                                      |  `string`  | Service Bus topic to connect to.                                 | *Required* |                     |  2.2  |
| history                                                    |  `object`  | Configuration for fetching historical data on startup            | *Optional* |                     |  2.2  |
|    fromDateTime                                            |  `string`  | Datetime boundary for historical data.                           | *Optional* | `"-P1D"`            |  2.2  |
|    timeout                                                 |  `integer` | Timeout in milliseconds                                          | *Optional* | `300000`            |   na  |
|    toDateTime                                              |  `string`  | Datetime boundary for historical data.                           | *Optional* | `"P1D"`             |  2.2  |
|    url                                                     |  `string`  | Endpoint to fetch from                                           | *Optional* |                     |   na  |


##### Parameter details

<h4 id="u__10__authenticationType">authenticationType</h4>

**Since version:** `2.5` ∙ **Type:** `enum` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"sharedaccesskey"`   
**Path:** /updaters/[10]   
**Enum values:** `sharedaccesskey` | `federatedidentity`

Which authentication type to use

<h4 id="u__10__customMidnight">customMidnight</h4>

**Since version:** `2.2` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0`   
**Path:** /updaters/[10] 

Time on which time breaks into new day.

It is common that operating day date breaks a little bit later than midnight so that the switch happens when traffic is at the lowest point. Parameter uses 24-hour format. If the switch happens on 4 am then set this field to 4.

<h4 id="u__10__fullyQualifiedNamespace">fullyQualifiedNamespace</h4>

**Since version:** `2.5` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[10] 

Service Bus fully qualified namespace used for authentication.

Has to be present for authenticationMethod FederatedIdentity.

<h4 id="u__10__servicebus_url">servicebus-url</h4>

**Since version:** `2.2` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[10] 

Service Bus connection used for authentication.

Has to be present for authenticationMethod SharedAccessKey. This should be Primary/Secondary connection string from service bus.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-azure-sx-updater",
      "topic" : "some_topic",
      "servicebus-url" : "service_bus_url",
      "feedId" : "feed_id",
      "customMidnight" : 4,
      "history" : {
        "url" : "endpoint_url",
        "fromDateTime" : "-P1D",
        "toDateTime" : "P1D",
        "timeout" : 300000
      }
    }
  ]
}
```

<!-- siri-azure-sx-updater END -->

## Changelog

- Added configuration for turning off stop arrival time match feature.
- Initial version (April 2022)
- Minor changes in logging (November 2022)
- Retry fetch from history endpoint if it failed (February 2023)
- Solve a bug in SiriAzureETUpdater and improve error logging (March 2023)
- Add support with federated identity authentication (February 2024)
