# Stop Areas

Public transit stations and stops can be modeled using a relation tagged with `public_transport=stop_area`.
Description of such relations can be found from this [OSM wiki page](https://wiki.openstreetmap.org/wiki/Tag:public_transport%3Dstop_area).

OpenTripPlanner detects such relations and applies some special logic to them.
Nodes, which are linked to street network, and are located within platform areas, are interpreted as connection points
from the street network to the platform. OTP automatically links such points with the platform geometry in order to improve
walk routing.

For example, an elevator or stairs can connect a normal street to a railway platform above it. There is
no need to add an explicit edge, which connects the entrance point with the actual geometry of the platform.

An  example: [Huopalahti railway station in Helsinki](https://www.openstreetmap.org/relation/6815620)

#### Instructions:

- Add platforms, which need special linking, as members of a `stop_area` tagged relation
- Set role=platform to these platform members
- Platforms must be also proper areas, tagged as routable highways and `area=yes`. Also a single tag `public_transport=platform` will do.
- Model required entrance points inside platform geometry and make them relation members, too. No special tagging or role is required.
- Connect entrance points to the street network
- Platform and its entrance point must have the same `level` tag value. Also matching by default value zero is accepted.
- If `level` tag is not set, `layer` tag is also considered


