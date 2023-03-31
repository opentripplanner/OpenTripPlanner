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
| type = "SIRI_ET_UPDATER"       |      `enum`     | The type of the updater.                                                                               | *Required* |               |  1.5  |
| blockReadinessUntilInitialized |    `boolean`    | Whether catching up with the updates should block the readiness check from returning a 'ready' result. | *Optional* | `false`       |  2.0  |
| feedId                         |     `string`    | The ID of the feed to apply the updates to.                                                            | *Required* |               |  2.0  |
| frequencySec                   |    `integer`    | How often the updates should be retrieved.                                                             | *Optional* | `60`          |  2.0  |
| fuzzyTripMatching              |    `boolean`    | If the fuzzy trip matcher should be used to match trips.                                               | *Optional* | `false`       |  2.0  |
| previewIntervalMinutes         |    `integer`    | TODO                                                                                                   | *Optional* | `-1`          |  2.0  |
| requestorRef                   |     `string`    | The requester reference.                                                                               | *Optional* |               |  2.0  |
| timeoutSec                     |    `integer`    | The HTTP timeout to download the updates.                                                              | *Optional* | `15`          |  2.0  |
| url                            |     `string`    | The URL to send the HTTP requests to.                                                                  | *Required* |               |  2.0  |
| [headers](#u__8__headers)      | `map of string` | HTTP headers to add to the request                                                                     | *Optional* |               |  2.3  |


##### Parameter details

<h4 id="u__8__headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[8] 

HTTP headers to add to the request



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-et-updater",
      "url" : "https://example.com/some/path",
      "feedId" : "feed_id",
      "timeoutSec" : 30,
      "headers" : {
        "Authorization" : "Some-Token"
      }
    }
  ]
}
```

<!-- siri-et-updater END -->

## Changelog

- Initial version of SIRI updater (October 2019)
- Include situations with no or no handled entity selectors with Unknown EntitySelector (December
  2021, https://github.com/opentripplanner/OpenTripPlanner/pull/3780)
