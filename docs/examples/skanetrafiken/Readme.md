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

To reduced graph size, only data for southern part of Sweden is used. Script below downloads OSM
for whole country and then clips out the northern part.

```
#!/bin/bash

bashdir=$(dirname "$(readlink -f "$0")")

mapHost=http://download.geofabrik.de/europe
downloadFile=sweden-latest.osm.pbf
now=$(date +%Y-%m-%dT%H%M%S)
skanetrafikenFile=skanetrafiken.osm.pbf
filterFile=skanetrafiken-filtered-$now.osm.pbf


if [ -d /tmp/osm-skanetrafiken ]; then
        echo "Delete existing /tmp/osm-skanetrafiken"
        rm -rf /tmp/osm-skanetrafiken
fi

mkdir /tmp/osm-skanetrafiken
cd /tmp/osm-skanetrafiken


echo "Start download"
wget $mapHost/$downloadFile

echo "Cut skanetrafiken region"
osmium extract --strategy complete_ways --bbox 11.7807,55.3,17.2162,57.7941 $downloadFile -o $skanetrafikenFile

echo "Filter OSM data"
osmium tags-filter $skanetrafikenFile w/highway w/public_transport=platform w/railway=platform w/park_ride=yes r/type=restriction -o $filterFile -f pbf,add_metadata=false

mv $filterFile $bashdir

echo "Created filtered OSM for skanetrafiken region ($filterFile)"

rm -rf /tmp/osm-skanetrafiken
```

#### Denmark

To reduce graph size, only data for northern part of Denmark is used. Script below downloads OSM
for whole country and then clips out the southern part.

```
#!/bin/bash

bashdir=$(dirname "$(readlink -f "$0")")

mapHost=http://download.geofabrik.de/europe
downloadFile=denmark-latest.osm.pbf
now=$(date +%Y-%m-%dT%H%M%S)
denmarkFile=denmark.osm.pbf
filterFile=denmark-filtered-$now.osm.pbf

if [ -d /tmp/osm-skanetrafiken ]; then
        echo "Delete existing /tmp/osm-denmark"
        rm -rf /tmp/osm-denmark
fi

mkdir /tmp/osm-denmark
cd /tmp/osm-denmark

echo "Start download"
wget $mapHost/$downloadFile

echo "Cut skanetrafiken region"
osmium extract --strategy complete_ways --polygon oresund.json $downloadFile -o $denmarkFile

echo "Filter OSM data"
osmium tags-filter $denmarkFile w/highway w/public_transport=platform w/railway=platform w/park_ride=yes r/type=restriction -o $filterFile -f pbf,add_metadata=false

mv $filterFile $bashdir

echo "Created filtered OSM for skanetrafiken region ($filterFile)"

rm -rf /tmp/osm-denmark
```

## Realtime

The **Azure Service Bus** is used to propagate SIRI SX and ET realtime messages to OTP.
This is solved through Siri Azure updaters that Skånetrafiken had implemented in OTP. There are
separate updaters for SIRI SX and ET.
Those updaters are used to provide data for Swedish traffic (NeTEx). Right now, there is no
connection to any realtime source for danish traffic (GTFS data).

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
This ensures that no realtime message is omitted and all OTP instance that ran in the
cluster does have exact same realtime data.
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






