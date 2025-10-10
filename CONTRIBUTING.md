# OpenTripPlanner Contributing Guide

Thank you for your interest in contributing to OpenTripPlanner.
Please read the guidelines carefully to make sure you follow our contribution process.

## Communication

- If you have any questions about problems you are encountering with code, deployment,
documentation, or development coordination, please don't hesitate to post to the
[OpenTripPlanner Gitter chat](https://gitter.im/opentripplanner/OpenTripPlanner).
- There are twice-weekly developer meetings to discuss and coordinate contributions in more detail.
See the [calendar](https://calendar.google.com/calendar/u/0/embed?src=ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com)
or [iCal link](https://calendar.google.com/calendar/ical/ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com/public/basic.ics)
for our schedule. A Google Meet link can be found in the calendar events.
- Before starting work on a major feature, please reach out to the community beforehand
to make sure the contribution makes sense and no one else is working on it.
- Document and plan features in issues.
Issues will not be discussed by the community automatically.
If you wish to discuss an issue in the developer meetings,
add it to a meeting's agenda thread in Gitter.
- Small bug fixes and patches do not require prior discussion in an issue.
- There is also a less used Google Group mailing list. It can be accessed as
[a web forum](https://groups.google.com/g/opentripplanner-users)
or as a mailing list: <opentripplanner-users@googlegroups.com>.

## Developer Meetings

- OTP development meetings usually occur twice a week.
- The meeting times have been deliberately chosen to allow participation during regular business
hours across as many time zones as possible, from the eastern Americas through Europe and Africa
to Asia. If these times are not suitable for you, please let us know and we will attempt to
schedule a call at a time that suits you.
- The meetings are open to anyone who wants to join, even if you simply want to observe the
process or ask a few questions.
- By default, pull requests will only be advanced by an author or someone relevant to the PR
participating in a meeting.
- If you are unable to participate and want your PR to be advanced, leave a comment on the PR
explaining the current state
e.g. "This PR doesn't have things to discuss and should be assigned reviewers."
- Check the specific times on
[this calendar](https://calendar.google.com/calendar/u/0/embed?src=ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com)
which will be updated to reflect holidays or changes to meeting times. Note that times on this
calendar are expressed in the Central European time zone by default. There is also
an [iCal link to import this calendar](https://calendar.google.com/calendar/ical/ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com/public/basic.ics)
into calendar apps. Check the details of the calendar events for the Google Meet link, which is
different on different days of the week.

## Code Conventions and Architecture

### Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md)

### Coding Style

See [Codestyle.md](doc/dev/decisionrecords/Codestyle.md)

### Naming Conventions

See [NamingConventions.md](doc/dev/decisionrecords/NamingConventions.md)

### Development Process Conventions

See [DevelopmentProcessConventions.md](doc/dev/decisionrecords/DevelopmentProcessConventions.md)

### Development Decision Records

See [DEVELOPMENT_DECISION_RECORDS.md](DEVELOPMENT_DECISION_RECORDS.md)

## Contributing Issues and Pull Requests

### Short Version

- When creating
issues and pull requests, please follow the instructions in the template.
- Do not leave remnants of
the instructions in your submitted text.
- Follow the style guidelines and naming conventions.
- Please discuss your proposed changes in advance before proceeding too far with
  development. There are often other plans to address the same problem, or at least other points of
  view and ideas. Coordinating with other OTP developers can avoid a lot of duplicated effort,
  conflict, and technical debt and make the review process a lot easier.
- Describe in detail the problem you are solving and any major design choices you have made. PR
  descriptions must clearly state how your changes work.
- Break large changes down into a series of smaller logical steps in separate PRs.
- Tie such series of PRs together with an "epic issue" that explains the overall plan.
- Use GitHub issue references e.g. "related to #12" or "closes #13" to connect PRs and issues
together.
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


### Long Version

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

In summary, if you are interested in integrating your code into core OTP, a significant amount of the
time you invest in OTP will need to be spent on collaboration, discussion, documentation, etc. and
you will need to be available for regular meetings. You will need to take this into consideration in
your budget and timeline.

## Other Ways to Share Development Work

We don't want to discourage innovation and experimentation, and want promising new features to be
visible to other users, so we have also created a Sandbox system for fast-track review and inclusion
in mainline OTP. Most code for a Sandbox feature must be located in its own package, and any code
added to the core of OTP must be in conditional blocks controlled by a feature flag and disabled by
default. This greatly simplifies and accelerates the review process, as requirements on the code
outside core OTP will be much less stringent. Please
see http://docs.opentripplanner.org/en/latest/SandboxExtension/ for more details.

If you don't have the time to participate in this process, the community is of course still
interested in the new ideas and capabilities in your fork of OTP. Please do share them to
create awareness of your work and attract collaborators with
more resources. The goal of mainline OTP is not to be everything to everyone, but rather to contain
the most solid code relied upon daily by the primary OTP contributor organizations (as well as
Sandbox features that have been cleanly isolated from the core system). So in a sense it's
encouraged for people to work on special-purpose forks.

## Rejecting a PR - Standard Response

For maintainers: Use this template when closing issues or pull requests that don't follow our
contribution guidelines. Copy and paste as needed, adjusting the text if appropriate.

```
Hello!

Thank you for your interest in contributing to OpenTripPlanner. Unfortunately we don't have the 
resources to review changes unless they solve an actual issue, and/or follow our contribution 
guidelines. If you would like to contribute, please feel free to join us at one of our developer
meetings or talk to us on Gitter. We are happy to guide you through the process of creating a 
PR and set the right expectations. You can find the links in our 
[contribution guidelines](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/CONTRIBUTING.md).
```
