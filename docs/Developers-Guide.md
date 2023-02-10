# Developers Guide

## Quick setup

_A Quick guide to setting up the OpenTripPlanner project._

You need Git, Maven and Java(JDK) and an IDE installed on your computer. Your IDE might have JDK and
Maven embedded, if so you may skip step 3.

1. Clone OpenTripPlanner from GitHub.
2. Checkout the desired branch `git checkout dev-2.x`
3. Run `mvn package`- this will download all dependencies, build the project and run tests.
4. Open the project in your IDE.

## Working on OTP in an IDE

Most people writing or modifying OTP code use an Integrated Development Environment (IDE). Some of
the most popular IDEs for Java development are [IntelliJ IDEA](https://www.jetbrains.com/idea/),
[Eclipse](http://eclipse.org), and [NetBeans](https://netbeans.org). All three of these environments
are good for working on OTP. IntelliJ is used by most OTP developers, and the only IDE we support
with a code style formatter. You may choose another IDE, but Maven and Git integration is a plus
since OTP is under Git version control and build with Maven.

Many of the Core OTP developers use IntelliJ IDEA. It is an excellent IDE, and in my experience is
quicker and more stable than the competition. IntelliJ IDEA is a commercial product, but there is an
open source "community edition" that is completely sufficient for working on OTP.

Rather than using the version control support in my IDE, I usually find it more straightforward to
clone the OTP GitHub repository manually (on the command line or using some other Git interface
tool), then import the resulting local OTP repository into my IDE as a Maven project. The IDE should
then take care of fetching all the libraries OTP depends on, based on the Maven project
description (POM file) in the base of the OTP repository. This step can take a long time because it
involves downloading a lot of JAR files.

When running your local copy of the OTP source within an IDE, all command line switches and
configuration options will be identical to the ones used when running the OTP JAR from the command
line (as described in the [OpenTripPlanner Basic Tutorial](Basic-Tutorial.md) and
[configuration reference](Configuration.md)). The only difference is that you need to manually
specify the main class. When you run a JAR from the command line, the JVM automatically knows which
class contains the entry point into the program (the `main` function), but in IDEs you must create
a "run configuration".

Both IntelliJ and Eclipse have "run" menus, from which you can select an option to edit the run
configurations. You want to create a configuration for a Java Application, specifying the main class
`org.opentripplanner.standalone.OTPMain`. Unlike on the command line, the arguments to the JVM and
to the main class you are running are specified separately. In the field for the VM options you'll
want to put your maximum memory parameter (`-Xmx2G`, or whatever limit you want to place on JVM
memory usage). The rest of the parameters to OTP itself will go in a different field with a name
like "program arguments".

## Contributing to the project

OpenTripPlanner is a community based open source project, and we welcome all who wish to contribute.
There are several ways to get involved:

* Join the [Gitter chat room](https://gitter.im/opentripplanner/OpenTripPlanner) and the 
  [user mailing list](http://groups.google.com/group/opentripplanner-users).

* Fix typos and improve the documentation within the `/docs` directory of the project (details
  below).

* [File a bug or new feature request](http://github.com/openplans/OpenTripPlanner/issues/new).

* Create pull requests citing the relevant issue.

* Join developer meetings hosted twice a week. Check the specific times
  on [this calendar](https://calendar.google.com/calendar/u/0/embed?src=ormbltvsqb6adl80ejgudt0glc@group.calendar.google.com)

### Branches and Branch Protection

As of August 2022, we work on OTP 2.x and are using a Git branching model derived from 
[Gitflow](https://nvie.com/posts/a-successful-git-branching-model/). All development will occur
on the `dev-2.x` branch. Only release commits setting the Maven artifact version to a non-snapshot
number should be pushed to the `master` branch of OTP. All other changes to master should result 
from fast-forward merges of a Github pull request from the `dev-2.x` branch. In turn, all changes 
to `dev-2.x` should result from a fast-forward merge of a Github pull request for a single feature, 
fix, or other change. These pull requests are subject to code review. We require two pull request 
approvals from developers part of the OTP Review Team. These developers act on behalf of the 
leadership committee members. The reviewers should be from two different organizations. We also 
have validation rules ensuring that the code compiles and all tests pass before pull requests can 
be merged.

The `dev-1.x` exist for patching OTP version 1.x, but with few people to do the reviews, very few
PRs are accepted. We recommend getting in touch with the community before you spend time on making 
a PR.


### Issues

If no ticket exists for the feature or bug your
code implements or fixes, you
should [create a new ticket](http://github.com/openplans/OpenTripPlanner/issues/new) prior to
checking in, or ideally even prior to your development work since this provides a place to carry out
implementation discussions (in the comments). The created issue should be referenced in a pull
request. For really minor and uncontroversial pull requests, it is ok to not create an issue.

### Unit tests using real OSM data

Sometimes it is useful to build a graph from actual OSM or GTFS data. Since building these graphs in
a test can be quite slow they will be accepted in pull requests only if they conform to certain
standards:

1. Use the smallest possible regional extract - the OSM file should not contain more than a few
   hundred ways. Use `osmium-extract` to cut down a larger OSM file into a tiny subset of it.

2. Strip out any unneeded information by using the `osmium filter-tags` as describe
   in [Preparing OSM](Preparing-OSM.md)

### Code Comments

As a matter of [policy](http://github.com/opentripplanner/OpenTripPlanner/issues/93), all new
methods, classes, and fields should include comments explaining what they are for and any other
pertinent information. For Java code, the comments should use the
[JavaDoc conventions](http://java.sun.com/j2se/javadoc/writingdoccomments). It is best to provide
comments that not only explain *what* you did but also *why you did it* while providing some
context. Please avoid including trivial Javadoc or the empty Javadoc stubs added by IDEs, such as
`@param` annotations with no description.

### Itinerary and API Snapshot Tests

To test the itinerary generation, and the API there are snapshot test which save the result of the
requests as `*.snap` JSON-like files. These are stored in git so that it is possible to compare to
the expected result when running the tests.

If the snapshots need to be recreated than running `mvn clean -Pclean-test-snapshots` will remove
the existing `*.snap` files so that the next time the tests are run the snapshots will be recreated.
The updated files may be committed after checking that the changes in the files are expected.

### Documentation

OTP documentation is included directly in the OpenTripPlanner repository. This allows version
control to be applied to documentation as well as program source code. All pull requests that change
how OTP is used or configured should include changes to the documentation alongside code
modifications.

The documentation files are in Markdown format and are in the `/docs` directory under the root of
the project. On every push to the master branch the documentation will be rebuilt and deployed as
static pages to our subdomain of [ReadTheDocs](http://opentripplanner.readthedocs.org/). MkDocs is a
Python program and should run on any major platform. See http://www.mkdocs.org/ for information on
how to install it and how to generate a live local preview of the documentation while you're working
on writing it.

In short:

```
$ pip install mkdocs
$ mkdocs serve
```

The OTP REST API documentation is available online in the format of:

http://dev.opentripplanner.org/apidoc/x.x.x/index.html

For example, for v2.2.0:

http://dev.opentripplanner.org/apidoc/2.2.0/index.html

### Debug layers

Adding new renderer is very easy. You just need to create new class (preferably in
`org.opentripplanner.inspector` package) which implements EdgeVertexRenderer. It is best if class
name ends with Rendered. To implement this interface you need to write three functions `renderEdge`,
`renderVertex` and `getName`. Both render functions accepts `EdgeVisualAttributes` object in which
label of edge/vertex and color can be set. And both return `true` if edge/vertex should be rendered
and `false` otherwise. `getName` function should return short descriptive name of the class and will
be shown in layer chooser.

For examples how to write renderers you can look into example renderers which are all
in `org.opentripplanner.inspector` package.

After your class is written you only need to add it to TileRenderManager:

```java
//This is how Wheelchair renderer is added
renderers.put("wheelchair", new EdgeVertexTileRenderer(new WheelchairEdgeRenderer()));
```

`wheelchair` is internal layer key and should consist of a-zA-Z and -.

By default all the tiles have cache headers to cache them for one hour. This can become problematic
if you are changing renderers a lot. To disable this change `GraphInspectorTileResource`:

```java
//This lines
CacheControl cc = new CacheControl();
cc.setMaxAge(3600);
cc.setNoCache(false);

//to this:
CacheControl cc = new CacheControl();
cc.setNoCache(true);
```

### Date format

Please use only ISO 8601 date format (YYYY-MM-DD) in documentation, comments, and throughout the
project. This avoids the ambiguity that can result from differing local interpretations of date
formats like 02/01/12.

## Code style

The OTP code style is described on a separate [style guide page](Codestyle.md).

## Code conventions and architecture

The [architecture](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/ARCHITECTURE.md) 
and [code conventions](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/CODE_CONVENTIONS.md)
are only available on GitHub, not in the project documentation. These documents contain relative 
links to code so, they are a bit easier to maintain that way. The target audience is also active 
OTP developers that have the code checked out locally.


## Continuous Integration

The OpenTripPlanner project uses
the [Github actions continuous integration system](https://github.com/opentripplanner/OpenTripPlanner/actions)
. Any time a change is pushed to the main OpenTripPlanner repository on GitHub or to an open pull
request, Github actions will
compile and test the new code, providing feedback on the stability of the build.

### Changelog workflow

The [changelog file](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Changelog.md)
is generated from the pull-request(PR) _title_ using the
[changelog workflow](https://github.com/opentripplanner/OpenTripPlanner/actions/workflows/automatic-changelog.yml)
. The workflow runs after the PR is merged, and it changes, commits and pushes the _Changelog.md_. A
secret _personal access token_ is used to bypass the "Require PR with 2 approvals" rule. To exclude
a PR from the changelog add the label `skip changelog` to the PR.

#### How-to update the CHANGELOG_TOKEN

The `CHANGELOG_TOKEN` is used by the changelog workflow. It contains a Personal Access Token. The
token must be generated by a Repository Owner and have the following rights (_Settings / Developer
settings / Personal access tokens_):

![Skjermbilde 2021-11-18 kl  12 08 27](https://user-images.githubusercontent.com/5525340/142404549-c0bf2aba-608b-4feb-a114-6e4992c57073.png)

## Release Process

New releases can be found on [GitHub](https://github.com/opentripplanner/OpenTripPlanner/releases). 
Releases are performed off the master branch, and are tagged with git annotated tags.

OpenTripPlanner is currently configured such that builds including releases upload JAR files to
GitHub Packages. This is not the most convenient place for end users to find and download the files.
Therefore we also attach a stand-alone "shaded" JAR to the GitHub tag/release page, and have
historically also uploaded Maven artifacts to Maven Central including compiled and source code JARs
as well as the "shaded" JAR containing all dependencies, allowing stand-alone usage. This release
process is handled by the Sonatype Nexus Staging plugin, which is no longer configured in the
OpenTripPlanner POM. This step currently requires making a few significant manual modifications to
the POM.

We no longer trigger deployment of artifacts to Maven Central or deployment of REST API
documentation to AWS automatically in our build scripts (GitHub Actions). These steps are prone to
failure and require storing a lot of infrequently used secret information in the repo and
environment variables on GitHub. Our releases are currently not very frequent so we just carry out
these steps manually by following the checklist. We aim to make a release every 6 months.

Use the [Release Checklist](ReleaseChecklist.md) to perform the release.
