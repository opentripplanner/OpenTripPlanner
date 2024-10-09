# Summary

This adds a component to specify a very simple schema for a token. The schema is a list of 
versioned token definitions and the encode/decode methods enforce versioning, and forward and 
backward compatibility. It also allows definitions to be merged. We only need to support one OTP
version. So after every release of OTP we can merge the definitions older than the last release.

# Issue
[Issue Create a reusable token generator #5451](https://github.com/opentripplanner/OpenTripPlanner/issues/5451)

# Example

## Define Schema
```Java
// v1:  (mode: byte)
var builder = TokenSchema.ofVersion(1).addByte("mode");
    
// v2:  (mode: byte, searchWindow : Duration, numOfItineraries : int)
builder.newVersion().addDuration("searchWindow").addInt("numOfItineraries");
    
// v3:  (mode: byte, @deprecated searchWindow : Duration, numOfItineraries : int, name : String)
builder = builder.newVersion().deprecate("searchWindow").addString("name");
    
// v4:  (@deprecated mode: byte, numOfItineraries : int, name : String, dateTime : Instant)
builder = builder.newVersion().deprecate("mode").addTimeInstant("dateTime");

var schema = builder.build();
```

## Merge Schema

The merging is provided to simplify the code when old versions of the schema is no longer needed.
For example after releasing OTP `v2.5`, everything older than `v2.4` can be merged - we only 
support backwards compatibility with the previous version. 

```Java
// v3  - v1, v2 and v3 merged
var builder = TokenSchema
    .ofVersion(3)
    .addByte("mode")
    .addInt("numOfItineraries")
    .addString("name");
```

## Encode token

Create a new token with latest version/definition(v4). Deprecated fields need to be inserted to 
be forward compatible.

```Java
var token = schema.encode()
    .withInt("numOfItineraries", 4)
    .withByte("mode", BUS_CODE)
    .withTimeInstant("dateTime", Instant.now())
    .withString("name", "Oslo - Bergen")
    .build();
```

## Decode token

The token returned is parsed using the schema version that was used to generate the token. When
acting on this, a mapping into the existing code must be provided for each supported version. If an
old version can not be supported anymore, then merging the Schema is an option. 

```Java
var token = schema.decode("rO0ABXcaAAIxMwAUMjAyMy0xMC0yM1QxMDowMDo1OVo=");

if(token.version() == 1) { token.getByte("mode") ... }
if(token.version() == 2) { ... }
if(token.version() == 3) { ... }
```
