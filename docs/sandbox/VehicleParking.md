# Vehicle Parking Updaters

## Contact Info

- For HSL Park and Ride updater: Digitransit team, HSL, Helsinki, Finland
- For Bikely updater: Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)


## Documentation

This sandbox contains vehicle parking updaters. Unlike for some other sandbox features, this is not
enabled/disabled through `otp-config.json` but from `router-config.json` updaters.

Currently contains the following updaters:

- [HSL Park and Ride](https://p.hsl.fi/docs/index.html)
- [ParkAPI](https://github.com/offenesdresden/ParkAPI)
- [Bikely](https://www.safebikely.com/)

### Configuration

These sandboxed vehicle parking updaters can be enabled by editing the `updaters` section in
the `router-config.json` according to the following examples.

All updaters have the following parameters in common:

- `type`: this needs to be `"vehicle-parking"`
- `feedId`: this is used as a "prefix" for park ids, entrance ids and sometimes also for tags.

## HSL Park and Ride

<!-- hsl-park BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                |     Type    | Summary                                      |  Req./Opt. | Default Value | Since |
|---------------------------------|:-----------:|----------------------------------------------|:----------:|---------------|:-----:|
| type = "vehicle-parking"        |    `enum`   | The type of the updater.                     | *Required* |               |  1.5  |
| facilitiesFrequencySec          |  `integer`  | How often the facilities should be updated.  | *Optional* | `3600`        |  2.2  |
| facilitiesUrl                   |   `string`  | URL of the facilities.                       | *Optional* |               |  2.2  |
| [feedId](#u__2__feedId)         |   `string`  | The name of the data source.                 | *Required* |               |  2.2  |
| hubsUrl                         |   `string`  | Hubs URL                                     | *Optional* |               |  2.2  |
| [sourceType](#u__2__sourceType) |    `enum`   | The source of the vehicle updates.           | *Required* |               |  2.2  |
| [timeZone](#u__2__timeZone)     | `time-zone` | The time zone of the feed.                   | *Optional* |               |  2.2  |
| utilizationsFrequencySec        |  `integer`  | How often the utilization should be updated. | *Optional* | `600`         |  2.2  |
| utilizationsUrl                 |   `string`  | URL of the utilization data.                 | *Optional* |               |  2.2  |


#### Details

<h4 id="u__2__feedId">feedId</h4>

**Since version:** `2.2` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[2] 

The name of the data source.

This will end up in the API responses as the feed id of of the parking lot.

<h4 id="u__2__sourceType">sourceType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[2]   
**Enum values:** `park-api` | `bicycle-park-api` | `hsl-park` | `bikely`

The source of the vehicle updates.

<h4 id="u__2__timeZone">timeZone</h4>

**Since version:** `2.2` ∙ **Type:** `time-zone` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[2] 

The time zone of the feed.

Used for converting abstract opening hours into concrete points in time.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "vehicle-parking",
      "sourceType" : "hsl-park",
      "feedId" : "hslpark",
      "timeZone" : "Europe/Helsinki",
      "facilitiesFrequencySec" : 3600,
      "facilitiesUrl" : "https://p.hsl.fi/api/v1/facilities.json?limit=-1",
      "utilizationsFrequencySec" : 600,
      "utilizationsUrl" : "https://p.hsl.fi/api/v1/utilizations.json?limit=-1",
      "hubsUrl" : "https://p.hsl.fi/api/v1/hubs.json?limit=-1"
    }
  ]
}
```

<!-- hsl-park END -->

## ParkAPI 

<!-- park-api BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                |       Type      | Summary                                                                    |  Req./Opt. | Default Value | Since |
|---------------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "vehicle-parking"        |      `enum`     | The type of the updater.                                                   | *Required* |               |  1.5  |
| [feedId](#u__3__feedId)         |     `string`    | The name of the data source.                                               | *Required* |               |  2.2  |
| frequency                       |    `duration`   | How often to update the source.                                            | *Optional* | `"PT1M"`      |  2.2  |
| [sourceType](#u__3__sourceType) |      `enum`     | The source of the vehicle updates.                                         | *Required* |               |  2.2  |
| [timeZone](#u__3__timeZone)     |   `time-zone`   | The time zone of the feed.                                                 | *Optional* |               |  2.2  |
| url                             |     `string`    | URL of the resource.                                                       | *Optional* |               |  2.2  |
| [headers](#u__3__headers)       | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |               |  2.2  |
| [tags](#u__3__tags)             |    `string[]`   | Tags to add to the parking lots.                                           | *Optional* |               |  2.2  |


#### Details

<h4 id="u__3__feedId">feedId</h4>

**Since version:** `2.2` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[3] 

The name of the data source.

This will end up in the API responses as the feed id of of the parking lot.

<h4 id="u__3__sourceType">sourceType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[3]   
**Enum values:** `park-api` | `bicycle-park-api` | `hsl-park` | `bikely`

The source of the vehicle updates.

<h4 id="u__3__timeZone">timeZone</h4>

**Since version:** `2.2` ∙ **Type:** `time-zone` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[3] 

The time zone of the feed.

Used for converting abstract opening hours into concrete points in time.

<h4 id="u__3__headers">headers</h4>

**Since version:** `2.2` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[3] 

HTTP headers to add to the request. Any header key, value can be inserted.

<h4 id="u__3__tags">tags</h4>

**Since version:** `2.2` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[3] 

Tags to add to the parking lots.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "vehicle-parking",
      "sourceType" : "park-api",
      "feedId" : "parkapi",
      "timeZone" : "Europe/Berlin",
      "frequency" : "10m",
      "url" : "https://foo.bar",
      "headers" : {
        "Cache-Control" : "max-age=604800"
      },
      "tags" : [
        "source:parkapi"
      ]
    }
  ]
}
```

<!-- park-api END -->

## Bikely

<!-- bikely BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                |       Type      | Summary                                                                    |  Req./Opt. | Default Value | Since |
|---------------------------------|:---------------:|----------------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "vehicle-parking"        |      `enum`     | The type of the updater.                                                   | *Required* |               |  1.5  |
| [feedId](#u__4__feedId)         |     `string`    | The name of the data source.                                               | *Required* |               |  2.2  |
| frequency                       |    `duration`   | How often to update the source.                                            | *Optional* | `"PT1M"`      |  2.3  |
| [sourceType](#u__4__sourceType) |      `enum`     | The source of the vehicle updates.                                         | *Required* |               |  2.2  |
| url                             |      `uri`      | URL of the locations endpoint.                                             | *Optional* |               |  2.3  |
| [headers](#u__4__headers)       | `map of string` | HTTP headers to add to the request. Any header key, value can be inserted. | *Optional* |               |  2.3  |


#### Details

<h4 id="u__4__feedId">feedId</h4>

**Since version:** `2.2` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[4] 

The name of the data source.

This will end up in the API responses as the feed id of of the parking lot.

<h4 id="u__4__sourceType">sourceType</h4>

**Since version:** `2.2` ∙ **Type:** `enum` ∙ **Cardinality:** `Required`   
**Path:** /updaters/[4]   
**Enum values:** `park-api` | `bicycle-park-api` | `hsl-park` | `bikely`

The source of the vehicle updates.

<h4 id="u__4__headers">headers</h4>

**Since version:** `2.3` ∙ **Type:** `map of string` ∙ **Cardinality:** `Optional`   
**Path:** /updaters/[4] 

HTTP headers to add to the request. Any header key, value can be inserted.



##### Example configuration

```JSON
// router-config.json
{
  "updaters" : [
    {
      "type" : "vehicle-parking",
      "feedId" : "bikely",
      "sourceType" : "bikely",
      "url" : "https://api.safebikely.com/api/v1/s/locations",
      "headers" : {
        "X-Bikely-Token" : "${BIKELY_TOKEN}",
        "Authorization" : "${BIKELY_AUTHORIZATION}"
      }
    }
  ]
}
```

<!-- bikely END -->


## Changelog

- Create initial sandbox implementation (January 2022, [#3796](https://github.com/opentripplanner/OpenTripPlanner/pull/3796))
- Add timeZone parameter to hsl and parkapi updaters (September 2022, [#4427](https://github.com/opentripplanner/OpenTripPlanner/pull/4427))
- Added support for HSL parking hubs (October 2022, [#4510](https://github.com/opentripplanner/OpenTripPlanner/pull/4510))
- Add Bikely updater (November 2022, [#4589](https://github.com/opentripplanner/OpenTripPlanner/pull/4589))
