# Skånetrafiken Deployment Configuration

This is a snapshot of Skånetrafiken deployment configuration. At Skånetrafiken we deploy our OTP
instances as microservices to a Kubernetes cluster. Some parts of the config are missing due to
confidentiality.

## Data input files

### NeTEx

We are using NeTEx for transport data inside Sweden. Each night we are building new file based on data from our SQL database. At the moment those files are not accessible through any public endpoint. 

### GTFS 

We are using GTFS data for traffic inside Denmark. GTFS for danish public transport can be downloaded [here](https://transitfeeds.com/p/rejseplanen/705?p=1).

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

#### Danmark

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

We are using **Azure Service Bus** to propagate SIRI SX and ET realtime messages to OTP. 
This is solved through Siri Azure updaters that we had implemented in OTP. We have separate updaters for SIRI SX and ET.
Except for the **Service Bus**we do also fetch old messages through a http endpoint on startup. 
This ensures that all OTP instance that we are running in the cluster does have exact same realtime data. 
So no matter which instance client is hitting it will always get the same search results.
See the `updaters` section of `router-config.json` file provided in this folder. This is an example
configuration for the updaters. 

Those updaters are used to provide data for Swedish traffic (NeTEx). Right now we are 
not connected to any realtime source for traffic inside Denmark (GTFS).


