# Container image

The CI pipeline deploys container images for runtimes like Docker, Kubernetes or Podman to 
[Dockerhub](https://hub.docker.com/r/opentripplanner/opentripplanner/tags).

The image assumes you use a volume to mount the input data (GTFS/NeTex, OSM) and config files into 
`/var/opentripplanner/`. When serving a graph it's also expected to be in this directory.

## Quick start

Let's use the image to build a graph in Berlin.

```bash
# create directory for data and config
mkdir berlin
# download OSM
curl -L https://download.geofabrik.de/europe/germany/berlin-latest.osm.pbf -o berlin/osm.pbf  
# download GTFS
curl -L https://vbb.de/vbbgtfs -o berlin/vbb-gtfs.zip
# build graph and save it onto the host system via the volume
docker run --rm -v "$(pwd)/berlin:/var/opentripplanner" docker.io/opentripplanner/opentripplanner:latest --build --save 
# load and serve graph
docker run -it --rm -p 8080:8080 -v "$(pwd)/berlin:/var/opentripplanner" docker.io/opentripplanner/opentripplanner:latest --load --serve
```

Now open [http://localhost:8080](http://localhost:8080) to see your running OTP instance.

## Additional notes
Make sure to include the word "gtfs" when naming the gtfs files that you want to use for OTP. Otherwise, graph build will fail.

If you want to set JVM options you can use the environment variable `JAVA_TOOL_OPTIONS`, so
a full example to add to your docker command is `-e JAVA_TOOL_OPTIONS='-Xmx4g'`. 
