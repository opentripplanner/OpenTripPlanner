# README - OTP Custom/Fork Release Scripts

**Note! This describes how you can set up and release OTP in your own GitHub fork, not how OTP in
the main repo is released.**


## Introduction

The scripts here can be used to release a fork of OTP. Run

```
# script/custom-release.py otp/dev-2.x
```

You can customize the release with:

 - `script/custom-release-env.json` Required config for the main script.
 - `script/custom-release-extension` Optional script executed by the main scrit if it exist.


### Process overview

The release is made in the `release branch` in the forked git repository. The release is created 
based on the `base revision` - a branch, tag or commit, for example `otp/dev-2.x`. 

1. The release script will start by _resetting_ the `release branch` to the given `base revision`.
   **Nothing is kept from previous releases.**
2. Then all pending PRs tagged with your `TEST` label is meged in. The name of the label is 
   configured in the `custom-release-env.json` file.
3. Then all your extension branches are meged in. These are normally branchech with your deployment
   config and GitHub workflow files. The name of these branches is configured in the 
   `custom-release-env.json` file.
4. The `custom-release-extension` script is run, if it exist. For example you may delete 
   workflow scripts comming from the upstream `base revision`. 
5. The old release is then merged with an _empty merge_, this is done to create a continuous line
   of releases in the release branch for easy viewing and navigation of the git history. Nothing
   from the previous release is copied into the new release. We call this an _empty merge_.
6. Then the script update the _otp-serialization-version-id_ and the OTP _version_ in the pom.xml 
   file. Each release is given a unique version number specific to your fork, like 
   `v2.7.0-MY_ORG-1`. The release script uses both the git history and the GitHub GraphQL API 
   (PRs labeled `bump serialization id`) to resolve the serialization version number. 
7. Finally the release is tagged and pushed to the remote Git repository. The repository is 
   configured in the `custom-release-env.json` file.

Do not worry about deleting more recent versions, the release script will preserve the history so
nothing is lost.

> Tip! If something goes wrong, like a conflicting merge, you may fix the problem and resume the
> release by running the release script again. The script will detect that it failed and ask you
> what to do.

The release script also support making a hotfix release. Simply make the needed changes 
to any commit in the release branch. For example merge in bug-fix branches on top of another 
release tag. Complete the process by running:

```
# script/custom-release.py --release
```

### Advanced Git flow - roll back

![Release git flow](images/release-git-flow.svg)

Each release has a single commit as a base, this allows us to choose any commit as the base and
safely **Roll-back, by rolling forward**. In the diagram above, commit (D) contained a bug. So, we
can go back to commit (C). If we merged `dev2-x` into the release-branch then going back to this
commit would be challenging since commit (D) was already in the release branch at this point. 
Also note that the CI configuration is changed (c2) and this change needs to be included in the
new release (v3). 
 
> **Note!** OTP ignores config it does not understand it. This allows us to roll out config for new 
> features BEFORE we roll out the new version of OTP. We can then verify that the old version of 
> OTP works with the new config. Config is dependent on both the environment OTP run in and OTP. 
> So, the config has its own life-cycle, independent of the OTP software.


### Advanced Git flow - rollback and a feature branch

![Release git flow with feature branch](images/release-git-flow-feature-branch.svg)

When you make a new releae all open PRs tagged with your custom `TEST` label is automatically
merged in. This is ilustrated with the _feature-branch-in-progress_ above. 

## Extension branches

You should create one or more branches in the local git repository where you keep your 
deployment-specific config. Put the following in this(these) branch(es):

The branch(es) must include(required):

- The `script/custom-release-env.json`, containing the release configuration. The branch containing
  this file must be merged into the `release brach` before the first release. If you make changes
  to the config the branch must be merged into main again before the changes take  effect. You can
  do this by making a release(last release with old config), and then a new release - or just merg
  in the config before you run the script. If the release run in a CI/CD pipline the easiest is to
  just trigger two new releases.

The branch may include(optional):
 
- Local CI build configuration.
- OTP deployment configuration.
- `script/custom-release-extension`, this is your custom script executed by the main  release
  script if it exist. At entur we delete workflows from the upstream project and move some of the
  configuration into the right place. We have all over config in a single folder to be able to
  maintain it in a simple way and avoid merge conflicts.

The config branches are merged into the release - so the best way to avoid merge conflict is to
use a "old" commit from the **base branch/repo** as the base for your config. Do not use a commit 
which only exist in the release branch, this will lead to conficts with the pom.xml version number.


## Setup

Create a configuration extension branch (`main_config`) in your local fork based on a commit in the
upstream repo, for example `HEAD` of `opentripplanner/OpenTripPlanner/dev-2.x`. 

Add the `script/custom-release-env.json` file to your branch. The content of the file should be:

```json
{
  "upstream_remote": "otp",
  "release_remote": "<organization name>",
  "release_branch": "<release branch>",
  "ser_ver_id_prefix": "<organization abbrivation, max 2 characters>",
  "include_prs_label": "<organization name> Test",
  "ext_branches" : [ "<release config branch>"]
}
```
If you organization is _Curium_, then the file would look like this:
```json
{
  "upstream_remote": "otp",
  "release_remote": "curium",
  "release_branch": "main",
  "ser_ver_id_prefix": "CU",
  "include_prs_label": "Curium Test",
  "ext_branches" : [ "main_config"]
}
```

The `<organization name>` must match the GitHub organization name (repository owner) in your local
Git clone. Use `git remote -v` to list all remote repos. 

```
# git remote -v 
curium	https://github.com/curium/OpenTripPlanner.git (fetch)
curium	https://github.com/curium/OpenTripPlanner.git (push)
otp	https://github.com/opentripplanner/OpenTripPlanner.git (fetch)
otp	https://github.com/opentripplanner/OpenTripPlanner.git (push)
```

Avoid using the default `origin`, instead rename your repositories to avoid mistakes.

```
# git remote rename origin otp 
```

### Setup GitHub Access

The script uses the GitHub GraphQL API to fetch information about pending PRs. This call must
have a valid access token. The token only need READ access to PUBLIC repositories.

#### Setting GitHub Access token from a workflow (Continuous Integration Pipeline)

Export the `secrets.GITHUB_TOKEN` to the `CUSTOM_RELEASE_GIT_HUB_API_TOKEN` environment variable 
to run the script in a workflow.

```
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Release OTP
        env:
          CUSTOM_RELEASE_GIT_HUB_API_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          CUSTOM_RELEASE_LOG_LEVEL: "info"
        run: |
          echo "Start otp release script"
          git config user.name github-actions[bot]
          git config user.email 41898282+github-actions[bot]@users.noreply.github.com
          git remote rename origin entur
          git remote add otp https://github.com/opentripplanner/OpenTripPlanner.git
          git fetch --no-tags otp
          git remote -v
          echo "Run script/custom-release.py otp/dev-2.x"
          script/custom-release.py --summary otp/dev-2.x
```

#### Setting GitHub Access token in on local machine 

1. Create a [Fine-grained personal access tokens](https://github.com/settings/personal-access-tokens)
   with access to PUBLIC repositories with READ ACCESS only. Copy token.
2. Add the token to your local terminal configuration(_.bashrc_ or _.zshrc_):

```
export CUSTOM_RELEASE_GIT_HUB_API_TOKEN=github_pat_Z2y...
```


### Pending Pull Requests

You may want to merge in some of the pending PRs when releasing your fork. To do so you can add a
label to these PRs and specify the label in the _script/custom-release-env.json_ file. At Entur we
label all PRs we want to be merged into the next release with `Entur Test`, this ensures that any
team member can do the release. This allows us to test features at Entur before the PR is accepted
and merged in the upstream repo. We combine this with config, and sometimes the OTPFeature toggle
to turn _on_ new features in over test environment. When the new feature is tested ok, we can 
enable it by changing the config.


## How To Make The First Release

The release script looks for the _last release_ to resolve the next version. It uses the version
in the Maven _pom.xml_, strips of `-SNAPSHOT` and appends `<organization name>` and a sequence number
(`N`). The release fails if there are no tags matching `vX.Y.Z-<organization name>-N`. There are two
ways to resolve this:

- The simplest way to fix this, is to tag the _last release_ with an extra tag. Let say the new OTP 
  version is `X.Y.Z-SNAPSHOT`. Then tag the last released version (or any commit in the release 
  branch) with `vX.Y.Z-<organization name>-0`, for example `v2.8.0-entur-0`. The script will now 
  use this as the _last version_ and set the new version number to `v2.8.0-entur-1`. You can delete
  the tag `v2.8.0-entur-0` after the release is done.
- Another way to do it, is to make the first release manually. Make sure to tag the release with 
  `vX.Y.Z-<organization name>-1`. 


## How To Make A Release

Find the base branch/commit to use, for example `otp/dev-2.x`. If you use a specific branch/commit, 
then replace `otp/dev-2.x` below with your branch name or commit hash. Run the  script:

```
# script/custom-release.py otp/dev-2.x
```

Use `script/custom-release.py --help` to list documentation and print options.

The `--dryRun` options is used to run the script and skip pushing changes to the remote repo. Be
aware that the local git repo is changed, and you must manually revert all changes. The `--dryRun` 
option is used to test the script.

If the script fails to rebase/compile one of the extension branches or PRS, you can resolve the
problem/conflict and resume the script by running it again. Remember to commit you changes before
you rerun the script.

> **Tip!** If you have conflicts in documentation files, then consider running the test. The tests
> will regenerate the correct documentation file. After the test is run and new documentation is
> generated you mark the conflict as resolved.

If a conflic happens in the CI/CD pipline it is recomended to fix the branch causing the conflict.
The conflict can normally be fixed by rebasing or merging the extension branches or PRs. If not,
you will have to make the release on a local mashine resolving conflicts by hand.


## How-to make a hot-fix release 🔥

Sometimes it is necessary to roll out a new fix as fast as possible and with minimum risk. You can 
do this by applying the new “hot-fix” on top of the an old/latest release, and then make a new 
release. A hot-fix release can normally be rolled out without waiting for new graphs to build, 
since the serialization version number is the same.

1. Find out what the current OTP version is.
2. Check out the `release branch`, pull the latest version. You may have to reset it to the 
   version in the production environment. 
3. Cherry-pick or fix the problem. 
4. Run tests.
5. Complete the release by running the `script/custom-release.py --release`. 


## How-to roll back, by rolling forward 🔥

You can roll-back in two ways:

1. Find the commit you want to use as the base in the base repository and run the script:
   `script/custom-release.py <commit hash>`. This will create a new release using the
   `<commit hash>` as the base. 
2. Reset the release brach to a specific commit. Apply your changes. Merge in config
   branches if nessessary, and run `script/custom-release.py --release`. This is similar to making
   a hot-fix.


## How-to print a changelog summary

To add a summary in the CI/CD Pipline you can run the script standing at the tagged version you
want to produce the summary for. This will list changes in the _changelog.md_ and all PRs merged
into the current release. This will only compare the release with the previous version, if you want 
compare with another version you will need to use the `changelog-diff.py` script. The changelog 
script only make a diff on the changelog, it does not include the PRs.

```
# script/custom-release.py --summary
```
The report look like this:

> # OTP Release Summary
>
> ## Version
>
> - New version/git tag: `2.7.0-entur-68`
> - New serialization version: `EN-0087`
> - Old serialization version: `EN-0087`
>
>
> ## Pull Requests
> These PRs are tagged with Entur Test.
>
> - Fix ClassCastExcaption in Itinerary mapper [#6455](https://github.com/opentripplanner/OpenTripPlanner/pull/6455) [`Entur Test`]
> 
> ## Changelog v2.7.0-entur-67 vs v2.7.0-entur-68
>
> ### Added PRs
> - Disable OptimizeTransfers for via search temporarily. [#6449](https://github.com/opentripplanner/OpenTripPlanner/pull/6449)


