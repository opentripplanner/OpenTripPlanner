# SIRI-ET MQTT Updater

Support for consuming SIRI-ET messages over an MQTT subscription. Similarly to the SIRI-ET HTTP 
updater, this updater is developed to support the Nordic SIRI profile which is a subset of the SIRI 
specification.

## Contact Info
HBT, Hamburg, Germany
jessica.koehnke@hbt.de

## Documentation

This updater consumes SIRI real time information over an asynchronous MQTT feed
by subscribing to an MQTT topic.

For more documentation see
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI-ET MQTT updater you need to add it to the updaters section
of the `router-config.json`.

### SIRI-ET via MQTT

<!-- INSERT: siri-et-mqtt -->

