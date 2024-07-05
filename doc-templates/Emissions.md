# CO₂ Emissions calculation

## Contact Info

- Digitransit Team

## Documentation

Graph build import of CO₂ Emissions from GTFS data sets (through custom emissions.txt extension)
and the ability to attach them to itineraries by Digitransit team.
The emissions are represented in grams per kilometer (g/Km) unit.

Emissions data is located in an emissions.txt file within a gtfs package and has the following columns:

`route_id`: route id

`avg_co2_per_vehicle_per_km`: Average carbon dioxide equivalent value for the vehicles used on the route at grams/Km units.

`avg_passenger_count`: Average passenger count for the vehicles on the route.

For example:
```csv
route_id,avg_co2_per_vehicle_per_km,avg_passenger_count
1234,123,20
2345,0,0
3456,12.3,20.0
```

Emissions data is loaded from the gtfs package and embedded into the graph during the build process.


### Configuration
To enable this functionality, you need to enable the "Co2Emissions"  feature in the
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

<!-- INSERT: config -->

## Changelog

### OTP 2.5

- Initial implementation of the emissions calculation.
