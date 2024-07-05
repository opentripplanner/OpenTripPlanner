# Vehicle Rental Service Directory API support.

This adds support for the GBFS service directory endpoint component located at 
https://github.com/entur/lamassu. OTP uses the service directory to lookup and connect to all GBFS
endpoints registered in the directory. This simplifies the management of the GBFS endpoints, since
multiple services/components like OTP can connect to the directory and get the necessary
configuration from it.


## Contact Info

- Entur, Norway


## Changelog

- Initial implementation of bike share updater API support
- Make json tag names configurable [#3447](https://github.com/opentripplanner/OpenTripPlanner/pull/3447)
- Enable GBFS geofencing with VehicleRentalServiceDirectory [#5324](https://github.com/opentripplanner/OpenTripPlanner/pull/5324)


## Configuration

To enable this you need to specify a url for the `vehicleRentalServiceDirectory` in
the `router-config.json`

### Parameter Summary

<!-- INSERT: PARAMETERS-TABLE -->


### Parameter Details

<!-- INSERT: PARAMETERS-DETAILS -->


### Example

<!-- INSERT: JSON-EXAMPLE -->
