# Data Sources

## Input Formats

At the core of OpenTripPlanner is a library of Java code that finds efficient paths through
multi-modal transportation networks built
from [OpenStreetMap](http://wiki.openstreetmap.org/wiki/Main_Page)
and [GTFS](https://developers.google.com/transit/gtfs/) data. It can also receive GTFS-RT (real-time)
data.

In addition to GTFS, OTP can also load data in the Nordic Profile of Netex, the EU-standard transit
data interchange format. The upcoming EU-wide profile was heavily influenced by the Nordic Profile
and uses the same schema, so eventual support for the full EU profile is a possibility.

GTFS and Netex data are converted into OTP's own internal model which is a superset of both. It is
therefore possible to mix Netex and GTFS data, and potentially even data from other sources.
