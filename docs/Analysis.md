# Travel Time Analysis

## Background

Since the beginning of the project, many OTP contributors and users have been primarily interested in research, spatial analysis, and urban planning use cases. They have prototyped many ideas within or on top of the OTP codebase, including one-to-many searches producing travel time grids, isochrones, and access-to-opportunities indicators (see Terminology Note below). This has historically been a major area of application for OpenTripPlanner and has helped popularize cumulative opportunities metrics in urban planning. For example, the University of Minnesota Accessibility Observatory used OpenTripPlanner for [Access Across America](https://www.cts.umn.edu/programs/ao/aaa). 

Although we consider these use cases quite important, most work of this kind has long since shifted to separate projects focused on urban planning and analytics. As of version 2, OTP has chosen to focus entirely on passenger information rather than analytics.

## Travel Time Analysis in OTP1

Much of the analysis code present in the v1.x legacy branch of OTP is essentially an unmaintained and unsupported early prototype for later projects, specifically [R5](https://github.com/conveyal/r5/) and the [Conveyal Analysis](https://conveyal.com/learn) system built upon it. OTP1 seems to have gained popularity for analysis uses due to the existence of documentation and an active user community, but has significant technical shortcomings. One of these is simply speed: OTP1 can be orders of magnitude slower (and more memory-intensive) than the approaches used in R5. The other is the requirement to search at a single specific time. Travel times and especially wait times on scheduled transit vary greatly depending on when you depart. Accounting for variation over a time window requires repeated independent searches at each possible departure time, which is very inefficient. R5 is highly optimized to capture variations in travel time across time windows and account for uncertainty in waiting times on frequency-based routes.

## Travel Time Analysis in OTP2

OTP2's new transit router is quite similar to R5 (indeed it was directly influenced by R5) and would not face the same technical problems. Nonetheless, we have decided not to port the OTP1 analysis features over to OTP2 since it would broaden the scope of OTP2 away from passenger information and draw the finite amount of available attention and resources away from existing open source analytics tools. If you would like to apply the routing innovations present in OTP2 in analytics situations, we recommend taking a look at projects like R5 or the R and Python language wrappers for it created by the community.


## Terminology Note

In OpenTripPlanner, we usually use the term "accessibility" with its most common meaning: design of products, devices, services, vehicles, or environments to ensure they are usable by people with disabilities. The term "accessibility" has a completely separate, unrelated definition in the fields of spatial analysis, urban transportation planning, and associated social sciences, where it refers to quantitative indicators of how well-connected a particular location is to people or opportunities. OTP has been widely used in research and planning settings for the calculation of such indicators. Although this meaning of the term dates back many decades, it is less well known and has become a source of confusion, so the academic and planning communities are gradually shifting to the expression "access to opportunities", often shortened to "access".
