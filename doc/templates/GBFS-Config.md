<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user 
-->

Besides GTFS-RT or SIRI transit data, OTP can also fetch real-time data about vehicle rental networks
including the number of vehicles and free parking spaces at each station. We support vehicle rental
systems that use the GBFS standard.

[GBFS](https://github.com/NABSA/gbfs) can be used for a variety of shared mobility services, with
partial support for both v1 and v2.2 ([list of known GBFS feeds](https://github.com/NABSA/gbfs/blob/master/systems.csv)). OTP supports the following
GBFS form factors:

- bicycle
- scooter
- car

<!-- INSERT: vehicle-rental -->

## Other updaters in sandboxes

- [Vehicle parking](sandbox/VehicleParking.md)
- [SIRI over HTTP](SIRI-Config.md)
- [SIRI over Google Cloud PubSub](sandbox/siri/SiriGooglePubSubUpdater.md)
- [SIRI over Azure Message Bus](sandbox/siri/SiriAzureUpdater.md)
- [VehicleRentalServiceDirectory](sandbox/VehicleRentalServiceDirectory.md)

