# Release Checklist

This section serves as a checklist for the person performing releases. Note that much of this mimics
the actions taken by the Maven release plugin. Based on past experience, the Maven release plugin
can fail at various points in the process leaving the repo in a confusing state. Taking each action
manually is more tedious, but keeps eyes on each step and is less prone to failure.

* Check that your local copy of the dev branch is up to date with no uncommitted changes
    * `git status`
    * `git checkout dev-2.x`
    * `git clean -df`
    * `git pull`
* Make sure the documentation is up to date
    * Check all links and references to the release and update to the target release version. Search
      all files for with a regular expression: `2\.[012]\.0` and replace if appropriate with the new
      version.
    * In `doc/user/index.md` replace what is the latest version and add a new line for the previous one
* Update `doc/user/Changelog.md`
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
* Verify that all dependencies in the POM are non-SNAPSHOT versions (e.g. with `grep`)
* Check [on GH Actions](https://github.com/opentripplanner/OpenTripPlanner/actions/workflows/) that the build is currently passing
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
    * `mvn versions:set -DnewVersion=x.y.z`
    * `git add pom.xml`
    * `git commit -m "prepare release x.y.z"`
* Run a test build of the release locally, without deploying it
    * `mvn clean install -Prelease`
    * The `install` goal will sign the Maven artifacts so you need the GPG signing certificate set
      up
    * You can also use the `package` goal instead of the `install` goal to avoid signing if you
      don't have the GPG certificate installed.
    * All tests should pass
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
* Check that Maven artifact appears on Maven Central
    * [Directory listing of OTP releases on Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp/)
    * It may take a while (half an hour) for releases to show up in the central repo after Travis
      uploads the artifacts
* Merge master back into dev (to sync up the Maven artifact version from the POM)
    * `git checkout dev-2.x`
    * `git merge master`
    * `git push`
* Set up next development iteration
    * Add a new section header to `doc/user/Changelog.md` like `x.y+1.0-SNAPSHOT (in progress)`
    * Edit version in `pom.xml`s: `mvn versions:set -DnewVersion=x.y.z-SNAPSHOT`
    * `git add pom.xml doc/user/Changelog.md`
    * `git commit -m "Prepare next development iteration x.y+1.0-SNAPSHOT"`
    * `git push`
* Send a message in Gitter and email the OTP users mailing lists
    * Mention the new version number.
    * Provide links to the new developer documentation.
    * Provide links to the artifacts directory on Maven Central.
* Prepare for the next release in GitHub by renaming the released milestone and creating a new
  milestone for the next release. Then make sure all issues and PRs are tagged with the correct
  milestone.
    * Close open PRs older than 2 years, make sure the milestone is set to `Rejected`.
    * Rename the old milestone from `x.y (Next Release)` to `x.y`. All issues and PRs assigned to 
      this milestone are automatically updated.
    * Create a new milestone: `x.y+1 (Next Release)`
    * All PullRequests SHOULD have a milestone (except some very old ones) 
        * Assign all *open* PRs to this new milestone `x.y+1 (Next Release)`.
        * Assign all *closed* PRs without a milestone in the release to the released milestone 
          `x.y`. Make sure NOT to include very old PRs or PRs merged after the release(if any).
    * Some issues have a milestone, but not all.
        * Move all open issues with the released milestone `x.y` to the next release 
          `x.y+1 (Next Release)`. 

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
