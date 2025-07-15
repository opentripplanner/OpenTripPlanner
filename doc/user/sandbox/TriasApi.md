# TRIAS API

## Contact Info

- Leonard Ehrenfried, mail@leonard.io

## Documentation

This sandbox feature implements part of the [TRIAS API](https://www.vdv.de/projekt-ip-kom-oev-ekap.aspx) 
which is a standard defined by the German VDV, the association of transit agencies.

The following request types are supported:

- `StopEventRequest`

To enable this turn on `TriasApi` as a feature in `otp-config.json`.

### URLs

- Endpoint: `http://localhost:8080/otp/trias/v1/`
- Visual API Explorer: `http://localhost:8080/otp/trias/v1/explorer`

## Configuration

This feature allows a small number of config options. To change the configuration, add the 
following to `router-config.json`.

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// router-config.json
{
  "triasApi" : {
    "hideFeedId" : true,
    "hardcodedInputFeedId" : "nvbw"
  }
}
```
### Overview

| Config Parameter                                       |     Type    | Summary                                                          |  Req./Opt. | Default Value | Since |
|--------------------------------------------------------|:-----------:|------------------------------------------------------------------|:----------:|---------------|:-----:|
| [hardcodedInputFeedId](#triasApi_hardcodedInputFeedId) |   `string`  | The hardcoded feedId to add to all input ids.                    | *Optional* |               |  2.8  |
| [hideFeedId](#triasApi_hideFeedId)                     |  `boolean`  | Hide the feed id in all API output, and add it to input ids.     | *Optional* | `false`       |  2.8  |
| [timeZone](#triasApi_timeZone)                         | `time-zone` | If you don't want to use the feed's timezone, configure it here. | *Optional* |               |  2.8  |


### Details

<h4 id="triasApi_hardcodedInputFeedId">hardcodedInputFeedId</h4>

**Since version:** `2.8` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /triasApi 

The hardcoded feedId to add to all input ids.

Only turn this feature on if you have unique ids across all feeds, without the feedId prefix _and_ `hideFeedId` is set to `true`.`

<h4 id="triasApi_hideFeedId">hideFeedId</h4>

**Since version:** `2.8` ∙ **Type:** `boolean` ∙ **Cardinality:** `Optional` ∙ **Default value:** `false`   
**Path:** /triasApi 

Hide the feed id in all API output, and add it to input ids.

Only turn this feature on if you have unique ids across all feeds, without the feedId prefix.

<h4 id="triasApi_timeZone">timeZone</h4>

**Since version:** `2.8` ∙ **Type:** `time-zone` ∙ **Cardinality:** `Optional`   
**Path:** /triasApi 

If you don't want to use the feed's timezone, configure it here.

By default the input feed's timezone is used. However, there may be cases when you want the
API to use a different timezone.

**Think hard about changing the timezone! We recommend that you keep the feed's time zone and
convert the time in the client which will make debugging OTP much easier.**





<!-- config END -->
