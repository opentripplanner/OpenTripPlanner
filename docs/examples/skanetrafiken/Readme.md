# Skånetrafiken Deployment Configuration

This is a snapshot of Skånetrafiken deployment configuration. At Skånetrafiken we deploy our OTP
instances as microservices to a Kubernetes cluster. Some parts of the config are missing due to
confidentiality.

## Data input files

### NeTEx

We are using NeTEx for transport data inside Sweden. Each night we are building new file based on
data from our SQL database. At the moment those files are not accessible through any public
endpoint.

### GTFS

We are using GTFS data for traffic inside Denmark. GTFS for danish public transport can be
downloaded [here](https://transitfeeds.com/p/rejseplanen/705?p=1).

### OSM

We are using two OSM files:

#### Sweden

We download OSM data for whole Sweden, and then we clip northern part of it to reduce file size.

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

We download OSM data for whole Denmark, and then we clip southern part to reduce file size.

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
This is solved through Siri Azure updaters that we had implemented in OTP. We have separate updaters
for SIRI SX and ET.
Those updaters are used to provide data for Swedish traffic (NeTEx). Right now, there is no
connection
to any realtime source for GTFS.

Except for receiving messages from **Service Bus** we do also implement endpoints through which we
fetch old messages on startup.
We have two separate endpoint for respectively SX and ET history.
The updater will first create new subscription on ServiceBus topic and then send request to the
history endpoint.
For Us, it takes some time get a response from the endpoint, so we tend to set timeout quite high.
Once OTP is done with processing of history messages the updater will start querying messages from
the subscription.
This ensures that no realtime message is omitted and all OTP instance that we are running in the
cluster does have exact same realtime data.

Once the updaters are done with processing of history messages they will change their status to
primed,
and we will start channeling request to this OTP instance.
This ensures that all OTP instances does have identical realtime state and no matter which instance
the client is hitting it will always get the same search results.

### History endpoint contract

See the `updaters` section of `router-config.json` file provided in this folder. This is an example
configuration for the updaters. The `history` configuration is optional. It can be skipped so that
OTP does not fetch any old messages on startup.

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
Since in SIRI ET standard each messages contains all necessary data, our implementation of the
endpoint returns only the last message
for each DatedServiceJourney ID (sending multiple messages would be pointless since they will
override each other).
The messages are processed in the same order as they came in (in the list) so it would still work
to include multiple messages on same DatedServiceJourney as long as they are sorted in correct order
and the newest message is the last one in the list.






