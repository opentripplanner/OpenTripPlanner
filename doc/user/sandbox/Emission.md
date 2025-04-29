# CO₂ Emissions calculation

## Contact Info

- Digitransit Team
- Entur AS

## Documentation

The emission module enable import of CO₂ Emissions data witch OTP can use to decorate itineraries
and itinerary legs. Emissions data is loaded during the graph build process. OTP support two data
import formats (yet to be standarized).

- The _Route format_ provided by HSL decorate each route with a avegare co2 emission per kilometers
  per person. The input parameters are `route_id`, `avg_co2_per_vehicle_per_km` and
  `avg_passenger_count`.
- The _Trip format_ provided by Entur decorate each trip hop. The plan is to expand this to include
  service date. This allow the emission provider to calculate acurate data down to each trip
  depature (except depatures operated by more than one vhiechle). The input parameters are 
  `trip_id`, `from_stop_id`, `from_stop_sequence` and `co2`.
 

### Emission input data files

OTP can load emission data files as part of a GTFS bundle(zip, composite resource or directory) and
from separate files provided individually.

- If provided as part of a GTFS bundle the file MUST be named _emissions.txt_.
- If provided as a standalone file the file must be listed in the _build-config.json_ and the file
  name MUST include the word '"emisson"' and the extention must be `.csv` or `.txt`. The
  configuration must include a feedId for each file. This feedId is used to match the the trip id
  tho the transit feed. Both NeTEx and GTFS feeds are supported.

Both formats are supported as part of a GTFS bundle and as standalone files.

### Emission route format input data files

(by the Digitransit team)

The emissions are represented in grams per kilometer (g/Km) unit.

Emission file has the following columns:

| CSV Header                   | Description                                                                                   |
| :--------------------------- |:----------------------------------------------------------------------------------------------|
| `route_id`                   | Route id                                                                                      |
| `avg_co2_per_vehicle_per_km` | Average carbon dioxide equivalent value for the vehicles used on the route at grams/Km units. |
| `avg_passenger_count`        | Average passenger count for the vehicles on the route.                                        |

For example:

```csv
route_id,avg_co2_per_vehicle_per_km,avg_passenger_count
1234,123,20
2345,0,0
3456,12.3,20.0
```

### Emission trip hop format input data files
(by the Entur team)

The emissions are represented in grams per kilometer (g/Km) unit.

Emission file has the following columns:

| CSV Header                 | Description                                                                                                                                                                                                                                |
|:---------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `trip_id`                  | The GTFS trip id or the NeTEx ServiceJourney id                                                                                                                                                                                            |
| `from_stop_id`             | The GTFS stop id or the NeTEx Quey id. This is use together with the `from_stop_sequence` to match the stop. If the emissions data do not match the given transit feed, then the row is dropped and an issue is added to the build report. |
| `from_stop_sequence`       | The boardins stop sequence number in the trip stop pattern. The first stop is number one(1), not zero(0).                                                                                                                                  |
| `co2`                      | Average carbon dioxide equivalent value for the vehicles used on given trip hop starting at the given stop. The unit is grams per person per hop.                                                                                          |

For example:

```csv
trip_id,from_stop_id,from_stop_sequence,co2
Trip:3,Stop:A,1,90.456
Trip:3,Stop:A,2,130.483
Trip:3,Stop:A,3,172.646
```


### Configuration

To enable this functionality, you need to enable the "Co2Emissions" feature in the
`otp-config.json` file.

```JSON
//otp-config.json
{
  "Co2Emissions": true
}

```

Include the `emissions` object in the
`build-config.json` file. The `emissions` object should contain parameters called
`carAvgCo2PerKm` and `carAvgOccupancy`. The `carAvgCo2PerKm` provides the average emissions value for a car in g/km and
the `carAvgOccupancy` provides the average number of passengers in a car.

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// build-config.json
{
  "emission" : {
    "carAvgCo2PerKm" : 170,
    "carAvgOccupancy" : 1.3,
    "feeds" : [
      {
        "feedId" : "MY",
        "source" : "https://my.org/emissions/latest"
      }
    ]
  }
}
```
### Overview

| Config Parameter         |    Type    | Summary                                                                         |  Req./Opt. | Default Value | Since |
|--------------------------|:----------:|---------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| carAvgCo2PerKm           |   `gram`   | The average CO₂ emissions of a car in grams per kilometer.                      | *Optional* | `"170g"`      |  2.5  |
| carAvgOccupancy          |  `double`  | The average number of passengers in a car.                                      | *Optional* | `1.3`         |  2.5  |
| [feeds](#emission_feeds) | `object[]` | List of emisstion feeds.                                                        | *Optional* |               |  2.8  |
|       feedId             |  `string`  | Specify the feed id to use for matching transit ids in the emission input data. | *Required* |               |  2.8  |
|       source             |    `uri`   | Specify the feed source url.                                                    | *Required* |               |  2.8  |


### Details

<h4 id="emission_feeds">feeds</h4>

**Since version:** `2.8` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /emission 

List of emisstion feeds.




<!-- config END -->

## Changelog

### OTP 2.5

- Initial implementation of the emissions calculation. Support for the route input data format by 
  Digitransit.

### OTP 2.8

- Support for the trip input data format by Entur and for standalone input files.
