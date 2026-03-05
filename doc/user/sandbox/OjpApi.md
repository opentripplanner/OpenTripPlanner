# OpenJourneyPlanner (OJP) API

## Contact Info

- Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)

## Documentation

This sandbox feature implements part of the [OpenJourneyPlanner API](https://opentransportdata.swiss/en/cookbook/open-journey-planner-ojp/) 
which is a standard defined by CEN, the European Committee for Standardization.

The following request types are supported:

- `StopEventRequest`
- `TripRequest`

To enable this turn on `OjpApi` as a feature in `otp-config.json`.

## Configuration

This feature allows a small number of config options. To change the configuration, add the 
following to `router-config.json`.

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// router-config.json
{
  "ojpApi" : {
    "hideFeedId" : true,
    "hardcodedInputFeedId" : "ch"
  }
}
```
### Overview

| Config Parameter                                     |    Type   | Summary                                                      |  Req./Opt. | Default Value | Since |
|------------------------------------------------------|:---------:|--------------------------------------------------------------|:----------:|---------------|:-----:|
| [hardcodedInputFeedId](#ojpApi_hardcodedInputFeedId) |  `string` | The hardcoded feedId to add to all input ids.                | *Optional* |               |  2.9  |
| [hideFeedId](#ojpApi_hideFeedId)                     | `boolean` | Hide the feed id in all API output, and add it to input ids. | *Optional* | `false`       |  2.9  |


### Details

<h4 id="ojpApi_hardcodedInputFeedId">hardcodedInputFeedId</h4>

**Since version:** `2.9` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /ojpApi 

The hardcoded feedId to add to all input ids.

Only turn this feature on if you have unique ids across all feeds, without the feedId prefix _and_ `hideFeedId` is set to `true`.`

<h4 id="ojpApi_hideFeedId">hideFeedId</h4>

**Since version:** `2.9` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /ojpApi 

Hide the feed id in all API output, and add it to input ids.

Only turn this feature on if you have unique ids across all feeds, without the feedId prefix.




<!-- config END -->
