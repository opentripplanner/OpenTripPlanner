# Pedestrian Routing

OpenTripPlanner 1.X introduces extended functionality for weighed pedestrian routing based on OpenStreetMap tags. This functionality can be used to favor or disfavor specific street edges in walk routing based on properties of the corresponding OSM ways, such as roadway type, the presence of sidewalks, etc.

## Walk Comfort Configuration

At the heart of the pedestrian routing functionality is the "walk comfort" configuration, which defines a set of "rules" that map specific key/value pairs in a street way's OSM property set to "walk comfort" factors which are then applied to the routing weight of any derived street edges in the street network graph.

A single rule specifies a "test" that consists of an OSM tag key/value pair, as well as a numeric factor that is applied to any ways that satisfy the key/value test. The factor is a multiplier that is applied to the default edge weight (which itself is a combination of edge length and elevation-derived steepness, if applicable); factors *greater* than 1.0 adjust the weight upward (i.e. making the edge *less* attractive to routing) while factors *less* than 1.0 adjust the weight downward (making it *more* attractive).

The walk configuration file is an optional JSON file called `walk-config.json` that must reside in graph's home directory (alongside `build-config.json` and `router-config.json`, if provided). Following is an example of a simple two-rule walk comfort configuration:

```JSON
// build-config.json
{
  "rules": [
    {
      "key": "highway",
      "value": "primary",
      "factor": 1.5
    },
    {
      "key": "sidewalk",
      "value": "both",
      "factor": 0.9
    }
  ]
}
```

In the above example, two rules are created; one applies a factor of 1.5 (i.e. a 50 percent "penalty") to ways that are designated `primary` highways, and one that applies a factor of 0.9 (i.e. a 10 percent "bonus") to ways that are coded as having sidewalks on both sides of the street.

If multiple rules are satisfied for a given way, the final walk comfort factor will be the product of all associated rule-level factors; in the above example, a way that is both a primary highway and that also has sidewalks would have a final walk comfort factor of 1.5 * 0.9 = __1.35__.

The is no limit on the number of rules that may be specified, and the order of the rules in the configuration file has no affect on how they are applied.

## Dynamic Configuration Reloading

In most production environments, the walk configuration file will be provided at graph build time, with the walk comfort factors computed as part of the graph build process and stored with the street edges in the `Graph.obj` file.

For development and testing of walk comfort rules, however, the ability to make updates to the walk rules and see their impact without rebuilding the graph is helpful. To that end, OTP also includes a mechanism for updating and reloading the walk configuration for running servers "on the fly".

To enable this feature, a special flag, `includeOsmTags`, must be set to `true` in the graph builder's `build-config.json` file, as shown in the following example. This will cause the full set of OSM property tags to be embedded in the graph for all street edges.

```JSON
// build-config.json
{
  "includeOsmTags": true
}
```

Enabling this flag will increase the size of the graph, typically by 10 to 20 percent depending on how extensively tagged the OSM data is. Therefore, its use should be limited to testing and development contexts.

Once a graph that was built with the `includeOsmTags` flag enabled is deployed to an OTP server, the `walk-config.json` file can be dynamically reloaded by invoking a special `/walk_comfort` OTP server endpoint; for the default router on a locally-deployed server on port 8001, the endpoint URL would be `http://localhost:8001/otp/routers/default/walk_comfort`

Invoking this endpoint will reload the `walk-config.json` file (which must be located in the current graph's home directory) and will use the freshly loaded rules to recalculate the walk comfort factors for all street edges in the graph. A query to this endpoint will return a simple JSON response indicating whether the query was successful, the number of street edges updated, and the time in milliseconds taken by the query; following is an example of such a response:

```JSON
{
  "success": true,
  "edgesUpdated": 37667,
  "time": 38
}
```

Any subsequent routing requests will be performed on the updated graph, but note that the `Graph.obj` file is *not* rewritten on disk; to incorporate any updated walk rules into the graph file it must be rebuilt using the graph builder.
