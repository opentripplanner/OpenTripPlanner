![OTP Logo](images/otp-logo.svg)
# OpenTripPlanner 2

**OpenTripPlanner** (OTP) is an open source multi-modal trip planner, focusing on travel by scheduled public transportation in combination with bicycling, walking, and mobility services including bike share and ride hailing. Its server component runs on any platform with a Java virtual machine (including Linux, Mac, and Windows). It exposes APIs that can be accessed by various clients including open source Javascript components and native mobile applications. OTP is released under the [LGPL license](https://opensource.org/licenses/LGPL-3.0). As of 2020, the codebase has been in active development for over years, and is relied upon by transportation authorities and travel planning applications in [deployments](Deployments) around the world.

You are currently reading the documentation for **OpenTripPlanner 2**, the second major version of OTP, which is still under development but approaching release. As of 17 September 2020 OpenTripPlanner 2 is in feature freeze, and we have published a release candidate v2.0-RC1 which is now undergoing testing.

The most recent stable release of OTP is **OpenTripPlanner 1.4**. If you do not want to test the cutting edge system, please switch to that documentation set.

# Audience

The end users of OTP are the millions of people who rely on it to help plan their daily travel, often without even knowing they are using OTP. As an infrastructure component, installation and configuration of OTP tends to be somewhat technical and ideally invisible to those end users. This documentation is indended for people who wish to perform such deployments of OTP, but without necessarily diving into the internal details of the software.

For members of the OTP community interested in software development, additional documentation detailing algorithms, data structures etc. is available within the source repository. It may be browsed via Github by navigating to the directory `src/x`. 
 
## Versions of this documentation

Several versions of this documentation are built and published automatically for different branches of OTP. Each of these has a different stable URL, and you may switch between these versions using the selector in the lower right of the published documentation.

 - [Latest](http://docs.opentripplanner.org/en/latest) - Latest stable release from master branch
 - [dev-1.x](http://docs.opentripplanner.org/en/dev-1.x) - OTP 1 active development 
 - [dev-2.x](http://docs.opentripplanner.org/en/dev-2.x) - OTP 2 active development
 - [2.0-rc](http://docs.opentripplanner.org/en/2.0-rc) - OTP 2 release candidate undergoing testing

# Quick Start
We encourage you to read the introductory sections of this documentation to first familiarize yourself with OpenTripPlanner use cases and configuration. But if you want to get started right away running your own OTP instance, the best place to start is the [Basic Usage](Basic-Usage) page.

## Contact Info

Send questions and comments to the [user mailing list](http://groups.google.com/group/opentripplanner-users).
Discuss internal development details on the [dev mailing list](http://groups.google.com/group/opentripplanner-dev).
File bug reports via the Github [issue tracker](https://github.com/openplans/OpenTripPlanner/issues). Note that the issue tracker is not intended for support questions or discussions. Please post them to one of the mailing lists instead.

## Financial and In-Kind Support

OpenTripPlanner is a member project of Software Freedom Conservancy, a 501(c)(3) organization incorporated in New York, and donations made to it are fully tax-deductible to the extent permitted by law. Donations can be made by credit card, wire transfer or paper check. Please contact <accounting@sfconservancy.org> for instructions.

OTP development is primarily carried out by full-time software engineers employed by transportation authorities and consultancies. Even with funding, it can be difficult to engage staff who have the specialized skill set required. Therefore, one of the best ways to support OTP is to allocate software development staff at your organization with transportation domain knowledge to participate in weekly development meetings and contribute to this effort. This also builds connections between organizations favoring open source collaboration.