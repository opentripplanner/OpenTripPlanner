# Frontends (User Interfaces) for OpenTripPlanner

## Introduction

OpenTripPlanner is a client-server system. A backend service written in Java is accessed over an API by some client software, which is generally embedded in a web page or a mobile app. When OpenTripPlanner developers refer to OpenTripPlanner or OTP, they often specifically mean the Java backend service. But for this service to be useful to end users who want to plan their journeys, you will typically also want to deploy a user interface as well as a few other components providing mapping capabilities and address lookup functionality.

For the purposes of this document and in many OpenTripPlanner development discussions, the terms frontend, UI, and client will be used interchangeably. In reality many kinds of clients are possible, not all of them being user interfaces (UIs). The OpenTripPlanner API may be called by various scripts and microservices, potentially all within what would traditionally be called a "backend" with no user interface at all. For example, updates to trip plans could be requested, summarized and sent as text messages over a mobile network. But in this documentation page, we are exclusively talking about graphical user interfaces, usually written in Javascript for execution and display in web browsers.

## Two Kinds of Frontends

Broadly speaking there are two kinds of frontends for OTP: debug frontends and production frontends. 

**Debug frontends** are included in the main OpenTripPlanner repository, and are intended to work "out of the box" with little to no configuration. They are served by OTP itself or a simple local web server, or set up such that a single deployment is usable by anyone via a content delivery network (CDN). The primary purpose of debug frontends is to allow OTP developers (as well as people evaluating OpenTripPlanner for their organization) to observe, test, and debug an OTP instance they're working on. Debug frontends will expose some internal details of OTP operation such as graph nodes and edges, traversal permissions, or transit data entity identifiers. Their primary role is as a development and deployment tool.

On the other hand, **production frontends** are intended to be a component of larger public-facing deployments of OTP, with a more polished appearance and user experience. They generally do not work "out of the box", and require a significant amount of configuration and coordination with external components such as map tile servers and geocoders. They are designed to be components of a large professionally maintained and managed OTP deployment, and present a simpler view of OpenTripPlanner options and results to a non-technical audience of end users. 

## Debug Frontends

The main OpenTripPlanner repository currently contains two debug web frontends: 

- new one currently under development at [`/client`](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/client).
- the classic one in [`/application/src/client/classic-debug/`](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/application/src/client/classic-debug) 

The **new debug client** is a React/TypeScript Single Page App (SPA) that can be served locally or accessed over a content delivery network (CDN). 
Unlike the original debug client, it connects to the OTP Java backend via the GraphQL API using the Transmodel vocabulary. By default, it is available at the root URL (`http://localhost:8080/` in local operation).

The **classic debug client** is a jQuery and Backbone based UI whose history can be traced back over a decade to the first days of the OTP project. 
It connects to the OTP Java backend via a REST API using the GTFS vocabulary. Historically this was the default OTP interface available at the root URL.
It is still available, but has been moved to `http://localhost:8080/classic-debug/` .

There is a third piece of software that might qualify as an OTP client: a Java Swing application making use of the Processing visualization library, 
located in the [GraphVisualizer class](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/application/src/main/java/org/opentripplanner/visualizer/GraphVisualizer.java). 
While it would not be accurate to call this a "native" desktop application (as it's cross-platform Java) it is not a web app. This very developer-centric 
UI is also over a decade old and has been very sparsely maintained, but continues to exist because it can visualize the progress of searches through the 
street network, providing some insight into the internals of the routing algorithms that are not otherwise visible.

## Working with Debug Frontends

While the two debug frontends are enabled by default as of this writing, they may not be in the future, and you may wish to disable them if you've chosen to use a different frontend. 
Also, to get full use of the existing debug frontends you may want to enable OTP's built-in simple testing geocoder which performs fuzzy searches for 
transit stops by name, supplying their coordinates to the routing engine. Without it, you will be limited to origins and destinations selected on a map or 
specified in terms of latitude and longitude coordinates. The debug frontend and the geocoder can be toggled in `otp-config.json`:

```json5
// otp-config.json
{
  "otpFeatures": {
    "DebugUi": true,
    "SandboxAPIGeocoder": true
  }
}
```

## Production Frontends

Many different production OTP frontends exist. Any number of agencies and consultancies may have built new frontends, whether in Javascript or as native mobile apps, without the OpenTripPlanner development team even being aware of them.

That said, there are two main Javascript-based web user interfaces that are generally recommended by members of the OTP development team who also work on professional, public-facing OpenTripPlanner deployments:

- The [Digitransit UI](https://github.com/HSLdevcom/digitransit-ui), part of the Finnish Digitransit project.
- The [OpenTripPlanner React UI](https://github.com/opentripplanner/otp-react-redux), developed and maintained by [Arcadis IBI](https://www.ibigroup.com)'s [transit routing team](https://www.ibigroup.com/ibi-products/transit-routing/).

**Digitransit** is an open-source public transportation project, originally created in Finland to replace existing nationwide and regional journey planning solutions in 2014. Digitransit has since been used around the world in other projects, for example in Germany. It is a joint project of Helsinki Regional Transport Authority (HSL), Fintraffic, and Waltti Solutions.

**Arcadis IBI** has for several years actively developed, deployed, and maintained instances of the React-based OTP UI, systematically contributing their improvements back to the original repositories under the OpenTripPlanner organization: 

- [https://github.com/opentripplanner/otp-ui](https://github.com/opentripplanner/otp-ui)
- [https://github.com/opentripplanner/otp-react-redux](https://github.com/opentripplanner/otp-react-redux)

Both major frontend projects mentioned above support internationalization and have several translations already available.

## Deploying a Production Frontend

Deploying a full OpenTripPlanner-based system including a web UI can be quite complex, as there are many "moving parts". Such a system needs to remain responsive and function smoothly under load that varies wildly from one time of day to another, constantly refresh input data, and meet customer expectations in recognizing addresses and names of landmarks etc. Because deployment and management of such a system demands long term commitment by a professional team with sufficient experience and knowledge about the component parts, the major production frontend projects do not really aim to work "out of the box". There's a working assumption that they'll be actively customized for any large regional deployment.

Unfortunately this creates a bit of a barrier for organizations evaluating such components. Please don't hesitate to join the OpenTripPlanner Gitter chat and ask if you need any information on this subject. Several organizations that manage multiple large OTP deployments are active there and will be happy to engage with you.

While you are welcome to stand up an instance of one of these UIs for testing or evaluation, and there has been some effort to make this more straightforward especially on the Arcadis IBI projects, it is still strongly recommended to collaborate with a specialist. The process of tailoring a deployment to a specific region and deployment environment is estimated to take at least several full days even for an experienced professional, and while it may start off looking deceptively simple, can rapidly grow into a seemingly never-ending project for those who are not aware of the pitfalls. The goal here is not by any means to discourage you from using or learning about any of these projects, but to set expectations that the process will be much smoother if you work with someone who's done it before. Be aware that both of these systems rely on multiple additional backend services other than the OTP Java backend. To get either one working, you will need to configure a source of map tiles and a geocoder that translates geographic names to latitude and longitude coordinates, among other components.

That said, as of this writing (early 2024) it may now be possible to get a minimal, non-production test deployment of OTP-React-Redux working with only a few adjustments to the example configuration file. See the README and `example-config.yml` in [the OTP-RR repo](https://github.com/opentripplanner/otp-react-redux).

## Historical Background

The history of the more widely used OpenTripPlanner interfaces is roughly as follows:

- From the beginning, OpenTripPlanner included a Javascript UI module. This jQuery and Backbone based UI was deployed as the main interface in several locations in the early to mid 2010s.
- As of early 2024, this UI is still present in the main OpenTripPlanner repository under `src/client`, but is not intended for use by the general public. It has long been regarded as a "debug" UI, serving as a built-in, readily available and familiar interface for OTP developers to test and debug the behavior of the backend. It uses the older OTP REST API which is now removed.
- In the late 2010s people started developing a new React-based UI as a more modular, modern interface for public consumption. This project is located at https://github.com/opentripplanner/otp-react-redux under the OpenTripPlanner Github organization, and is developed and maintainted by Arcadis IBI.
- Some React components were factored out of that UI project, allowing them to be integrated in different ways with different OTP deployments. This component library is in a separate repository at https://github.com/opentripplanner/otp-ui. Likewise, it is developed and maintained by Arcadis IBI.
- Meanwhile, starting in 2014, HSL (the Helsinki Regional Transport Authority) and Finntrafic (the Finnish national transportation authority) began the Digitransit project, a set of open-source microservices to replace their existing national and regional scale trip planners. This includes a Javascript web UI module. In addition to Finland, the Digitransit system has been deployed in various places around the world including Germany.
- As of 2024, a completely new debug UI (again, intended for developer use rather than public consumption) is being developed in the main OpenTripPlanner repository under `src/client`. This new UI follows a more conventional contemporary Javascript development style, and uses the most recent OpenTripPlanner GraphQL API which has fully replaced the now-removed REST API. 
- In June 2024, the default was swapped and the new GraphQL-based one is now the default with the old one being available at `http://localhost:8080/classic-debug/`