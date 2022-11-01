# Siri Updator

Support for consuming SIRI ET, SX and ET messages. The updator is developed to support the Norwegian
SIRI profile which is a subset of the SIRI specification.

## Contact Info

- Lasse Tyrihjell, Entur, Norway

## Changelog

- Initial version of SIRI updator (October 2019)
- Include situations with no or no handled entity selectors with Unknown EntitySelector (December
  2021, https://github.com/opentripplanner/OpenTripPlanner/pull/3780)

## Documentation

This updator consumes SIRI Real Time Information. It is developed by entur and support the Nordic
Profile for SIRI. It should be possible to develop it further to support a broader set of the SIRI
specification.

For more documentation goto
the [Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

### Configuration

To enable the SIRI updator you need to add it to the updaters section of the `router-config.json`.

```
{
    "type": "siri-updater",
    "frequencySec": 60,
    "url": "https://api.updater.com/example-updater"
}
```
