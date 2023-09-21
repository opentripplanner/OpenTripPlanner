# Digitransit CO2 Emissions calculation

## Contact Info

- Digitransit Team

## Documentation

CO2 Emissions and ability to show them via plan queries in itinerary by Digitransit team.
This implementation enables OTP to retrieve CO2 information, which is then utilized during 
itinerary queries. The emissions are represented in grams per kilometer (g/Km) unit

Emissions data is located in an emissions.txt file within a gtfs package and has the following properties:

`route_id`: route id

`avg_co2_per_vehicle_per_km`: Average carbon dioxide equivalent value for the vehicles used on the route at grams/Km units.

`avg_passenger_count`: Average passenger count for the vehicles on the route.

For example:
```csv
route_id,avg_co2_per_vehicle_per_km,avg_passenger_count
1234,123,20
2345,0,0
```

Emissions data is loaded from the gtfs package and embedded into the graph during the build process.


### Configuration
To enable this functionality, you need to add the "Co2Emissions"  feature in the
`otp-config.json` file. 

```json
//otp-config.json
{
  "Co2Emissions": true
}
```
Include the `digitransitEmissions` object in the
`build-config.json` file. The `digitransitEmissions` object should contain parameters called
`carAvgCo2` and `carAvgOccupancy`. The `carAvgCo2` provides the average emissions value for a car and
the `carAvgOccupancy` provides the average number of passengers in a car.

```json
//build-config.json
{
  "digitransitEmissions": {
    "carAvgCo2": 170,
    "carAvgOccupancy": 1.3
  }
}
```
## Changelog

### OTP 2.5

- Initial implementation of the emissions calculation.


