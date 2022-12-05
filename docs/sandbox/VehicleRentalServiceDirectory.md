# Vehicle Rental Service Directory API support.

## Contact Info

- Gard Mellemstrand, Entur, Norway

## Changelog

- Initial implementation of bike share updater API support
- Make json tag names
  configurable [#3447](https://github.com/opentripplanner/OpenTripPlanner/pull/3447)

## Documentation

This adds support for the GBFS service directory endpoint component located
at https://github.com/entur/lahmu. OTP use the service directory to lookup and connect to all GBFS
endpoints registered in the directory. This simplify the management of the GBFS endpoints, since
multiple services/components like OTP can connect to the directory and get the necessary
configuration from it.

### Configuration

To enable this you need to specify a url for the `vehicleRentalServiceDirectory` in
the `router-config.json`
