# OpenTripPlanner Deployments Worldwide

## Official Production

The following are known deployments of OTP in a government- or agency-sponsored production capacity:

* **Finland (nationwide)** The [Helsinki Regional Transport Authority](https://www.reittiopas.fi/), the [Finnish Transport Agency](https://opas.matka.fi/), and other [Finnish cities](https://waltti.fi/?lang=en) have collaborated to create [Digitransit](https://digitransit.fi/en/), providing OTP-based trip planners, APIs, open data, Docker containers and open source code. Each member organisation runs its own instance of a shared codebase and deployment environment. Their source code is available [on Github](https://github.com/HSLdevcom/), including a [new custom UI](https://github.com/HSLdevcom/digitransit-ui). This system also has a strong real-time component.
* **Norway (nationwide)** Ruter provides a [journey planner for the Oslo region](https://ruter.no/). It has been in production since January 2016 and serves around 200,000 users per day. Since November 2017, Entur has also prodvided a [nation-wide journey planner](https://en-tur.no/) which consumes schedule data in the EU standard NeTEx format with SIRI realtime updates.
* **Portland, Oregon** TriMet is the agency that originally started the OpenTripPlanner project. Their [Regional Trip Planner](http://ride.trimet.org) is based on OTP and provides about 40,000 trip plans on a typical weekday.
* **New York State** The State Department of Transportation's [transit trip planner](https://511ny.org/#TransitRegion-1) provides itineraries for public transit systems throughout the state in a single unified OTP instance.
* **Los Angeles, California** The new [metro.net trip planner](https://www.metro.net/).
* **Atlanta, Georgia** The Metropolitan Atlanta Rapid Transit Authority's (MARTA) [trip planner](http://itsmarta.com/planatrip.aspx) and the Atlanta region's transit information hub [atltransit.org](https://atltransit.org/) both use OTP to power their website trip planners.
* **Boston, Massachusetts** The [Massachusetts Bay Transportation Authority trip planner](https://www.mbta.com/trip-planner).
* **Seattle, Washington** The [Sound Transit Trip Planner](https://www.soundtransit.org/tripplanner) is based on OTP. OTP also powers the trip planning feature of the [OneBusAway native apps](http://onebusaway.org/) in the Puget Sound region.  Technical details are [here](https://github.com/OneBusAway/onebusaway-android/blob/master/SYSTEM_ARCHITECTURE.md#add-trip-planning-andor-bike-share-optional).
* **Tampa, Florida** Hillsoborough Area Regional Transit uses an OpenTripPlanner server to power the trip planning feature of the [OneBusAway native apps](http://onebusaway.org/) in their region.  Technical details are [here](https://github.com/OneBusAway/onebusaway-android/blob/master/SYSTEM_ARCHITECTURE.md#add-trip-planning-andor-bike-share-optional).
* [**Piemonte Region, Italy**](https://map.muoversinpiemonte.it/#planner) and the [**City of Torino**](https://www.muoversiatorino.it/) built on OpenTripPlanner by [5T](http://www.5t.torino.it/).
* [**Valencia, Spain**](http://www.emtvalencia.es/geoportal/?lang=en_otp) from the Municipal Transport Company of Valencia S.A.U.
* [**Grenoble, France**](http://www.metromobilite.fr/) from SMTC, Grenoble Alpes métropole, l'État Français, the Rhône-alpes region, the Isère council and the City of Grenoble.
* **Rennes, France** where the STAR network provides an OTP client for [iOS](https://itunes.apple.com/us/app/starbusmetro/id899970416?mt=8), [Android](https://play.google.com/store/apps/details?id=com.bookbeo.starbusmetro), Windows Phone et Web.
* [**Poznań, Poland**](http://ztm.poznan.pl/#planner) from Urban Transport Authority of Poznań (ZTM Poznan).
* **Trento Province, Italy** - [ViaggiaTrento](https://play.google.com/store/apps/details?id=eu.trentorise.smartcampus.viaggiatrento) and [ViaggiaRovereto](https://play.google.com/store/apps/details?id=eu.trentorise.smartcampus.viaggiarovereto)
  were implemented as part of the [SmartCampus Project](http://www.smartcampuslab.it), a research project founded by [TrentoRise](http://trentorise.eu), [UNITN](http://www.unitn.it), and [FBK](http://www.fbk.eu).
* **University of South Florida** (Tampa, Florida). The [USF Maps App](https://maps.usf.edu/) is a responsive web application for that helps university students, staff, and visitors find their way around the campus using multiple modes of transportation, including the USF Bull Runner campus shuttle, Share-A-Bull bike share, and pedestrian pathways. Open-sourced [on Github](https://github.com/CUTR-at-USF/usf-mobullity).

## Independent Production

The following OTP-based services are presented as production-quality deployments, but are not backed by an official transportation authority or government. OTP is also known to be used on the back end of several popular multi-city mobile trip planning applications.

* **The Netherlands (nationwide)** [Plannerstack Foundation](http://www.plannerstack.org/) provides national scale trip planning APIs using OTP and other open source trip planners, based on [OpenOV's extremely detailed open data](http://gtfs.openov.nl/) including minutely real-time updates for every vehicle in the country.
* [OTP Android](https://play.google.com/store/apps/details?id=edu.usf.cutr.opentripplanner.android) by CUTR-USF and Vreixo González can find itineraries on many different OTP servers via a service discovery mechanism.
* [**ViviBus Bologna**](http://www.vivibus.it/) Bologna, Italy.
