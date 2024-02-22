# Skånetrafiken Deployment Configuration

This is a snapshot of Skånetrafiken deployment configuration. At Skånetrafiken we deploy our OTP
instances as microservices to a Kubernetes cluster. Some parts of the config are missing due to
confidentiality.

## Data input files

### NeTEx

NeTEx is used for transport data inside Sweden. Each night the system automatically builds new file
based on
data from SQL database. At the moment those files are not accessible through any public
endpoint.

### GTFS

GTFS data is used for traffic inside Denmark. GTFS for danish public transport can be
downloaded [here](https://transitfeeds.com/p/rejseplanen/705?p=1). There is some processing applied
to the original data so that unnecessary trips are filtered out and ID structure for journeys and
stop point / stop places matches with the NeTEx. The modified GTFS is not accessible through any
public
endpoint either.

### OSM

Two different OSM files are used:

#### Sweden

OSM data is downloaded from `http://download.geofabrik.de/europe/sweden-latest.osm.pbf`.
To reduced graph size, only data for southern part of Sweden is used.

#### Denmark
OSM data is downloaded from `http://download.geofabrik.de/europe/denmark-latest.osm.pbf`.
To reduce graph size, only data for northern part of Denmark is used.

## Real-time

The **Azure Service Bus** is used to propagate SIRI SX and ET real-time messages to OTP.
This is solved through Siri Azure updaters that Skånetrafiken had implemented in OTP. There are
separate updaters for SIRI SX and ET.
Those updaters are used to provide data for Swedish traffic (NeTEx). Right now, there is no
connection to any real-time source for danish traffic (GTFS data).

Except for receiving messages from **Service Bus** there are two endpoints through which historical
ET and SX messages can be downloaded at OTP startup.
The updater will first create new subscription on ServiceBus topic and then send request to the
history endpoint.
It can take some time get a response from the endpoint, so the timeout is set quite high.
Once OTP is done with processing of history messages the updater will start querying messages from
the subscription.

Once the updaters are done with processing of history messages they will change their status to
primed,
and the system will start channeling request to this OTP instance.
This ensures that no real-time message is omitted and all OTP instance that ran in the
cluster does have exact same real-time data.
Thi means that no matter which instance the client is hitting it will always get the same search
results.

### History endpoint contract

See the `updaters` section of `router-config.json` file provided in this folder. This is an example
configuration for the updaters. The `history` configuration is optional. It can be skipped so that
OTP does not fetch any historical messages on startup.

There are two separate endpoints for respectively SX and ET. They are basic GET endpoints with
following query parameters:

| parameters   | format      |
|--------------|-------------|
| fromDateTime | ISO 8601    |
| toDateTime   | ISO 8601    |

Those two parameters are used to define time boundaries for the messages.

Both endpoints generate XML response which is an SIRI object containing SX or ET messages. Messages
are
formatted according to Siri Nordic Profile.
Since in SIRI ET standard each messages contains all necessary data, Skånetrafikens implementation
of the
endpoint returns only the last message
for each DatedServiceJourney ID (sending multiple messages would be pointless since they will
override each other).
The messages are processed in the same order as they came in (in the list) so it would still work
to include multiple messages on same DatedServiceJourney as long as they are sorted in correct order
and the newest message is the last one in the list.

### Matching on stop arrival-times
Normally ET messages are matched with corresponding trips based on ServiceJourney or DatedServiceJourney
id from the message. In case OTP was not able to find corresponding trip additional search will be
performed based on arrival-times/stop-patterns from the ET message. This feature turned off by default but can be
activated by adding *fuzzyTripMatching* property to updater configuration. 

### FederatedIdentity
It is also possible to connect to Service Bus through FederatedIdentity. Change **authenticationType** to
**FederatedIdentity** and provide **fullyQualifiedNamespace** in router-config.





