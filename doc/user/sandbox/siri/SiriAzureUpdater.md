# SIRI Azure Updater

This is a sandbox extension developed by Skånetrafiken that allows OTP to fetch SIRI ET & SX messages 
through *Azure Service Bus*.
It also enables OTP to download historical data from en HTTP endpoint on startup.

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

| Config Parameter                                           |    Type    | Summary                                                          |  Req./Opt. | Default Value       | Since |
|------------------------------------------------------------|:----------:|------------------------------------------------------------------|:----------:|---------------------|:-----:|
| type = "siri-azure-et-updater"                             |   `enum`   | The type of the updater.                                         | *Required* |                     |  1.5  |
| [authenticationType](#u__11__authenticationType)           |   `enum`   | Which authentication type to use                                 | *Optional* | `"sharedaccesskey"` |  2.5  |
| autoDeleteOnIdle                                           | `duration` | The time after which an inactive subscription is removed.        | *Optional* | `"PT1H"`            |  2.5  |
| [customMidnight](#u__11__customMidnight)                   |  `integer` | Time on which time breaks into new day.                          | *Optional* | `0`                 |  2.2  |
| feedId                                                     |  `string`  | The ID of the feed to apply the updates to.                      | *Required* |                     |  2.2  |
| [fullyQualifiedNamespace](#u__11__fullyQualifiedNamespace) |  `string`  | Service Bus fully qualified namespace used for authentication.   | *Optional* |                     |  2.5  |
| fuzzyTripMatching                                          |  `boolean` | Whether to apply fuzzyTripMatching on the updates                | *Optional* | `false`             |  2.2  |
| prefetchCount                                              |  `integer` | The number of messages to fetch from the subscription at a time. | *Optional* | `10`                |  2.5  |
| [servicebus-url](#u__11__servicebus_url)                   |  `string`  | Service Bus connection used for authentication.                  | *Optional* |                     |  2.2  |
| [startupTimeout](#u__11__startupTimeout)                   | `duration` | Maximum time to wait for real-time services during startup.      | *Optional* | `"PT5M"`            |   na  |
| topic                                                      |  `string`  | Service Bus topic to connect to.                                 | *Required* |                     |  2.2  |
| history                                                    |  `object`  | Configuration for fetching historical data on startup            | *Optional* |                     |  2.2  |
|    fromDateTime                                            |  `string`  | Datetime boundary for historical data                            | *Optional* | `"-P1D"`            |  2.2  |
|    timeout                                                 |  `integer` | Timeout in milliseconds                                          | *Optional* | `300000`            |   na  |
|    url                                                     |  `string`  | Endpoint to fetch from                                           | *Optional* |                     |   na  |


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

<h4 id="u__11__startupTimeout">startupTimeout</h4>

**Since version:** `na` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT5M"`   
**Path:** /updaters/[11] 

Maximum time to wait for real-time services during startup.

Maximum time to wait for real-time services during startup. If real-time services are unavailable, OTP will start without real-time data after this timeout.



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
| fuzzyTripMatching                                          |  `boolean` | Whether to apply fuzzyTripMatching on the updates                | *Optional* | `false`             |  2.2  |
| prefetchCount                                              |  `integer` | The number of messages to fetch from the subscription at a time. | *Optional* | `10`                |  2.5  |
| [servicebus-url](#u__10__servicebus_url)                   |  `string`  | Service Bus connection used for authentication.                  | *Optional* |                     |  2.2  |
| [startupTimeout](#u__10__startupTimeout)                   | `duration` | Maximum time to wait for real-time services during startup.      | *Optional* | `"PT5M"`            |   na  |
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

<h4 id="u__10__startupTimeout">startupTimeout</h4>

**Since version:** `na` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT5M"`   
**Path:** /updaters/[10] 

Maximum time to wait for real-time services during startup.

Maximum time to wait for real-time services during startup. If real-time services are unavailable, OTP will start without real-time data after this timeout.



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