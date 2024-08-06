# Siri Updater

Support for consuming SIRI ET and SX messages. The updater is developed to support the Nordic
SIRI profile which is a subset of the SIRI specification.

## Contact Info

- Lasse Tyrihjell, Entur, Norway

## Documentation

This updater consumes SIRI real time information. It is developed by Entur and supports the Nordic
Profile for SIRI. It should be possible to develop it further to support a broader set of the SIRI
specification.

For more documentation goto
the [Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI updater you need to add it to the updaters section of the `router-config.json`.

### Siri-ET via HTTPS

<!-- INSERT: siri-et-updater -->

### Siri-SX via HTTPS

<!-- INSERT: siri-sx-updater -->

## Changelog

- Initial version of SIRI updater (October 2019)
- Include situations with no or no handled entity selectors with Unknown EntitySelector (December
  2021, https://github.com/opentripplanner/OpenTripPlanner/pull/3780)
