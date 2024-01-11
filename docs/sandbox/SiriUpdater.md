# Siri Updater

Support for consuming SIRI ET and SX messages. The updater is developed to support the Nordic
SIRI profile which is a subset of the SIRI specification.

## Contact Info

- Lasse Tyrihjell, Entur, Norway

## Documentation

This updater consumes SIRI real time information. It is developed by Entur and supports the Nordic
Profile for SIRI. It should be possible to develop it further to support a broader set of the SIRI
specification.

For more documentation goto
the [Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI updater you need to add it to the updaters section of the `router-config.json`.

### Siri-ET via HTTPS

<!-- siri-et-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter               |       Type      | Summary                                                                                                |  Req./Opt. | Default Value | Since |
|--------------------------------|:---------------:|--------------------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-et-updater"       |      `enum`     | The type of the updater.                                                                               | *Required* |               |  1.5  |
| blockReadinessUntilInitialized |    `boolean`    | Whether catching up with the updates should block the readiness check from returning a 'ready' result. | *Optional* | `false`       |  2.0  |
| feedId                         |     `string`    | The ID of the feed to apply the updates to.                                                            | *Required* |               |  2.0  |
| frequency                      |    `duration`   | How often the updates should be retrieved.                                                             | *Optional* | `"PT1M"`      |  2.0  |
| fuzzyTripMatching              |    `boolean`    | If the fuzzy trip matcher should be used to match trips.                                               | *Optional* | `false`       |  2.0  |
| previewInterval                |    `duration`   | TODO                                                                                                   | *Optional* |               |  2.0  |
| requestorRef                   |     `string`    | The requester reference.                                                                               | *Optional* |               |  2.0  |
| timeout                        |    `duration`   | The HTTP timeout to download the updates.                                                              | *Optional* | `"PT15S"`     |  2.0  |
| [url](#u__8__url)              |     `string`    | The URL to send the HTTP requests to.                                                                  | *Required* |               |  2.0  |
| [headers](#u__8__headers)      | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.                             | *Optional* |               |  2.3  |


##### Parameter details

<h4 id="u__8__url">url</h4>

**Since version:** `2.0` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[8] 

The URL to send the HTTP requests to.

Use the file protocol to set a directory for reading updates from a directory. The file
loader will look for xml files: '*.xml' in the configured directory. The files are
renamed by the loader when processed:

&nbsp;&nbsp;&nbsp; _a.xml_ &nbsp; ➞ &nbsp; _a.xml.inProgress_ &nbsp; ➞ &nbsp; _a.xml.ok_ &nbsp; or &nbsp; _a.xml.failed_



<h4 id="u__8__headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[8] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-et-updater",
      "url" : "https://example.com/some/path",
      "feedId" : "feed_id",
      "timeout" : "30s",
      "headers" : {
        "Authorization" : "Some-Token"
      }
    }
  ]
}
```

<!-- siri-et-updater END -->

### Siri-SX via HTTPS

<!-- siri-sx-updater BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                |       Type      | Summary                                                                                                |  Req./Opt. | Default Value | Since |
|---------------------------------|:---------------:|--------------------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-sx-updater"        |      `enum`     | The type of the updater.                                                                               | *Required* |               |  1.5  |
| blockReadinessUntilInitialized  |    `boolean`    | Whether catching up with the updates should block the readiness check from returning a 'ready' result. | *Optional* | `false`       |  2.0  |
| [earlyStart](#u__9__earlyStart) |    `duration`   | This value is subtracted from the actual validity defined in the message.                              | *Optional* | `"PT0S"`      |  2.0  |
| feedId                          |     `string`    | The ID of the feed to apply the updates to.                                                            | *Required* |               |  2.0  |
| frequency                       |    `duration`   | How often the updates should be retrieved.                                                             | *Optional* | `"PT1M"`      |  2.0  |
| requestorRef                    |     `string`    | The requester reference.                                                                               | *Optional* |               |  2.0  |
| timeout                         |    `duration`   | The HTTP timeout to download the updates.                                                              | *Optional* | `"PT15S"`     |  2.0  |
| [url](#u__9__url)               |     `string`    | The URL to send the HTTP requests to. Supports http/https and file protocol.                           | *Required* |               |  2.0  |
| [headers](#u__9__headers)       | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted.                             | *Optional* |               |  2.3  |


##### Parameter details

<h4 id="u__9__earlyStart">earlyStart</h4>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /updaters/[9] 

This value is subtracted from the actual validity defined in the message.

Normally the planned departure time is used, so setting this to 10s will cause the
SX-message to be included in trip-results 10 seconds before the the planned departure
time.

<h4 id="u__9__url">url</h4>

**Since version:** `2.0` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[9] 

The URL to send the HTTP requests to. Supports http/https and file protocol.


Use the file protocol to set a directory for reading updates from a directory. The file
loader will look for xml files: '*.xml' in the configured directory. The files are
renamed by the loader when processed:

&nbsp;&nbsp;&nbsp; _a.xml_ &nbsp; ➞ &nbsp; _a.xml.inProgress_ &nbsp; ➞ &nbsp; _a.xml.ok_ &nbsp; or &nbsp; _a.xml.failed_



<h4 id="u__9__headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[9] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-sx-updater",
      "url" : "https://example.com/some/path",
      "feedId" : "feed_id",
      "timeout" : "30s",
      "headers" : {
        "Key" : "Value"
      }
    }
  ]
}
```

<!-- siri-sx-updater END -->

## Changelog

- Initial version of SIRI updater (October 2019)
- Include situations with no or no handled entity selectors with Unknown EntitySelector (December
  2021, https://github.com/opentripplanner/OpenTripPlanner/pull/3780)
