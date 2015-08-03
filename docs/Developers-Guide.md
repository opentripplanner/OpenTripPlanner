# Developers Guide

## Working on OTP in an IDE

Most people writing or modifying OTP code use an Integrated Development Environment (IDE). Some of the most popular IDEs for
Java development are [Eclipse](http://eclipse.org), [IntelliJ IDEA](https://www.jetbrains.com/idea/), and
[NetBeans](https://netbeans.org). All three of these environments are good for working on OTP,
and any IDE with Maven build support should also work (ensure that you have the Maven plugins installed and enabled).
Git integration is a plus since OTP is under Git version control.

IntelliJ IDEA is a commercial product, but its authors give free licenses to open source projects and the OpenTripPlanner
development community has received such a license. If you want to use IntelliJ just ask us for the license key. It's
an excellent IDE, and in my experience is quicker and more stable than the competition.

Rather than using the version control support in my IDE, I usually find it more straightforward to clone the OTP GitHub
repository manually (on the command line or using some other Git interface tool), then import the resulting local OTP
repository into my IDE as a Maven project. The IDE should then take care of fetching all the libraries OTP depends on,
based on the Maven project description (POM file) in the base of the OTP repository. This step can take a long time because
it involves downloading a lot of JAR files.

When running your local copy of the OTP source within an IDE, all command line switches and configuration options will
be identical to the ones used when running the OTP JAR from the command line (as described in the
[basic introduction](Basic-Usage) and [configuration reference](Configuration)). The only difference is that you need to
manually specify the main class. When you run a JAR from the command line, the JVM automatically knows which class
contains the entry point into the program (the `main` function), but in IDEs you must create a "run configuration".

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

 * Fix typos and improve the documentation on the wiki or within the `/docs` directory of the project (details below).

 * [File a bug or new feature request](http://github.com/openplans/OpenTripPlanner/issues/new).

 * Submit patches. If you're not yet a committer, please provide patches as pull requests citing the relevant issue.
   Even when you do have push access to the repository, pull requests are a good way to get feedback on changes.


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


### Code Comments

As a matter of [policy](http://github.com/opentripplanner/OpenTripPlanner/issues/93), all new methods, classes, and 
fields should include comments explaining what they are for and any other pertinent information. For Java code, 
the comments should use the [JavaDoc conventions](http://java.sun.com/j2se/javadoc/writingdoccomments).
It is best to provide comments that
not only explain *what* you did but also *why you did it* while providing some context. Please avoid including trivial
Javadoc or the empty Javadoc stubs added by IDEs, such as `@param` annotations with no description.

### Documentation

Most documentation should be included directly in the OpenTripPlanner repository rather than the GitHub wiki.
This allows version control to be applied to documentation as well as program source code.
All pull requests that change how OTP is used or configured should include changes to the documentation alongside code
modifications. Pages that help organize development teams or serve as scratchpads can still go
[on the wiki](https://github.com/opentripplanner/OpenTripPlanner/wiki), but all documentation that would be of interest
to people configuring or using OTP belong [in the repo](https://github.com/opentripplanner/OpenTripPlanner/tree/master/docs).

The documentation files are in Markdown format and are in the `/docs` directory under the root of the project. On every
push to the master branch the documentation will be rebuilt and deployed as static pages to our subdomain of
[ReadTheDocs](http://opentripplanner.readthedocs.org/). MkDocs is a Python program and should run on any major platform.
See http://www.mkdocs.org/ for information on how to install it and how to generate a live local preview of the
documentation while you're working on writing it.

In short:

```
$ pip install mkdocs
$ mkdocs serve
```

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

Please use only ISO 8601 date format (YYYY-MM-DD) in documentation, comments, and throughout the project.
This avoids the ambiguity that can result from differing local interpretations of date formats like 02/01/12.


### Project proposals and decision making

Decisions are made by the OpenTripPlanner community through a proposal and informal voting process on the 
[project mailing list](http://groups.google.com/group/opentripplanner-dev).

While we do vote on proposals, we don't vote in a strict democratic sense, but rather as a way to easily register 
opinions, foster discussion, and move toward consensus. When responding to a proposal, we use the following system:

 * +1 - *I support this*

 * +0 - *I don't have a strong opinion, but I'm not opposed*

 * -0 - *I'm against this, but I don't have a good alternative / I'm not willing to do the work on the alternative / I won't block*

 * -1 - *Blocking no* (note: in general and when appropriate, this requires the blocker to propose something else that he/she would help put the time into doing)

A proposal does *not* need to be a formal or lengthy document; it can and should be a straightforward recommendation of 
what you want to do, ideally with a brief explanation for why it's a good idea. 

Proposals are just messages sent to the list and can be as simple as *"I think we should do X because of Y and Z. 
Deadline for response is 2015-10-29. Assuming I've heard no blocking votes by then, I'll go ahead."*
Note that you should make sure to include a **deadline** by which you will go ahead and do what you're proposing 
if you don't hear any blocking responses. In general, you should leave at least 72 hours for people to respond. 
This is not a hard-and-fast rule and you should use your best judgement in determining how far in the future the 
deadline should be depending on the magnitude of the proposal and how much it will affect the overall project and the 
rest of the community.

Of course you may always fork the [OTP repo on GitHub](https://github.com/opentripplanner/OpenTripPlanner/) 
and submit your changes as a pull request, or develop and share whatever features you like on your fork even if they
are not included in mainline OTP.


## Code style

### Java

OpenTripPlanner uses the same code formatting and style as the [GeoTools](http://www.geotools.org/) and 
[GeoServer](htp://geoserver.org) projects. It's a minor variant of the 
[Sun coding convention](http://www.oracle.com/technetwork/java/codeconv-138413.html). Notably, **we do not use tabs** 
for indentation and we allow for lines up to 100 characters wide.

The Eclipse formatter configuration supplied by the GeoTools project allows comments up to 150 characters wide.
A modified version included in the OpenTripPlanner repository will wrap comments to the same width as lines of code, 
which makes for easier reading in narrow windows (e.g. when several documents are open side-by-side on a wide display).

If you use Eclipse, you should do the following to make sure your code is automatically formatted correctly:

1. Open the project `Properties` (right-click on the project directory in Eclipse and select `Properties` or choose `Project` -> `Properties`).

2. Select `Java`, then `Code Style`, and finally `Formatter`.  

3. Check the `Enable project specific settings` checkbox.

4. Click `Import...`, select the `formatter.xml` file in the root of the OpenTripPlanner git repository, and click `Open`.

5. Click `OK` to close the `Properties` window.


### JavaScript

As of #206, we follow [Crockford's JavaScript code conventions](http://javascript.crockford.com/code.html). Further guidelines include:

 * All .js source files should contain one class only

 * Capitalize the class name, as well as the source file name (a la Java)

 * Include the GNU LGPL header at top of file, i.e., `/* This program is free software:...*/`

 * Include the namespace definition in each and every file: `otp.namespace("otp.configure");`

 * Include a class comment. For example,                                                                                                      

```java
    /**
     * Configure Class
     *
     * Purpose is to allow a generic configuration object to be read via AJAX/JSON, and inserted into an Ext Store
     * The implementation is TriMet route map specific...but replacing ConfigureStore object (or member variables) with
     * another implementation, will give this widget flexibility for other uses beyond the iMap.
     *
     * @class
     */
```

*Note: There is still a lot of code following other style conventions, but please adhere to consistent style when you
 write new code, and help clean up and reformat code as you refactor.*


## Continuous Integration

The OpenTripPlanner project has a [continuous integration (CI) server](http://ci.opentripplanner.org). Any time a change
is pushed to the main OpenTripPlanner repository on GitHub, this server will compile and test the new code, providing
feedback on the stability of the build. It is also configured to run a battery of speed tests so that we can track
improvements due to optimizations and spot drops in performance as an unintended consequence of changes.

## Release Process

This section is intended as a checklist for the person within the OTP development community who is responsible for
performing releases (currently Andrew Byrd). Releases should be performed on the CI server because documentation and
release JARs must be published after a release. The CI server serves as the OTP project web server, and also has a
very fast network connection which makes uploading Maven artifacts to the repository on Amazon S3 less painful.
Carrying out a release requires CI server login credentials as well as Amazon AWS credentials for deployment to our
Maven repository.

Release checklist:

- update docs/Changelog.md, check in changes, and push
- ssh into ci.opentripplanner.org
- change to the ~/git/OpenTripPlanner directory
- check that you are on the master branch with no uncommitted changes (git status; git clean -df)
- pull down the latest master code
- verify that git push succeeds without prompting for a password (i.e. ~/.ssh/id_rsa.pub is known to Github)
- run a test build: mvn clean package site
- check that `~/.m2/settings.xml` contains AWS credentials for the repo
- mvn release:prepare (use the default release version and SCM release tag, bump the minor development version number)
- cp -R target/site/apidocs /usr/share/nginx/html/javadoc/x.y.0
- cp -R target/site/enunciate /usr/share/nginx/html/apidoc/x.y.0
- check that all docs in /usr/share/nginx/html/javadoc/ have o+r permissions (they should be by default)
- mvn release:perform
- cp target/checkout/target/otp-x.y.0-shaded.jar /usr/share/nginx/html/jars/
- rm /usr/share/nginx/html/jars/otp-x.y.0-SNAPSHOT*
- check http://dev.opentripplanner.org/jars/ and http://dev.opentripplanner.org/javadoc/ in a browser
- update the version numbers that appear in Basic-Usage, Developers-Guide, Getting-OTP, and index.md and check them in
- email the OTP dev and users mailing lists, and send a message on Slack

