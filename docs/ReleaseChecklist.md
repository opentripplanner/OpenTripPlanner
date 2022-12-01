# Release Checklist

This section serves as a checklist for the person performing releases. Note that much of this mimics
the actions taken by the Maven release plugin. Based on past experience, the Maven release plugin
can fail at various points in the process leaving the repo in a confusing state. Taking each action
manually is more tedious, but keeps eyes on each step and is less prone to failure.

* Make sure the documentation is up to date
  * Check all links and references to the release and update to the target release version. Search
    all files for with a regular expression: `2\.[012]\.0` and replace if appropriate with the new 
    version.
* Check that your local copy of the dev branch is up to date with no uncommitted changes
    * `git status`
    * `git checkout dev-2.x`
    * `git clean -df`
    * `git pull`
* Verify that all dependencies in the POM are non-SNAPSHOT versions (e.g. with `grep`)
* Update `docs/Changelog.md`
    * Lines should have been added or updated as each pull request was merged
    * If you suspect any changes are not reflected in the Changelog, review the commit log and add
      any missing items
    * Update the header at the top of the list from `x.y.z-SNAPSHOT` to just `x.y.z (current date)`
    * Check in any changes, and push to GitHub
    * It is important to finalize the documentation before tagging the release, to ensure the 
      published documentation is associated with the release tag 
* Update the variable MASTER_BRANCH_VERSION in `cibuild.yml`
    * This tells the GH Action that pushes the documentation on master what the name of the 
      current version is. 
      For version 2.3.0 Leonard has already done it: [Example commit](https://github.com/opentripplanner/OpenTripPlanner/commit/3cb061ab1e4253c3977a5d08fa5abab1b0baefd7)
* Check [on GH Actions](https://github.com/opentripplanner/OpenTripPlanner/actions/workflows/) that
  the build is currently passing
* Switch to the HEAD of master branch, and ensure it's up to date with no uncommitted changes
    * `git checkout master`
    * `git status`
    * `git clean -df`
    * `git pull`
* Merge the dev branch into master
    * `git merge dev-2.x`
* Bump the SNAPSHOT version in the POM to the release version
    * Edit version in POM, removing SNAPSHOT and increasing version numbers as needed (following
      semantic versioning)
    * `git add pom.xml`
    * `git commit -m "prepare release x.y.z"`
* Run a test build of the release locally, without deploying it
    * `mvn clean install site -Prelease`
      The current version of ENUNCIATE does not support Java 17 "out of the box", use 
      `export MAVEN_OPTS="--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"`
      to ignore the problem.
    * The `install` goal will sign the Maven artifacts so you need the GPG signing certificate set
      up
    * You can also use the `package` goal instead of the `install` goal to avoid signing if you
      don't have the GPG certificate installed.
    * All tests should pass
    * This build will also create Enunciate API docs and Javadoc with the correct non-snapshot
      version number
* Deploy the documentation to AWS S3
    * You have to do this right after the test release build to ensure the right version number in
      the docs
    * You will need AWSCLI tools (`sudo pip install -U awscli`)
    * You will need AWS credentials with write access to the bucket `s3://dev.opentripplanner.org`
    * `aws s3 cp --recursive target/site/apidocs s3://dev.opentripplanner.org/javadoc/x.y.z --acl public-read`
    * `aws s3 cp --recursive target/site/enunciate/apidocs s3://dev.opentripplanner.org/apidoc/x.y.z --acl public-read`
    * Check that docs are readable and show the correct version via
      the [development documentation landing page](http://dev.opentripplanner.org).
* Finally, if everything looks good, tag and push this release to make it official
    * `git tag -a vX.Y.Z -m "release X.Y.Z"`
    * `git push origin vX.Y.Z`
    * `git push origin master`
    * Note that **only one** commit may have a particular non-snapshot version in the POM (this is
      the commit that should be tagged as the release)
    * Go to the [GitHub tags page](https://github.com/opentripplanner/OpenTripPlanner/tags) and use
      the `...` button to mark the new tag as a release.
    * Give the release a name like `v2.2.0 (November 2022)`
    * Optionally add a very condensed version of the changelog in the description box, with only the
      5-10 most significant changes that might affect someone's choice to upgrade.
    * Attach the JAR files produced in `/target` to the GitHub release page you just created.
* Check that the push triggered an Actions build and that it was uploaded to GHPR.
    * Currently, pushing the tag does not trigger deployment of release Maven artifacts, only GHPR
      JARs.
* Deploy the build artifacts to Maven Central and GitHub and Maven Central
    * This step can get complicated and requires you to have the GPG keys and Maven repo credentials
      set up.
    * Apply the changes recorded
      in https://github.com/opentripplanner/OpenTripPlanner/tree/signed-deploy-to-central
    * While still on the tag commit, run `mvn deploy -Prelease`.
* Set up next development iteration
    * Add a new section header to `docs/Changelog.md` like `x.y+1.0-SNAPSHOT (in progress)`
    * Edit minor version in `pom.xml` to `x.y+1.0-SNAPSHOT`
    * `git add pom.xml docs/Changelog.md`
    * `git commit -m "Prepare next development iteration x.y+1.0-SNAPSHOT"`
    * `git push`
* Check that Maven artifact appears on Maven Central
    * [Directory listing of OTP releases on Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp/)
    * It may take a while (half an hour) for releases to show up in the central repo after Travis
      uploads the artifacts
* Merge master back into dev (to sync up the Maven artifact version from the POM)
    * `git checkout dev-2.x`
    * `git merge master`
    * `git push`
* Email the OTP dev and users mailing lists
    * Mention the new version number.
    * Provide links to the new developer documentation.
    * Provide links to the artifacts directory on Maven Central.

## Artifact Signing

Maven release artifacts must be digitally signed to prove their origin. This is a safeguard against
compromised code from a malicious third party being disguised as a trusted library.

The OTP artifact signing key was created by Conveyal. We export only that signing subkey, with our
company's main key blanked out. Therefore, even if someone managed to acquire the decrypted key file
and the associated GPG passphrase, they would not have the main key. We could deactivate the signing
key and create a new one, without the main key being compromised.

When modified for Maven Central deployment, OpenTripPlanner's POM is set up to sign artifacts in the
verify phase, which means signing will happen for the `install` and `deploy` targets, but not the
`package` target. When performing a local test build, if you do `mvn clean install site` it will
test the signing process. If you do not have the certificate installed, you can instead do
`mvn clean package site` to bypass signing, but this provides less certainty that everything is set
up correctly for the CI-driven final release.
