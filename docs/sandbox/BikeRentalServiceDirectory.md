# Bike Rental Service Directory API support.

## Contact Info
- Gard Mellemstrand, Entur, Norway

## Changelog
- Initial implementation of bike share updater API support

## Documentation
This adds support for the GBFS service directory endpoint component located at https://github.com/entur/bikeservice. OTP use the service directory to lookup and connect to all bike share operation registered in the directory. This simplify the management of the bike share enpoints, since multiple services/components like OTP can connect to the directory and get the necessary configuration from it. 

### Configuration
To enable this you need to specify a url for the `bikeRentalServiceDirectory` in the `router-config.json`
