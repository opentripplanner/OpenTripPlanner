# One-to-many Travel Time Analysis

This page covers one way to use OTP for one-to-many travel time analysis. Note that this page describes APIs that have been mostly undocumented, unsupported, and unmaintained for several years. They were created as a minimum viable prototype framework for cumulative opportunities accessibility calculations, which informed subsequent work on the R5 and Conveyal Analysis projects. As niche tools the latter do not have the same level of community activity as OTP, but represent a much more specialized descendant of the analysis functionality described here. Unlike OTP, they handle variability in travel time across a window of departure times, uncertainty in travel time for routes in scenarios defined in terms of headways, lightweight on-the-fly application of scenarios modifying the transit network, and a novel percentile-based definition of accessibility that is robust against outliers and partially unreachable destinations.

That said, a lot of people like to use OTP for more straightforward travel time and accessibility analysis, and this page is provided to serve as a starting point.

## Rationale
If you're calculating travel times from one or more origin points to a large set of destination points, you probably don't want to use OTP's default routing API for passenger-facing journey planning.

There are two main reasons for this. First, by default OTP does not optimize on travel time, it optimizes "generalized cost" including a lot of factors that have been introduced over time to reflect rider preferences. If changes to transportation services happen to open up new possibilities that are easier or more pleasant for the rider but slower, those slower travel times will be reported. This is then perceived as a confusing "decrease" in service quality due to added service. Second, finding paths between each origin/destination pair separately is extremely inefficient. If you need paths to more than one destination from each origin, it is generally preferable to find all those paths with a single search. It is possible to find paths from one origin to thousands or even millions of destinations in about the same amount of time it takes to find paths from one origin to one destination. Exploiting this one fact can easily reduce total computation time by orders of magnitude.

Note that the features described below are present in OTP 1.x, but have been removed from OTP 2.x. There are currently no plans to support analysis use cases in OTP 2, but OTP1 should remain usable for this purpose for people who wish to do so.

## Travel Time Surface / PointSet APIs

Definitions: 

A **PointSet** is a set of geographic locations that could serve as the origins or destinations of a batch of trip planning operations. They may have quantities of "opportunities" attached to them (such as jobs, square meters of retail space, hospital beds, etc.) which can be summed to yield cumulative opportunities accessibility metrics. 

The term **Surface** is used here in the sense of a function from a 2D location to a quantity. For any origin point we can produce a travel time surface, i.e. a travel time to every geographic coordinate that can be reached from that origin. 

Once created, such a surface may be evaluated at every destination in a PointSet at once, incurring minimal additional computational cost.

Because these APIs are not actively maintained and have the potential to cache large amounts of data in memory, they are disabled by default. You have to enable them with additional command line options when starting OTP. They are not intended for use in production on public servers. They were created for local research use, as a minimum viable prototype of this conceptual framework.

Although the PointSet class could certainly be initialized and used differently within Java code, for the purposes of these HTTP APIs they are always loaded from CSV or GeoJSON files in a directory specified on the command line. 

## Example

PointSets can be loaded from CSV files, with one point defined on each row. The file should be in UTF-8 encoding and have a header row giving the column names. At least one column should be called "lat" (or "latitude") and at least one should be called "lon" (or "longitude"). These specify a location for each point as floating point numbers in the WGS84 coordinate system. The other columns define the magnitude of opportunities reachable at each point (e.g. jobs or square meters of retail space).  These should always be integers. You can always set the magnitude to 1 if you're only interested in the travel time or quantity of points reached. Colons can be used in the magnitude column names to group them into categories. Here is a short example defining two points (perhaps representing buildings or small census units) with associated numbers of jobs in two categories:

```
lat,lon,jobs:total,jobs:retail,jobs:manufacturing,population:total
45.0,33.3,100,40,60,30
45.1,33.0, 20,15,5,40
```

As mentioned above it is also possible to load PointSets from GeoJSON. Examples of the CSV, GeoJSON, and Shapefile formats for PointSets can be found at https://github.com/opentripplanner/OpenTripPlanner/tree/master/src/test/resources/pointset

If you want to try out the synthetic data in that directory for Austin, Texas, you can find OSM data at https://www.nextzen.org/metro-extracts/index.html#austin_texas  and GTFS data at https://transit.land/feed-registry/operators/o-9v6-capitalmetro

If you had a directory tree rooted at `~/otp` with subdirectory `~/otp/graphs/austin` containing an OSM PBF file for the streets and a GTFS ZIP file with transit data, and another subdirectory `~/otp/pointsets` containing a PointSet CSV file named `destinations.csv`, you could perform accessibility calculations as follows:

First, build an OpenTripPlanner graph from the OSM and GTFS inputs:
`java -Xmx4G -jar otp-1.4.0-shaded.jar --build ~/otp/graphs/austin`

Then start the OpenTripPlanner server:
`java -Xmx4G -jar otp-1.4.0-shaded.jar --graphs ~/otp/graphs --router austin --server --analyst --pointSets ~/otp/pointsets`

The OpenTripPlanner server should then be available at http://localhost:8080/index.html. The `--analyst` switch enables the HTTP endpoints that calculate travel time surfaces, and the `--pointSet` parameter tells OTP where to look to load any pointsets referenced in those API requests.

The contents of the PointSets should then be visible at:
`http://localhost:8080/otp/pointsets/destinations`

You can then create a travel time surface by hitting `/otp/surfaces` with a POST request, using query parameters similar to those in a normal OTP trip planning API request:

`curl -X POST "http://localhost:8080/otp/surfaces?batch=true&fromPlace=30.285159872426014%2C-97.7467346191406&time=5%3A46pm&date=07-31-2020&mode=TRANSIT%2CWALK&maxWalkDistance=800&arriveBy=false"

Note the `batch=true` parameter. This is important to tell OTP not to validate the toPlace parameter, which we're leaving out to build paths to all destinations from one origin. 

If the request is successful, the server should respond with a JSON representation of a single travel time surface record, something like:
`{"id":0,"params":{"date":"07-31-2020","mode":"TRANSIT,WALK","arriveBy":"false","fromPlace":"30.285159872426014,-97.7467346191406","batch":"true","time":"5:46pm","maxWalkDistance":"800"}}`

This same record can now be seen at http://localhost:8080/otp/surfaces
which lists all recently created travel time surfaces.

Now that we have a surface and know its ID from the JSON (in this case 0), we can evaluate that surface at a set of points, like so:
`http://localhost:8080/otp/surfaces/0/indicator?targets=destinations`

That loads the pointset from `destinations.csv` and finds the travel time to each point in it, then aggregates the results into an accessibility indicator (hence the endpoint name "indicator"). The "data" property of the JSON response contains, for each column present in the pointset, an array of "sums" and "counts". The counts array represents the number of points reached at each minute of travel time from the origin. The "sums" array sums up the magnitudes of the points reached within each minute. Summing the values in the array from 0 to N then gives a cumulative opportunities accessibility indicator for N minutes of travel.

If you add the query parameter `detail=true`, the response will also contain individual travel times to every destinations point in the PointSet.

These indicator calls could in principle be combined with the one creating the travel time surface, eliminating the need to retain the surface on the server and make a seconds call. But retaining the surface is particularly useful for interactively generating multiple isochrones or rendering tiles, where someone might want to scroll around and zoom in generating thousands of images from the same surface. There are other endpoints for those purposes.


## Client Code

The API endpoints described above are used when you switch to "Analyst" mode in the default OTP1 client at localhost:8080. You can use your browser's dev tools to observe the requests it sends, which render map tiles from the surface. Note that the tile requests are currently a bit strange: the UI is redundantly sending the full routing query string with every request, a vestige of the older system that did not assign IDs to cached travel time surfaces and instead looked them up based on the request parameters.

## Relevant Java Classes

Though it is perhaps not ideal, the best way to understand this aspect of OTP is probably to dig through source code. Throughout its history OTP has been heavily used by people who worked directly with the source code, and documentation has not always kept pace with more experimental parts of the system, though there is a decent amount of commentary inline in the source code. Here are some classes that may serve as good starting points:

- org.opentripplanner.analyst.PointSet

- org.opentripplanner.analyst.SurfaceCache The cache of travel time surfaces is defined at org.opentripplanner.analyst.SurfaceCache and is currently hard-wired to hold on to the last 100 surfaces generated. Please don't read too much into these "design decisions" resulting in a stateful API susceptible to memory exhaustion. Again, this was created as a minimum viable prototype for use in local analysis work, and is not really suitable for a public-facing system.

- org.opentripplanner.api.resource.SurfaceResource Defines the HTTP API endpoints for working with PointSets and travel time surfaces. Whether or not you actually use this HTTP API, this class is important as the only example in the OTP codebase of how to construct a minimum-travel-time surface and bulk-evaluate it at a large number of destinations. These patterns of Java code could be adapted in your own methods, or perhaps integrated with the scripting API. Once a surface exists, you can also make isochrones and web Mercator map tiles of the travel time surface for display.

- org.opentripplanner.analyst.DiskBackedPointSetCache This loads the PointSets from disk and holds them in memory for use as destinations via the API. A request for the PointSet named "schools" will try to load it from the first file in the directory whose name begins with "schools" and ends with ".csv" or ".json". This defers to org.opentripplanner.analyst.PointSet#fromCsv and org.opentripplanner.analyst.PointSet#fromGeoJson to load files depending on the extension. (Note that there is also a PointSet#fromShapefile for Esri Shapefiles which is currently unused). 