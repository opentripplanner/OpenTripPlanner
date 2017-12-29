# OpenTripPlanner Deployments Worldwide

## Official Production

The following are known deployments of OTP in a government- or agency-sponsored production capacity:

* **Norway (nationwide)** Ruter provides a [journey planner for the Oslo region](https://ruter.no/). It has been in production since January 2016 and serves around 200,000 users per day. Since November 2017, Entur has also prodvided a [nation-wide journey planner](https://en-tur.no/) which consumes schedule data in the EU standard NeTEx format with SIRI realtime updates.
* **Finland (nationwide)** The [Helsinki Regional Transport Authority](https://www.reittiopas.fi/), the [Finnish Transport Agency](https://opas.matka.fi/), and other [Finnish cities](https://waltti.fi/?lang=en) have collaborated to create [Digitransit](https://digitransit.fi/en/), providing OTP-based trip planners, APIs, open data, Docker containers and open source code. Each member organisation runs its own instance of a shared codebase and deployment environment. Their source code is available [on Github](https://github.com/HSLdevcom/), including a [new custom UI](https://github.com/HSLdevcom/digitransit-ui). This system also has a strong real-time component.
* **Los Angeles, California** The new [metro.net trip planner](https://www.metro.net/).
* **Boston, Massachusetts** The [Massachusetts Bay Transportation Authority trip planner](https://www.mbta.com/trip-planner).
* **New York State** The State Department of Transportation's [transit trip planner](http://511ny.org/tripplanner/default.aspx) provides itineraries for public transit systems throughout the state in a single unified OTP instance.
* **Arlington, Virginia** The [commute planning site](http://www.carfreeatoz.com/) for the Washington, DC metropolitan area depends on OpenTripPlanner to weigh the costs and benefits of various travel options, making use of profile routing.
* **Portland, Oregon** TriMet is the agency that originally started the OpenTripPlanner project, and their [Regional Trip Planner](http://ride.trimet.org) is based on it.
* [**Valencia, Spain**](http://www.emtvalencia.es/geoportal/?lang=en_otp) from the Municipal Transport Company of Valencia S.A.U.
* [**Grenoble, France**](http://www.metromobilite.fr/) from SMTC, Grenoble Alpes métropole, l'État Français, the Rhône-alpes region, the Isère council and the City of Grenoble.
* **Rennes, France** where the STAR network provides an OTP client for [iOS](https://itunes.apple.com/us/app/starbusmetro/id899970416?mt=8), [Android](https://play.google.com/store/apps/details?id=com.bookbeo.starbusmetro), Windows Phone et Web.
* [**Poznań, Poland**](http://ztm.poznan.pl/planer) from Urban Transport Authority of Poznań (ZTM Poznan).
* [**Lublin, Poland**](http://lublin.iplaner.pl) from ZTM Lublin.
* [**Adelaide, Australia**](http://jp.adelaidemetro.com.au/opentripplanner-webapp/) the Adelaide Metro Journey Planner.
* **Trento Province, Italy** - [ViaggiaTrento](https://play.google.com/store/apps/details?id=eu.trentorise.smartcampus.viaggiatrento) and [ViaggiaRovereto](https://play.google.com/store/apps/details?id=eu.trentorise.smartcampus.viaggiarovereto)
  were implemented as part of the [SmartCampus Project](http://www.smartcampuslab.it), a research project founded by [TrentoRise](http://trentorise.eu), [UNITN](http://www.unitn.it), and [FBK](http://www.fbk.eu).
* **University of South Florida** (Tampa, Florida). The [USF Maps App](https://maps.usf.edu/) is a responsive web application for that helps university students, staff, and visitors find their way around the campus using multiple modes of transportation, including the USF Bull Runner campus shuttle, Share-A-Bull bike share, and pedestrian pathways. Open-sourced [on Github](https://github.com/CUTR-at-USF/usf-mobullity).

## Independent Production

The following OTP-based services are presented as production-quality deployments, but are not backed by an official transportation authority or government. OTP is also known to be used on the back end of several popular multi-city mobile trip planning applications.

* **The Netherlands (nationwide)** [Plannerstack Foundation](http://www.plannerstack.org/) provides national scale trip planning APIs using OTP and other open source trip planners, based on [OpenOV's extremely detailed open data](http://gtfs.openov.nl/) including minutely real-time updates for every vehicle in the country.
* [**ViviBus Bologna**](http://www.vivibus.it/) Bologna, Italy.
* [**Singapore Nextride**](https://itunes.apple.com/us/app/nextride-singapore-public/id565103559) from [buUuk](http://www.buuuk.com/) is an iOS application using OTP on the back end.
* [**BJCTA**](http://www.bjctatripplanner.org) Birmingham-Jefferson Country, Alabama.
* [**Tel Aviv, Israel**](http://www.tranzmate.co.il) from TranzMate.
* [OTP Android](https://play.google.com/store/apps/details?id=edu.usf.cutr.opentripplanner.android) by CUTR-USF and Vreixo González can find itineraries on many different OTP servers via a service discovery mechanism.

## Prototypes, technical previews, and demos

The following OTP-based services are demonstrations or prototypes. Caveats/disclaimers may apply regarding their use for actual on-the-ground trip planning; consult individual sites for details.

* [**Marseille Métropole**](http://62.210.125.178/demo/master/marseille/) Experimental client-side isochrone rendering and accessibility calculations.
* [**Tampa, Florida, USA**](http://opentripplanner.usf.edu/) University of South Florida's (USF) Center for Urban Transportation Research (CUTR) demo. Implemented as part of a [research project](http://www.locationaware.usf.edu/ongoing-research/open-transit-data/) under the Florida Department of Transportation and National Center for Transit Research. [Enabling Cost-Effective Multimodal Trip Planners through Open Transit Data](http://www.nctr.usf.edu/2011/05/enabling-cost-effective-multimodal-trip-planners-through-open-transit-data-2/).
* [**TransportesPúblicos.pt**](http://transportespublicos.pt) Portugal.
* [**A Coruña, Spain**](http://galadriel.dc.fi.udc.es:8080/opentripplanner-webapp/) University of A Coruña.
* [**Budapest, Hungary**](http://otp.flaktack.net) by [flaktack](https://github.com/flaktack).
* [**Athens, Greece**](http://zee.gr/bus/) Athens Public Transport Planner ([description](http://entropy.disconnect.me.uk/2012/01/athens-public-transport-planner.html))
* [**South Africa**](http://app.fromA2B.co.za) fromA2B Gauteng Public Transport Planner.
* [**London, UK**](http://london.optitrans.net/) from Opti-Trans.
* [**Canberra, Austrailia**](http://bus.lambdacomplex.org/tripPlanner.php) Action Busses, etc.
