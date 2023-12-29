# OpenTripPlanner Contributing Guide

Thank you for your interest in contributing to OpenTripPlanner. This document will give you some
pointers on how to contact and coordinate with the OTP development team, as well as a description of
the contribution process and expectations.

## Primary Channels of Communication

If you have any questions about problems you are encountering with code, deployment, documentation,
or development coordination, please don't hesitate to post to the [OpenTripPlanner Gitter chat](https://gitter.im/opentripplanner/OpenTripPlanner)
or the mailing list. This is the Google Group which can be accessed as web forums or as traditional 
email mailing lists:

- https://groups.google.com/g/opentripplanner-users (opentripplanner-users@googlegroups.com)

Any message posted there will be seen by most of the contributors, some of whom work on OTP full
time. It will also create a record of the discussion that will be useful to the larger community and
often leads to issues being discussed at the twice-weekly development meetings.

## Developer meetings

OTP development meetings usually occur twice a week. These meetings are open to anyone who wants to
join, even if you simply want to observe the process or ask a few questions. The most effective way
to advance pull requests and collaborate is to participate directly in these meetings. The meeting
times have been deliberately chosen to allow participation during regular business hours across as
many time zones as possible, from the eastern Americas through Europe and Africa to Asia. If these
times are not suitable for you, please let us know and we will attempt to schedule a call at a time
that suits you.

Check the specific times
on [this calendar](https://calendar.google.com/calendar/u/0/embed?src=ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com)
which will be updated to reflect holidays or changes to meeting times. Note that times on this
calendar are expressed in the Central European time zone by default. There is also
an [iCal link to import this calendar](https://calendar.google.com/calendar/ical/ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com/public/basic.ics)
into calendar apps. Check the details of the calendar events for the Google Meet link, which is
different on different days of the week.

Our primary tools for organizing development are Github issues and pull requests. When creating
issues and pull requests, please follow the instructions in the template: always specify the version
of OTP you are running and provide command lines and configuration files, do not leave remnants of
the instructions in your submitted text etc.

## Contributing Issues and Pull Requests

OpenTripPlanner has been in active development and use for well over a decade and is now relied upon
as infrastructure by large organizations and millions of public transit passengers around the world.
These organizations have invested a great deal of time, effort, and money in this project, and some
have gone all-in supporting and relying on OTP over the long term. Accordingly, when working on
OpenTripPlanner 2 we have decided to make code quality and maintainability a top priority, and have
built up a culture of active collaboration and code review to support this. At this stage, the
project has relatively high expectations on code quality and a desire to maintain a professional and
orderly development process.

We welcome well-documented bug reports as Github Issues, and pull requests sharing your patches and
new features. However, for all but the simplest bugfixes and documentation updates, please be aware
that pull requests will be subject to review and you will almost certainly be asked to explain use
cases, ensure encapsulation and integration with the rest of the system, and provide some assurance
that there is an organizational commitment to long term maintenance.

Most of the contributors to OTP2 are full-time software developers working for organizations with a
long term stake in OpenTripPlanner, and are professionally bound to ensure its reliability and
efficiency. It is an accepted fact among this team that a large part, perhaps the most important
part of software development is careful design and communication with current collaborators and
future maintainers of the system.

You will see a steady stream of pull requests being merged from different organizations. What almost
all these have in common is that their authors participated in the weekly meetings, at which they
discussed the problem or feature they were addressing, answered questions from the other developers
about their design and implementation, and were open to making changes based on the consensus
reached at those meetings. If you do not have time to participate in a meeting (or organize a
special-purpose call to review code together with other contributors), please understand up front
that contributions may stall.

Even before you start working on a contribution, please feel free to join a call and discuss how you
want to solve your problem or implement your feature. In the past, most contributions that were
undertaken without any discussion up front required major changes before acceptance.

We try to reduce the time demands on reviewers by putting more responsibilities on the PR submitter.
This does carry a risk of discouraging contributions, but without a "sponsor" organization for a
change, the time available to review is the bottleneck in the process.

In sum: if you are interested in integrating your code into core OTP, a significant amount of the
time you invest in OTP will need to be spent on collaboration, discussion, documentation, etc. and
you will need to be available for regular meetings. You will need to take this into consideration in
your budget and timeline.

## Other ways to share development work

We don't want to discourage innovation and experimentation, and want promising new features to be
visible to other users, so we have also created a Sandbox system for fast-track review and inclusion
in mainline OTP. Most code for a Sandbox feature must be located in its own package, and any code
added to the core of OTP must be in conditional blocks controlled by a feature flag and disabled by
default. This greatly simplifies and accelerates the review process, as requirements on the code
outside core OTP will be much less stringent. Please
see http://docs.opentripplanner.org/en/latest/SandboxExtension/ for more details.

If you don't have the time to participate in this process, the community is of course still
interested in the new ideas and capabilities in your fork of OTP. Please do share them on the
groups (mailing lists) where you may create awareness of your work and attract collaborators with
more resources. The goal of mainline OTP is not to be everything to everyone, but rather to contain
the most solid code relied upon daily by the primary OTP contributor organizations (as well as
Sandbox features that have been cleanly isolated from the core system). So in a sense it's
encouraged for people to work on special-purpose forks.

## Additional Considerations for Structuring Pull Requests

When creating and building on a pull request, please do the following:

- If possible, please discuss your proposed changes in advance before proceeding too far with
  development. There are often other plans to address the same problem, or at least other points of
  view and ideas. Coordinating with other OTP developers can avoid a lot of duplicated effort,
  conflict, and technical debt and make the review process a lot easier.
- Describe in detail the problem you are solving and any major design choices you have made. PR
  descriptions must clearly state how your changes work.
- Break large changes down into a series of smaller logical steps in separate PRs.
- Tie such series of PRs together with an "epic issue" that explains the overall plan.
- Use Github issue references ("addresses #12" etc.) to connect PRs and issues together.
- Consider squashing and rebasing to make the PR history easier to understand, eliminating extra "
  noise" commits like accidental changes to organization specific code, abandoned experiments, or
  reverted configuration changes.

In turn, in order to prevent OTP2 from turning into a "
big-ball-of-mud" (https://en.wikipedia.org/wiki/Big_ball_of_mud) and ensure development does not
grind to a halt, the maintainers of the project will:

- Keep an eye out for features which are not core, and suggest that they be Sandbox features.
- Ask for example use cases for new functionality to make sure OTP is the right place to implement
  it (rather than some external component).
- Make sure the implementation avoids duplication and the addition of new layers and is
  encapsulated, requesting refactoring if necessary.
- Make sure non-functional characteristics (performance and memory usage) are maintained when
  features are added.

## OTP2 versus OTP1

The vast majority of the work done by the core development team is now on OTP2 (the `dev-2.x`
branch) as opposed to OTP1 (the `dev-1.x` branch). Please
see http://docs.opentripplanner.org/en/latest/Version-Comparison/ for a discussion of the difference
between these two branches. At this point, OTP1 is essentially a legacy product that will receive
bug and stability fixes to the extent that they can be readily backported from OTP2 or that the
author of such patches can invest the effort to join a meeting and answer any questions about the
impact and design of their code.

There is a large base of existing deployments of OTP1, in both trip planning and academic research
use. OTP2 is different in many ways from OTP1, and some features that were research prototypes or
not actively maintained have been removed. We want to ensure long-term users can continue to rely on
these OTP1-specific if needed. Therefore we cannot apply changes to OTP1 which pose any significant
risk of introducing new bugs or behavior changes, as this will create additional maintenance or
documentation work for which there is no budgeted developer time.
