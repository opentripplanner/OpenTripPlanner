# Pedestrian Routing

OpenTripPlanner 1.X introduces extended functionality for weighed pedestrian routing based on OpenStreetMap tags. This functionality can be used to favor or disfavor specific street edges in walk routing based on properties of the corresponding OSM ways, such as roadway type, the presence of sidewalks, etc.

## Walk Comfort Configuration

At the heart of the pedestrian routing functionality is the "walk comfort" configuration, which defines a set of "rules" that map specific conditions based on a way's OSM property set to "walk comfort" factors which are then applied to the routing weight of any derived street edges in the street network graph.

A single rule specifies one or more "tests" that must be satisfied in order for the rule's weighting factor to be applied to a given way. The factor is a multiplier that is applied to the default edge weight (which itself is a combination of edge length and elevation-derived steepness, if applicable). Factors *greater* than 1.0 adjust the weight upward (i.e. making the edge *less* attractive to routing) while factors *less* than 1.0 adjust the weight downward (making it *more* attractive).

The walk configuration file is an optional JSON file called `walk-config.json` that must reside in graph's home directory (alongside `build-config.json` and `router-config.json`, if provided). Following is an example walk comfort configuration consisting of two simple tag value comparison rules:

```JSON
// walk-config.json
{
  "rules": [
    {
      "type": "equal",
      "key": "highway",
      "value": "primary",
      "factor": 1.5
    },
    {
      "type": "equal",
      "key": "sidewalk",
      "value": "both",
      "factor": 0.9
    }
  ]
}
```

In the above example, the first rule applies a factor of 1.5 (i.e. a 50 percent "penalty") to ways that are designated `primary` highways, while the second rule applies a factor of 0.9 (i.e. a 10 percent "bonus") to ways that are coded as having sidewalks on both sides of the street.

If multiple rules are satisfied for a given way, the final walk comfort factor will be the product of all associated rule-specified factors; in the above example, a way that is both a primary highway and that also has sidewalks would have a final walk comfort factor of 1.5 * 0.9 = __1.35__.

The is no limit on the number of rules that may be specified, and the order of the rules in the configuration file has no affect on how they are applied.

## Test types

Several different test types are supported, which are specified by the test's `type` property. Some test types (e.g. `and` and `or`) contain multiple sub-tests, allowing for rules consisting of nested tests (see example below). Following are the different test types and associated additional properties:

### `equal`
A basic text-based tag value comparison as illustrated in the above example.
  * `key` - the OSM tag key to be matched (e.g. `"highway"`)
  * `value` - the specific reference value(s) to be matched. May either be a single string (e.g. `"primary"`) or an array of strings (e.g. `["primary", "secondary"]`)

*Note: if the `type` field is omitted for a test, an `equal` test is assumed.*

### `absent`
A test of whether a tag is absent from a way's property set. The test only passes if a specified tag is *not* present for a given way.
* `key` - the OSM tag key that must not be present (e.g. `"cycleway"`)

### `numeric-equal`
An equality test where both the reference value and all tested tag values are assumed to be numeric (e.g. `2` will equal `2.0`). If either the reference value or the test value is non-numeric, the test will fail.
* `key` - the OSM tag key to be matched (e.g. `"lanes"`)
* `value` - a single numeric reference value to be matched (e.g. `2`)

### `greater`
A numeric test where the tag value must be greater than a specified reference value. If either the reference value or the test value is non-numeric, the test will fail.
* `key` - the OSM tag key to be matched (e.g. `"lanes"`)
* `value` - a single numeric reference value (e.g. `2`)

### `less`
A numeric test where the tag value must be less than a specified reference value. If either the reference value or the test value is non-numeric, the test will fail.
* `key` - the OSM tag key to be matched (e.g. `"lanes"`)
* `value` - a single numeric reference value (e.g. `2`)

### `greater-equal`
A variation on `greater` where the test also allows for the tag value and reference value to be equal.

### `less-equal`
A variation on `less` where the test also allows for the tag value and reference value to be equal.

### `and`
A test that performs an "and" comparison of multiple sub-tests. The test passes only if *all* of the specified sub-tests pass.
* `tests` - An array of sub-tests (which may be of any type)

### `or`
A test that performs an "or" comparison of multiple sub-tests. The test passes if *any* of the specified sub-tests pass.
* `tests` - An array of sub-tests (which may be of any type)

## Example

Following is an example of a more advanced rule involving a combination of `and`, `or`, `equal`, and `greater` tests. This rule tests whether a street segment is either a `primary` street *or* a `secondary`/`tertiary` street with more than two lanes, and applies a factor of 1.5 if so.

```JSON
// walk-config.json
{"rules": [
  {
    "type": "or",
    "tests": [
      {
        "type": "equal",
        "key": "highway",
        "value": "primary"
      },
      {
        "type": "and",
        "tests": [
          {
            "type": "equal",
            "key": "highway",
            "value": ["secondary", "tertiary"]
          },
          {
            "type": "greater",
            "key": "lanes",
            "value": 2
          }
        ]
      }
    ],
    "factor": 1.5
  }
]}
```

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

Invoking this endpoint will reload the `walk-config.json` file (which must be located in the current graph's home directory) and will use the freshly loaded rules to recalculate the walk comfort factors for all street edges in the graph. A query to this endpoint will return a simple JSON response indicating whether the query was successful, the time in milliseconds taken by the query, and the number of edges updated and rules loaded for each router active on the OTP server. Following is an example of such a response:

```JSON
{
  "success": true,
  "time": 6,
  "routers": [
    {
      "routerId": "pdx",
      "edgesUpdated": 3956,
      "rulesLoaded": 1
    }
  ]
}
```

Any subsequent routing requests will be performed on the updated graph, but note that the `Graph.obj` file is *not* rewritten on disk; to incorporate any updated walk rules into the graph file it must be rebuilt using the graph builder.
