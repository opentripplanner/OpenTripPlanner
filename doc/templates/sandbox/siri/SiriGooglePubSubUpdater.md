# Siri-ET Google PubSub Updater

Support for consuming SIRI-ET messages over a Google Cloud PubSub subscription.
Similarly to the SIRI-ET HTTP updater, this updater is developed to support the Nordic SIRI profile
which is a subset of the SIRI specification.

## Contact Info
Entur, Norway
https://entur.no/

## Documentation

This updater consumes SIRI real time information over an asynchronous publisher/subscriber feed
provided by a Google Cloud PubSub topic.

For more documentation see
the [Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and
the [Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile)
.

## Configuration

To enable the SIRI-ET Google PubSub updater you need to add it to the updaters section
of the `router-config.json`.

### Siri-ET via Google PubSub

<!-- INSERT: siri-et-google-pubsub-updater -->

