# Product Overview

## OpenTripPlanner project

OpenTripPlanner is a group of open source software applications that help individuals and organizations 
calculate and deliver multimodal trip plans based on OpenStreetMap (OSM) and other standardized data 
sources (e.g. GTFS, GBFS, NeTEx).

A community of dozens of individuals and organizations work on OpenTripPlanner collaboratively to 
improve multimodal trip planning best practices and to make it easier for public transit agencies and 
public transit riders to publish and access information about transit services.

OpenTripPlanner deployments are locally managed in many different ways by many different types of organizations. 
OpenTripPlanner consistently and dependably delivers multimodal trip plans to millions of riders 
everyday in dozens of countries around the globe. The project is actively maintained by the community, 
with more than 50 commits most weeks during 2022, and 20 different developers having made 50 or more 
commits during the life of the project.

## What exactly is OTP?

The most commonly used OpenTripPlanner component is a server-side Java application that is primarily 
designed to interpret and combine map, transit service, and other mobility data sets, providing API 
endpoints that receive user origins and destinations and return trip plans to other applications 
along with vector map tiles, stop departures, and other rider information.

In other words, OTP is a backend application that can work with other frontend user interfaces. OTP 
works with your website, mobile app, physical signage or other applications in order to provide 
relevant customer information and meaningful trip plans to riders.

There's no official OTP user interface, although the OpenTripPlanner ecosystem includes several user 
interface projects (for example [Digitransit](https://github.com/HSLdevcom/digitransit-ui) and 
[OTP-react-redux](https://github.com/opentripplanner/otp-react-redux)). 
How your OTP deployment looks is entirely up to you. 

## What OTP can do for you

### Transit agencies

Transit agencies can use OTP as a backend for their public website/app trip planner or internal trip planner 
for their customer service team.

- Fully customizable for use with any frontend
- High-performance trip planning
- Fully multimodal routing, including walking, biking, bikeshare, demand-responsive services in 
combined trip plans rather than presented in alternative trip plans
- Support for real-time alerts, trip re-routing based on real-time delays and vehicle positions
- Multi-criteria cost calculation allows for dynamic user inputs and requests including 
mode, time, transfers, and other preferences while delivering balanced results
- One OTP backend can power both a website and app for use by riders, along with a separate frontend 
with a different configuration for use by a customer service team
- Active development by dozens of agencies means new features every year [(see the OTP common roadmap here)](https://github.com/orgs/opentripplanner/projects/3)

### Regions, countries, and private entities

Regional, provincial, or national governments, or any private entity, can set up a trip planner including multiple transit agencies.

- Highly scalable software for large regions
- Interagency fare calculation
- Integrates multiple modes including bikeshare and demand-response
- Can be provided as a service to agencies for use in their websites and apps

### Researchers

While historically, OpenTripPlanner has often been used by researchers for the programmatic calculation 
of large numbers of trip plans, this is no longer an intended use case of OTP. You are welcome to use 
OTP for any purpose, but you may also find applications like [r5](https://github.com/conveyal/r5) to 
be more appropriate for these purposes.

## Talk to an expert about OTP

Everyone interested in OTP is welcome to post questions on the [Gitter chat](https://gitter.im/opentripplanner/OpenTripPlanner)
or [OpenTripPlanner-users group](https://groups.google.com/g/opentripplanner-users).

If you’re looking for a conversation with an individual from a similar organizational type, you can 
make that request and include some info about your organization on the forum linked above and a member 
of the OTP community will connect with you.