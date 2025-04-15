These updaters support consuming SIRI-ET and SX messages via HTTPS. They aim to support the [Nordic
and EPIP SIRI profiles](./features-explained/Netex-Siri-Compatibility.md) which 
are subsets of the SIRI specification.

For more documentation about the Norwegian profile and data, go to the [Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile).

To enable one of the SIRI updaters you need to add it to the `updaters` section of the `router-config.json`.

## SIRI-ET Request/Response via HTTPS

This requires there to be a SIRI server that handles SIRI POST requests, stores requestor refs 
and responds only with the newest data.

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
| producerMetrics                |    `boolean`    | If failure, success, and warning metrics should be collected per producer.                             | *Optional* | `false`       |  2.7  |
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

## SIRI-SX Request/Response via HTTPS

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

## SIRI-ET Lite

SIRI Lite is 
not very well specified
[[1]](https://nextcloud.leonard.io/s/2tdYdmYBGtLQMfi/download?path=&files=Proposition-Profil-SIRI-Lite-initial-v1-3%20en.pdf)
[[2]](https://normes.transport.data.gouv.fr/normes/siri/profil-france/#protocoles-d%C3%A9change-des-donn%C3%A9es-siri)
but this updater supports the following definition: 

> Fetching XML-formatted SIRI messages as a single GET request rather than the more common request/response 
> flow. 
 
This means that the XML feed must contain all updates for all trips, just like it is the case 
 for GTFS-RT TripUpdates.

<!-- siri-et-lite BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter           |       Type      | Summary                                                                    |  Req./Opt. | Default Value | Since |
|----------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-et-lite"      |      `enum`     | The type of the updater.                                                   | *Required* |               |  1.5  |
| feedId                     |     `string`    | The ID of the feed to apply the updates to.                                | *Required* |               |  2.7  |
| frequency                  |    `duration`   | How often the updates should be retrieved.                                 | *Optional* | `"PT1M"`      |  2.7  |
| fuzzyTripMatching          |    `boolean`    | If the fuzzy trip matcher should be used to match trips.                   | *Optional* | `false`       |  2.7  |
| timeout                    |    `duration`   | The HTTP timeout to download the updates.                                  | *Optional* | `"PT15S"`     |  2.7  |
| [url](#u__15__url)         |      `uri`      | The URL to send the HTTP requests to.                                      | *Required* |               |  2.7  |
| [headers](#u__15__headers) | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |               |  2.7  |


##### Parameter details

<h4 id="u__15__url">url</h4>

**Since version:** `2.7` ∙ **Type:** `uri` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[15] 

The URL to send the HTTP requests to.

Use the file protocol to set a directory for reading updates from a directory. The file
loader will look for xml files: '*.xml' in the configured directory. The files are
renamed by the loader when processed:

&nbsp;&nbsp;&nbsp; _a.xml_ &nbsp; ➞ &nbsp; _a.xml.inProgress_ &nbsp; ➞ &nbsp; _a.xml.ok_ &nbsp; or &nbsp; _a.xml.failed_



<h4 id="u__15__headers">headers</h4>

**Since version:** `2.7` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[15] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-et-lite",
      "feedId" : "sta",
      "url" : "https://example.com/siri-lite/estimated-timetable/xml",
      "fuzzyTripMatching" : true
    }
  ]
}
```

<!-- siri-et-lite END -->

## SIRI-SX Lite

This updater follows the same definition of SIRI Lite as the SIRI-ET one: it downloads the entire
feed in a single HTTP GET request.

<!-- siri-sx-lite BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                 |       Type      | Summary                                                                    |  Req./Opt. | Default Value | Since |
|----------------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "siri-sx-lite"            |      `enum`     | The type of the updater.                                                   | *Required* |               |  1.5  |
| [earlyStart](#u__16__earlyStart) |    `duration`   | This value is subtracted from the actual validity defined in the message.  | *Optional* | `"PT0S"`      |  2.0  |
| feedId                           |     `string`    | The ID of the feed to apply the updates to.                                | *Required* |               |  2.7  |
| frequency                        |    `duration`   | How often the updates should be retrieved.                                 | *Optional* | `"PT1M"`      |  2.7  |
| timeout                          |    `duration`   | The HTTP timeout to download the updates.                                  | *Optional* | `"PT15S"`     |  2.7  |
| [url](#u__16__url)               |      `uri`      | The URL to send the HTTP requests to.                                      | *Required* |               |  2.7  |
| [headers](#u__16__headers)       | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |               |  2.7  |


##### Parameter details

<h4 id="u__16__earlyStart">earlyStart</h4>

**Since version:** `2.0` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT0S"`   
**Path:** /updaters/[16] 

This value is subtracted from the actual validity defined in the message.

Normally the planned departure time is used, so setting this to 10s will cause the
SX-message to be included in trip-results 10 seconds before the the planned departure
time.

<h4 id="u__16__url">url</h4>

**Since version:** `2.7` ∙ **Type:** `uri` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[16] 

The URL to send the HTTP requests to.

Use the file protocol to set a directory for reading updates from a directory. The file
loader will look for xml files: '*.xml' in the configured directory. The files are
renamed by the loader when processed:

&nbsp;&nbsp;&nbsp; _a.xml_ &nbsp; ➞ &nbsp; _a.xml.inProgress_ &nbsp; ➞ &nbsp; _a.xml.ok_ &nbsp; or &nbsp; _a.xml.failed_



<h4 id="u__16__headers">headers</h4>

**Since version:** `2.7` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[16] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "siri-sx-lite",
      "feedId" : "sta",
      "url" : "https://example.com/siri-lite/situation-exchange/xml"
    }
  ]
}
```

<!-- siri-sx-lite END -->

