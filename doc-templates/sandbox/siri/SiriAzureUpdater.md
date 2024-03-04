# Siri Azure Updater

It is sandbox extension developed by Skånetrafiken that allows OTP to fetch Siri ET & SX messages through *Azure Service Bus*.
IT also OTP to download historical data from en HTTP endpoint on startup.

## Contact Info

Skånetrafiken, Sweden  
developer.otp@skanetrafiken.se

## Documentation

Documentation available [here](../../examples/skanetrafiken/Readme.md).

## Configuration

To enable the SIRI updater you need to add it to the updaters section of the `router-config.json`.

### Siri Azure ET Updater

<!-- INSERT: siri-azure-et-updater -->

### Siri Azure SX Updater

<!-- INSERT: siri-azure-sx-updater -->

## Changelog
- Added configuration for turning off stop arrival time match feature.
- Initial version (April 2022)
- Minor changes in logging (November 2022)
- Retry fetch from history endpoint if it failed (February 2023)
- Solve a bug in SiriAzureETUpdater and improve error logging (March 2023)
- Add support with federated identity authentication (February 2024)