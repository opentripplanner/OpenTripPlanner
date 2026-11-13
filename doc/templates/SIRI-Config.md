These updaters support consuming SIRI-ET and SX messages via HTTPS. They aim to support the
[Nordic and EPIP SIRI profiles](./features-explained/Netex-Siri-Compatibility.md) which are subsets
of the SIRI specification.

For more documentation about the Norwegian profile and data, go to the
[Entur Real-Time Data](https://developer.entur.org/pages-real-time-intro) documentation and the
[Norwegian SIRI profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile).

To enable one of the SIRI updaters you need to add it to the `updaters` section of the
`router-config.json`.

## SIRI-ET Request/Response via HTTPS

This requires there to be a SIRI server that handles SIRI POST requests, stores requestor refs and
responds only with the newest data.

<!-- INSERT: siri-et-updater -->

## SIRI-SX Request/Response via HTTPS

<!-- INSERT: siri-sx-updater -->

## SIRI-ET Lite

SIRI Lite is not very well specified [[1]](https://nextcloud.leonard.io/s/8kZGNA7LHjd6ZbJ?dir=/)
[[2]](https://normes.transport.data.gouv.fr/normes/siri/profil-france/#protocoles-d%C3%A9change-des-donn%C3%A9es-siri)
but this updater supports the following definition:

> Fetching XML-formatted SIRI messages as a single GET request rather than the more common
> request/response flow.

This means that the XML feed must contain all updates for all trips, just like it is the case for
GTFS-RT TripUpdates.

<!-- INSERT: siri-et-lite -->

## SIRI-SX Lite

This updater follows the same definition of SIRI Lite as the SIRI-ET one: it downloads the entire
feed in a single HTTP GET request.

<!-- INSERT: siri-sx-lite -->

## SIRI-FM Elevator Status

This updater reads the status of elevators from a SIRI-FM (Facility Monitoring) feed. It downloads
the entire feed in a single HTTP GET request, and expects the status of all elevators in each
response: an elevator that is not in the response is in service.

An elevator that is out of service is not used by street routing, for example for wheelchair users.
The regular transfers avoid it too, if alternative transfer paths were generated at graph build
(`elevatorAlternatives` in the transfer configuration).

The `FacilityRef` of a `FacilityCondition` must be the id of an elevator in the street graph. The
ids of the elevators are read from the OSM tags configured with `elevatorRefTags` in the build
configuration. A `FacilityCondition` whose features are given and do not include `lift` is ignored.

<!-- INSERT: siri-fm-elevator -->
