# Digitransit CO2 Emissions calculation

## Contact Info

- Digitransit Team

## Documentation

CO2 Emissions and ability to show them via plan queries in itinerary by Digitransit team.
This implementation enables OTP to retrieve CO2 information, which is then utilized during 
itinerary queries. The emissions are represented in grams per kilometer (g/Km) unit

Emissions data is located in an emissions.txt file within a gtfs.zip and has the following properties:

`route_id`: route id

`agency_id`: agency id

`route_short_name`: Short name of the route.

`type`: Mode of transportation.

`avg`: Average carbon dioxide equivalent value for the vehicles used on the route at grams/Km units

`p_avg`: Average passenger count for the vehicles on the route.

For example:
```csv
route_id,agency_id,route_short_name,type,avg,p_avg
1234,HSL,545,BUS,123,20
2345,HSL,1,TRAM,0,0
```

Emissions data is loaded from the gtfs.zip file(s) and embedded into the graph during the build process.


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


