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

### version 1.2.0-v3
- [PROD-1358](https://teralyticsag.atlassian.net/browse/PROD-1358):
    - adding support to UI to add and drag intermediate points and propagate them as waypoints to RoutingRequest
    - adding WaypointEuclideanHeuristic for CAR mode as initially implemented by Alessandro in edge repository
    - default alpha distance 500 meters and heuristic importance weight 2.0 (allows constructing paths on major highways)

### version 1.2.0-v4
- [PROD-1358](https://teralyticsag.atlassian.net/browse/PROD-1358):
    - adding sampling capability to waypoints heuristic

### version 1.2.0-v5
- [PROD-1358](https://teralyticsag.atlassian.net/browse/PROD-1358):
    - adding support for transit mode for waypoints heuristic

### version 1.2.0-v7
- [SWE-241](https://teralyticsag.atlassian.net/browse/SWE-241):
    - adding additional conditions for access=customer to be flagged as thru traffic disallowed, needs to be a non-toll road