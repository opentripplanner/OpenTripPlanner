## Changelog

### version 1.1.0 - git release/tag "otp-1.1.0" from OpenTripPlanner

### version 1.1.0_v1
- [PROD-457](https://teralyticsag.atlassian.net/browse/PROD-457): substituting HashBiMap in PatternInterlineDwell to DualHashBidiMap from commons collections (there are no kryo serializers for HashBiMap which creates some issues serializing the graph)
- [PROD-457]((https://teralyticsag.atlassian.net/browse/PROD-457)): adding initEdges method to Vertex which helps reconstruct the graph after kryo deserialization
- [PROD-457]((https://teralyticsag.atlassian.net/browse/PROD-457)): removing both logback loggers in Router (clashes with log4j forced by spark)
- [PROD-457]((https://teralyticsag.atlassian.net/browse/PROD-457)): exposing vertex map from Graph for graph reconstruction after deserialization
- [PROD-519]((https://teralyticsag.atlassian.net/browse/PROD-519)): adding support to SimpleStreetSplitter to set max search radius, exposing splitter from street index

### version 1.2.0-v1
- updating to otp-1.2.0

### version 1.2.0-v2
- [PROD-651](https://teralyticsag.atlassian.net/browse/PROD-651): adding capability to produce simple car alternatives and expose some parameters to RoutingRequest