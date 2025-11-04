# SIRI-ET MQTT Updater

Support for consuming SIRI-ET messages over an MQTT subscription. Similarly to the SIRI-ET HTTP 
updater, this updater is developed to support the Nordic SIRI profile which is a subset of the SIRI 
specification.

## Contact Info
HBT, Hamburg, Germany
jessica.koehnke@hbt.de

## Documentation

This updater consumes SIRI real time information over an asynchronous MQTT feed
by subscribing to an MQTT topic.

For more documentation see
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI-ET MQTT updater you need to add it to the updaters section
of the `router-config.json`.

### SIRI-ET via MQTT

<!-- siri-et-mqtt BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                         |    Type    | Summary                                                |  Req./Opt. | Default Value | Since |
|----------------------------------------------------------|:----------:|--------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-et-mqtt"                                    |   `enum`   | The type of the updater.                               | *Required* |               |  1.5  |
| feedId                                                   |  `string`  | The feed ID this updater should be applied to          | *Required* |               |  2.9  |
| fuzzyTripMatching                                        |  `boolean` | Whether or not the fuzzy trip matcher should be used   | *Required* |               |  2.9  |
| host                                                     |  `string`  | The host of the MQTT broker                            | *Required* |               |  2.9  |
| [maxPrimingIdleTime](#u__17__maxPrimingIdleTime)         | `duration` | Max idle time until priming is considered complete.    | *Optional* | `"PT1S"`      |  2.9  |
| [numberOfPrimingWorkers](#u__17__numberOfPrimingWorkers) |  `integer` | Number of priming workers to process retained messages | *Optional* | `1`           |  2.9  |
| [password](#u__17__password)                             |  `string`  | The password for authorization at the MQTT broker      | *Optional* |               |  2.9  |
| port                                                     |  `integer` | The port of the MQTT broker                            | *Required* |               |  2.9  |
| qos                                                      |  `integer` | The qos used for the MQTT subscription                 | *Required* |               |  2.9  |
| topic                                                    |  `string`  | The topic the updater should subscribe to              | *Required* |               |  2.9  |
| [user](#u__17__user)                                     |  `string`  | The user for authorization at the MQTT broker          | *Optional* |               |  2.9  |


##### Parameter details

<h4 id="u__17__maxPrimingIdleTime">maxPrimingIdleTime</h4>

**Since version:** `2.9` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT1S"`   
**Path:** /updaters/[17] 

Max idle time until priming is considered complete.

The worker(s) processing the retained messages for priming shut down when they are idle for
the specified amount of time. When all priming workers are shut down, the updater is
considered to be primed.


<h4 id="u__17__numberOfPrimingWorkers">numberOfPrimingWorkers</h4>

**Since version:** `2.9` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1`   
**Path:** /updaters/[17] 

Number of priming workers to process retained messages

The updater will only be primed and therefore ready for routing requests, when all
retained messages are processed. If the number of retained messages is large, this could
take a considerable amount of time. Profiling shows that the bottleneck is in the XML
parsing. Increasing the number of priming workers will parallelize parsing. Tests show
that 4 priming workers seem to be the optimal amount, increasing the number of priming
workers further will decrease performance. The reception of the messages and the updating
of the graph always occurs in a single thread.


<h4 id="u__17__password">password</h4>

**Since version:** `2.9` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[17] 

The password for authorization at the MQTT broker

If no authorization is required, the password does not need to be supplied.

<h4 id="u__17__user">user</h4>

**Since version:** `2.9` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[17] 

The user for authorization at the MQTT broker

If no authorization is required, the user does not need to be supplied.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-et-mqtt",
      "user" : "user",
      "password" : "pwd",
      "host" : "localhost",
      "port" : 1883,
      "feedId" : "1",
      "topic" : "trip/updates/#",
      "qos" : 1,
      "fuzzyTripMatching" : true,
      "numberOfPrimingWorkers" : 4,
      "maxPrimingIdleTime" : "1s"
    }
  ]
}
```

<!-- siri-et-mqtt END -->

