# Updater configuration

After the graph build has completed it is possible to dynamically add frequently changing real-time 
data to OTP.

To do this, configure an updater in the `updaters` section of `router-config.json`.

Each updater has a `type` field and other configuration option specific to their functionality which
are described in the following pages.

## Other updaters in sandboxes

There are also a number of updaters that are not part of the core code and maintained by their 
respective organisations. 

- [Vehicle parking](sandbox/VehicleParking.md)
- [SIRI over Google Cloud PubSub](sandbox/siri/SiriGooglePubSubUpdater.md)
- [SIRI over Azure Message Bus](sandbox/siri/SiriAzureUpdater.md)
- [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md)

