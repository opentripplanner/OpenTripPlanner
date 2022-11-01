# OpenTripPlanner Project History

## OpenTripPlanner 1

OpenTripPlanner was seeded by Portland, Oregon's transit agency [TriMet](http://trimet.org/) with
a [Regional Travel Options grant](http://www.oregonmetro.gov/tools-partners/grants-and-resources/travel-options-grants)
and opened with
a [3-day Kick-Off Workshop](https://github.com/opentripplanner/OpenTripPlanner/wiki/kick-off-workshop)
in July of 2009 bringing together transit agencies and the authors of the major open source transit
passenger information software of the day: David Emory of FivePoints, Brian Ferris
of [OneBusAway](https://github.com/OneBusAway/onebusaway/wiki), and Brandon Martin-Anderson
of [Graphserver](http://graphserver.github.io/graphserver/). From 2009 through 2012, development was
coordinated by New York nonprofit [OpenPlans](http://openplans.org/). In 2011 a second workshop was
held to mark the end of the first phase of development. TriMet's
2009-2011 [OTP Final Report](https://raw.githubusercontent.com/wiki/opentripplanner/OpenTripPlanner/History/2011-07-OTP-Workshop/OTP%202009-2011%20RTO%20Grant%20Final%20Report.pdf)
summarizes progress at that point.

The project has since grown to encompass a global community of users and developers. By early 2013,
OpenTripPlanner had become the primary trip planning software used by TriMet in
the [Portland regional trip planner](http://ride.trimet.org/) and was backing several popular mobile
applications. Public-facing OpenTripPlanner instances were available in at least ten countries
throughout the world. At this point the OpenPlans transportation software team became the
independent consultancy [Conveyal](http://www.conveyal.com/). The original OpenTripPlanner
development team from OpenPlans still actively participates in programming, design, and community
coordination via the mailing list and their roles on the
OTP [Project Leadership Committee](Governance.md).

In summer of 2013, the OpenTripPlanner project was accepted for membership in
the [Software Freedom Conservancy (SFC)](http://sfconservancy.org/). SFC handles the legal and
financial details common to many open source projects.

In 2013-2014 OpenTripPlanner was a focal point in the Dutch Transport Ministry's Beter Benutten
Multimodale Reisinformatie (Better Utilization: Multimodal Travel Information) project which
encouraged investment in trip planning platforms and
services. [Five companies worked together](https://www.ovmagazine.nl/nieuws/vijf-nieuwe-actuele-ov-routeplanners-zijn-af)
to improve OpenTripPlanner performance in large regional transport networks and add support for
streaming real-time data, making itineraries reflect service modifications and delays only seconds
after vehicles report their positions. Another consortium embarked on a full rewrite of the trip
planning core called [RRRR (or R4)](https://github.com/bliksemlabs/rrrr), a proof of concept
validating extremely efficient routing techniques and serving as an early prototype for OTP2.

In the fall of 2014, Arlington, Virginia launched a new commute planning site for the Washington, DC
metropolitan area, depending on OpenTripPlanner to weigh the costs and benefits of various travel
options. In 2015 the New York State department of transportation's 511 transit trip planner began
using OTP to provide itineraries for public transit systems throughout the state from a single
unified OTP instance. Starting in early 2016, the regional transport authorities of Helsinki,
Finland (HSL) and Oslo, Norway (Ruter) began using a completely open source passenger information
system based on OpenTripPlanner. National-scale OpenTripPlanner instances were also created in
Finland and Norway.

After seven years of hard work and almost 10,000 commits from over 100 contributors around the
world, OTP version 1.0 was released on 9 September 2016.

## OpenTripPlanner 2

The OTP community has a long history with round-based routing algorithms. FivePoints, one of the
predecessor projects to OTP, used a round-based method several years before the now-familiar Raptor
algorithm was published
in [an influential paper](https://www.microsoft.com/en-us/research/wp-content/uploads/2012/01/raptor_alenex.pdf)
. OpenPlans carried out experiments with routing innovations like Raptor and contraction hierarchies
as they emerged in the academic literature. Research and development work on OTP scalability has
focused on round-based tabular approaches since the MMRI pre-commercial procurement projects of
2013-2014. Conveyal built its high-performance transportation network analysis system around
its [R5 router](https://github.com/conveyal/r5). So in strategy discussions, the expected technical
direction was clear.

In the second quarter of 2018, Ruter and Entur took the lead on finally integrating a new
round-based transit routing engine inspired by R5 into OTP. They also began adding support for
importing EU-standard Netex data, making it possible for passenger information services in Europe to
achieve regulatory compliance with a fully open source software stack. In June 2018, at the first
OTP international summit hosted by Cambridge Systematics in Boston, the project leadership committee
officially approved this roadmap toward OTP2.

In April of 2019, the second OTP international summit was hosted by Entur in Oslo. Encouraged by the
crowd of participants from across the Nordic countries and North America, work on OTP2 continued
unabated through 2019, 2020, and 2021 with twice-weekly videoconferences bringing together software
developers from across the world. Videos of the
full [April 2019 OTP summit](https://www.youtube.com/watch?v=QZdpP73zuX0) and
the [October 2019 OTP webinar](https://www.youtube.com/watch?v=_2d68s_U4Tc) are available.

OTP2 went into feature freeze in September 2020, and the 2.0 release occurred at the end of November
2020. OTP2 is now seeing production use for a subset of requests in several national-scale trip
planners. The project leadership committee is exploring the creation of an OTP1 working group to
ensure follow-up maintenance of the final version of OTP1.
