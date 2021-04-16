# Developers Guide

## Quick setup
_A Quick guide to setting up the OpenTripPlanner project._

You need Git, Maven and Java(JDK) and an IDE installed on your computer. You IDE might have JDK and 
Maven embedded, if so you may skip step 3.

1. Clone OpenTripPlanner from GitHub.
2. Checkout the desired branch `git checkout dev-2.x` 
3. Run `maven package`- this will download all dependencies, build the project and run tests.
4. Open the project in your IDE.
5. Import the _intellij-code-style.xml_ (IntelliJ IDE).


## Working on OTP in an IDE

Most people writing or modifying OTP code use an Integrated Development Environment (IDE). Some of 
the most popular IDEs for Java development are [IntelliJ IDEA](https://www.jetbrains.com/idea/), 
[Eclipse](http://eclipse.org), and [NetBeans](https://netbeans.org). All three of these environments 
are good for working on OTP. IntelliJ is used by most OTP developers, and the only IDE we support 
with a code style formatter. You may choose another IDE, but Maven and Git integration is a 
plus since OTP is under Git version control and build with Maven.

Many of the Core OTP developers use IntelliJ IDEA. It is an excellent IDE, and in my experience is 
quicker and more stable than the competition. IntelliJ IDEA is a commercial product, but there is an 
open source "community edition" that is completely sufficient for working on OTP.

Rather than using the version control support in my IDE, I usually find it more straightforward to clone the OTP GitHub
repository manually (on the command line or using some other Git interface tool), then import the resulting local OTP
repository into my IDE as a Maven project. The IDE should then take care of fetching all the libraries OTP depends on,
based on the Maven project description (POM file) in the base of the OTP repository. This step can take a long time because
it involves downloading a lot of JAR files.

When running your local copy of the OTP source within an IDE, all command line switches and 
configuration options will be identical to the ones used when running the OTP JAR from the command
line (as described in the [OpenTripPlanner Basic Tutorial](Basic-Tutorial.md) and 
[configuration reference](Configuration.md)). The only difference is that you need to manually 
specify the main class. When you run a JAR from the command line, the JVM automatically knows which 
class contains the entry point into the program (the `main` function), but in IDEs you must create 
a "run configuration".

Both IntelliJ and Eclipse have "run" menus, from which you can select an option to edit the run configurations.
You want to create a configuration for a Java Application, specifying the main class
`org.opentripplanner.standalone.OTPMain`. Unlike on the command line, the arguments to the JVM and to the main class
you are running are specified separately. In the field for the VM options you'll want to put your maximum memory
parameter (`-Xmx2G`, or whatever limit you want to place on JVM memory usage). The rest of the parameters to OTP itself
will go in a different field with a name like "program arguments".


## Contributing to the project

OpenTripPlanner is a community based open source project, and we welcome all who wish to contribute.
There are several ways to get involved:

 * Join the [developer mailing list](http://groups.google.com/group/opentripplanner-dev)

 * Fix typos and improve the documentation within the `/docs` directory of the project (details below).

 * [File a bug or new feature request](http://github.com/openplans/OpenTripPlanner/issues/new).

 * Submit patches. If you're not yet a committer, please provide patches as pull requests citing the relevant issue.
   Even when you do have push access to the repository, pull requests are a good way to get feedback on changes.

### Branches and Branch Protection

As of January 2019, we have begun work on OTP 2.x and are using a Git branching model derived from [Gitflow](https://nvie.com/posts/a-successful-git-branching-model/). All development will occur on the `dev-1.x` and `dev-2.x` branches. Only release commits setting the Maven artifact version to a non-snapshot number should be pushed to the `master` branch of OTP. All other changes to master should result from fast-forward merges of a Github pull request from the `dev-1.x` branch. In turn, all changes to `dev-1.x` should result from a fast-forward merge of a Github pull request for a single feature, fix, or other change. These pull requests are subject to code review. We require two pull request approvals from OTP leadership committee members or designated code reviewers from two different organizations. We also have validation rules ensuring that the code compiles and all tests pass before pull requests can be merged.

The `dev-2.x` branch is managed similarly to `dev-1.x` but because it's rapidly changing experimental code worked on by relatively few people, we require only one pull request approval from a different organization than the author. Merges will not occur into `master` from `dev-2.x` until that branch is sufficiently advanced and receives approval from the OTP project leadership committee.

### Issues and commits

All commits should reference a specific issue number (this was formally decided in issue #175).
For example, `Simplify module X configuration #9999`.
If no ticket exists for the feature or bug your code implements or fixes,
you should [create a new ticket](http://github.com/openplans/OpenTripPlanner/issues/new) prior to checking in, or
ideally even prior to your development work since this provides a place to carry out implementation discussions (in the comments).

GitHub will automatically update issues when commits are merged in: if your commit message includes the text
` fixes #123 `, it will automatically append your message as a comment on the isse and close it.
If you simply mention ` #123 ` in your message, your message will be appended to the issue but it will remain open.
Many other expressions exist to close issues via commit messages. See [the GitHub help page on this topic](https://help.github.com/articles/closing-issues-via-commit-messages/).

### Unit tests using real OSM data

Sometimes it is useful to build a graph from actual OSM or GTFS data. Since building these graphs
in a test can be quite slow they will be accepted in pull requests only if they conform to certain
standards:

1. Use the smallest possible regional extract - the OSM file should not contain more than a few hundred ways.
   Use `osmium-extract` to cut down a larger OSM file into a tiny subset of it.
   
2. Strip out any unneeded information by using the `osmium filter-tags` as describe in [Preparing OSM](Preparing-OSM.md)


### Code Comments

As a matter of [policy](http://github.com/opentripplanner/OpenTripPlanner/issues/93), all new 
methods, classes, and fields should include comments explaining what they are for and any other
pertinent information. For Java code, the comments should use the 
[JavaDoc conventions](http://java.sun.com/j2se/javadoc/writingdoccomments). It is best to provide
comments that not only explain *what* you did but also *why you did it* while providing some
context. Please avoid including trivial Javadoc or the empty Javadoc stubs added by IDEs, such as
`@param` annotations with no description.


### Documentation

OTP documentation is included directly in the OpenTripPlanner repository. 
This allows version control to be applied to documentation as well as program source code.
All pull requests that change how OTP is used or configured should include changes to the
documentation alongside code modifications. 

The documentation files are in Markdown format and are in the `/docs` directory under the root of 
the project. On every push to the master branch the documentation will be rebuilt and deployed as 
static pages to our subdomain of [ReadTheDocs](http://opentripplanner.readthedocs.org/). MkDocs is 
a Python program and should run on any major platform. See http://www.mkdocs.org/ for information on
how to install it and how to generate a live local preview of the documentation while you're working
on writing it.

In short:

```
$ pip install mkdocs
$ mkdocs serve
```

The OTP REST API documentation is available online in the format of:

http://dev.opentripplanner.org/apidoc/x.x.x/index.html

For example, for v2.0.0:

http://dev.opentripplanner.org/apidoc/2.0.0/index.html


### Debug layers

Adding new renderer is very easy. You just need to create new class (preferably in
`org.opentripplanner.inspector` package) which implements EdgeVertexRenderer. It is best if class
name ends with Rendered. To implement this interface you need to write three functions `renderEdge`,
`renderVertex` and `getName`. Both render functions accepts `EdgeVisualAttributes` object in which
label of edge/vertex and color can be set. And both return `true` if edge/vertex should be rendered
and `false` otherwise. `getName` function should return short descriptive name of the class and will
be shown in layer chooser.

For examples how to write renderers you can look into example renderers which are all in `org.opentripplanner.inspector` package.

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


## Continuous Integration

The OpenTripPlanner project uses the [Travis CI continuous integration system](https://travis-ci.org/opentripplanner/OpenTripPlanner). Any time a change
is pushed to the main OpenTripPlanner repository on GitHub, this server will compile and test the new code, providing feedback on the stability of the build.

## Release Process

This section serves as a checklist for the person performing releases. Note that much of this mimics 
the actions taken by the Maven release plugin. Based on past experience, the Maven release plugin can 
fail at various points in the process leaving the repo in a confusing state. Taking each action 
manually is more tedious, but keeps eyes on each step and is less prone to failure. Releases are 
performed off the master branch, and are tagged with git annotated tags.

* Check that your local copy of the dev branch is up to date with no uncommitted changes
    * `git status`
    * `git checkout dev-1.x`
    * `git clean -df`
    * `git pull`
* Verify that all dependencies in the POM are non-SNAPSHOT versions
    * Currently we do have one SNAPSHOT dependency on `crosby.binary.osmpbf` which we are working to eliminate
* Update `docs/Changelog.md`
    * Lines should have been added or updated as each pull request was merged
    * If you suspect any changes are not reflected in the Changelog, review the commit log and add any missing items
    * Update the header at the top of the list from `x.y.z-SNAPSHOT` to just `x.y.z (current date)`
    * Check in any changes, and push to Github
* Check on Travis that the build is currently passing
    * [Link to OTP builds on Travis CI](https://travis-ci.org/opentripplanner/OpenTripPlanner/builds)
* Switch to the HEAD of master branch, and ensure it's up to date with no uncommitted changes
    * `git checkout master`
    * `git status`
    * `git clean -df`
    * `git pull`
* Merge the dev branch into master
    * `git merge dev-1.x` 
* Bump the SNAPSHOT version in the POM to the release version
    * Edit version in POM, removing SNAPSHOT and increasing version numbers as needed (following semantic versioning)
    * `git add pom.xml`
    * `git commit -m "prepare release x.y.z"`
* Run a test build of the release locally, without deploying it
    * `mvn clean install site`
    * The `install` goal will sign the Maven artifacts so you need the GPG signing certificate set up
    * You can also use the `package` goal instead of the `install` goal to avoid signing if you don't have the GPG certificate installed.
    * All tests should pass
    * This build will also create Enunciate API docs and Javadoc with the correct non-snapshot version number
    * The 'site' goal may cause a failing Javadoc build. In that case you can manually build only the Enunciate docs with `mvn enunciate:docs`
* Deploy the documentation to AWS S3
    * You have to do this right after the test release build to ensure the right version number in the docs
    * You will need AWSCLI tools (`sudo pip install -U awscli`)
    * You will need AWS credentials with write access to the bucket `s3://dev.opentripplanner.org`
    * `aws s3 cp --recursive target/site/apidocs s3://dev.opentripplanner.org/javadoc/x.y.z --acl public-read`
    * `aws s3 cp --recursive target/site/enunciate/apidocs s3://dev.opentripplanner.org/apidoc/x.y.z --acl public-read`
    * Check that docs are readable and show the correct version via the [development documentation landing page](http://dev.opentripplanner.org).
* Finally, if everything looks good, tag and push this release to make it official and trigger deployment
    * `git tag -a vX.Y.Z -m "release X.Y.Z"`
    * `git push origin vX.Y.Z`
    * Pushing the tag will trigger a Travis CI build and deployment of release Maven artifacts
    * Note that **only one** commit may have a particular non-snapshot version in the POM (this is the commit that 
      should be tagged as the release)
* Set up next development iteration
    * Add a new section header to `docs/Changelog.md` like `x.y+1.0-SNAPSHOT (in progress)`
    * Edit minor version in `pom.xml` to `x.y+1.0-SNAPSHOT`
    * `git add pom.xml docs/Changelog.md`
    * `git commit -m "Prepare next development iteration x.y+1.0-SNAPSHOT"`
    * `git push`
* Check that Travis CI build of the release tag succeeded
    * [Link to OTP builds on Travis CI](https://travis-ci.org/opentripplanner/OpenTripPlanner/builds)
    *  Check the end of the build log to make sure the Maven artifacts were staged for release
* Check that Maven artifact appears on Maven Central (deployment succeeded)
    * [Directory listing of OTP releases on Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp/)
    * It may take a while (half an hour) for releases to show up in the central repo after Travis uploads the artifacts
* Merge master back into dev (to sync up the Maven artifact version from the POM)
    * `git checkout dev-1.x`
    * `git merge master`
    * `git push`
* Make sure the main documentation is built
    * For some reason it doesn't always build automatically
    * Go to [builds of docs.opentripplanner.org](http://readthedocs.org/projects/opentripplanner/builds/)
    * Click "build version: latest"
* Email the OTP dev and users mailing lists
    * Mention the new version number.
    * Provide links to the new developer documentation.
    * Provide links to the artifacts directory on Maven Central.
    * Trigger build of latest OTP documentation on Readthedocs.

## Additional Information on Releases

OpenTripPlanner is released as Maven artifacts to Maven Central. These include compiled and source code JARs as well as 
a "shaded" JAR containing all dependencies, allowing stand-alone usage. This release process is handled by the 
Sonatype Nexus Staging plugin, configured in the OpenTripPlanner POM. Typically this final Maven deployment action is 
performed automatically when the Travis CI build succeeds in building a non-SNAPSHOT version. 

### Artifact Signing

Maven release artifacts must be digitally signed to prove their origin. This is a safeguard against compromised code 
from a malicious third party being disguised as a trusted library.

The OTP artifact signing key was created by Conveyal. We export only that signing subkey, with our company's main key 
blanked out. Therefore, even if someone managed to acquire the decrypted key file and the associated GPG passphrase, 
they would not have the main key. We could deactivate the signing key and create a new one, without the main key being 
compromised.

The exported signing key is present in the root of the git repo as the encrypted file `maven-artifact-signing-key.asc.enc`.
When building a tagged release, Travis CI will decrypt this file and import it into GPG on the build machine. The signing
key ID and GPG passphrase are also present as encrypted environment variables in the Travis configuration YAML. This only
happens on code from non-fork, non-pull-request commits, ensuring that no unreviewed third-party code has access to 
these files or variables.

OpenTripPlanner's POM is set up to sign artifacts in the verify phase, which means signing will happen for the `install` 
and `deploy` targets, but not the `package` target.
When performing a local test build, if you do `mvn clean install site` it will test the signing process. 
If you do not have the certificate installed, you can instead to `mvn clean package site` to bypass signing, but this 
provides less certainty that everything is set up correctly for the CI-driven final release.

