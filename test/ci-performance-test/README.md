#### Data files

We use a static set of data files in the CI perfomance test, so the results are comparable. We will
periodically update these. This will of cause change the benchmark results from time to time.

- [Norwegian NeTEx data](https://leonard.io/otp/rb_norway-aggregated-netex-2021-12-11.zip)
- [Norway OSM data](https://download.geofabrik.de/europe/norway-210101.osm.pbf)

If the link above do not work you should be able to find it on the ENTUR web:

- https://www.entur.org/

#### Configure the test

- Pick a valid "testDate" for your data set and set it in the speed-test-config.json.
- Make sure build-config "transitServiceStart" and "transitServiceEnd" include the "testDate".
 
