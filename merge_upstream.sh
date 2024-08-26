#!/usr/bin/env bash

# Pulls upstream state (usually this should be opentripplanner/opentripplanner/dev-2.x)
# Rebases local extension branches on top of pulled upstream and pushes rebased extensions
# Replaces upstream CI actions with digitransit setup
# Pushes the result (upstream + local modifications) to digitransit OTP dev-2.x branch
# To apply this script, you need to set up upstream opentripplanner as remote (say a remote called otp)
# IMPORTANT: make sure that digitransit origin is the default repository:
#     git config checkout.defaultRemote origin
# First fetch the upstream: git fetch otp
# Then run ./merge_upstream.sh otp/dev-2.x
# The result is that updated dev-2.x is pushed to our fork and gets deployed to development.
# You can test the merge as follows:   ./merge_upstream.sh --dryRun otp/dev-2.x

#set -euo pipefail

DEVBRANCH=dev-2.x
REMOTE_REPO=$(git remote -v  | grep -i "hsldevcom/OpenTripPlanner" | grep "push" | awk '{print $1;}')
STATUS_FILE=".merge_upstream.tmp"
STATUS=""
DRY_RUN=""
OTP_BASE=""

# bash colored output control characters
BASH_GREEN="\e[32m"
BASH_YELLOW="\e[33m"
BASH_RED="\e[31m"
BASH_ENDFORMAT="\e[0m"
BOLD="\e[1m"

echo_green () {
    echo -e "$BASH_GREEN$1$BASH_ENDFORMAT"
}

echo_yellow () {
    echo -e "$BASH_YELLOW$1$BASH_ENDFORMAT"
}

echo_red () {
    echo -e "$BASH_RED$1$BASH_ENDFORMAT"
}

echo_bold () {
    echo -e "$BOLD$1$BASH_ENDFORMAT"
}

function main() {
    setup "$@"
    resumePreviousExecution
    resetDevelop
    # digitransit currently has no extension features
    # rebaseAndMergeExtBranch digitransit_ext_features
    configDigitransitCI
    logSuccess
}

function setup() {
    if [[ $# -eq 2 &&  "$1" == "--dryRun" ]] ; then
      DRY_RUN="--dryRun"
      OTP_BASE="$2"
    elif [[ $# -eq 1 ]] ; then
      OTP_BASE="$1"
    else
        printHelp
        exit 1
    fi

    echo ""
    echo "Options: ${DRY_RUN}"
    echo "Git base branch/commit: ${OTP_BASE}"
    echo "Digitransit development branch: ${DEVBRANCH}"
    echo "Digitransit remote repo(pull/push): ${REMOTE_REPO}"
    echo ""

    if git diff-index --quiet HEAD --; then
       echo ""
       echo "OK - No local changes, prepare to checkout '${DEVBRANCH}'"
       echo ""
    else
       echo ""
       echo_red "You have local modification, the script will abort. Nothing done!"
       echo ""
       exit 2
    fi

    git fetch "${REMOTE_REPO}"
}

# This script create a status file '.merge_upstream.tmp'. This file is used to resume the
# script in the same spot as where is left when the error occurred. This allow us to fix the
# problem (merge conflict or compile error) and re-run the script to complete the proses.
function resumePreviousExecution() {
    readStatus

    if [[ -n "${STATUS}" ]] ; then
        echo ""
        echo "Resume: ${STATUS}?"
        echo ""
        echo "    If all problems are resolved you may continue."
        echo "    Exit to clear status and start over."
        echo ""

        ANSWER=""
        while [[ ! "$ANSWER" =~ [yx] ]]; do
            echo "Do you want to resume: [y:Yes, x:Exit]"
            read ANSWER
        done

        if [[ "${ANSWER}" == "x" ]] ; then
            exit 0
        fi
    fi
}

function resetDevelop() {
    echo ""
    echo_bold "## ------------------------------------------------------------------------------------- ##"
    echo_bold "##   RESET '${DEVBRANCH}' TO '${OTP_BASE}'"
    echo_bold "## ------------------------------------------------------------------------------------- ##"
    echo ""
    echo "Would you like to reset the '${DEVBRANCH}' to '${OTP_BASE}'? "
    echo ""

    whatDoYouWant

    if [[ "${ANSWER}" == "y" ]] ; then
      echo ""
      echo "Checkout '${DEVBRANCH}'"
      git checkout ${DEVBRANCH}

      echo ""
      echo "Reset '${DEVBRANCH}' branch to '${OTP_BASE}' (hard)"
      git reset --hard "${OTP_BASE}"
      echo ""
    fi
}

function rebaseAndMergeExtBranch() {
    EXT_BRANCH="$1"
    EXT_STATUS_REBASE="Rebase '${EXT_BRANCH}'"
    EXT_STATUS_COMPILE="Compile '${EXT_BRANCH}'"

    echo ""
    echo_bold "## ------------------------------------------------------------------------------------- ##"
    echo_bold "##   REBASE AND MERGE '${EXT_BRANCH}' INTO '${DEVBRANCH}'"
    echo_bold "## ------------------------------------------------------------------------------------- ##"
    echo ""
    echo "You are about to rebase and merge '${EXT_BRANCH}' into '${DEVBRANCH}'. Any local"
    echo "modification in the '${EXT_BRANCH}' will be lost."
    echo ""

    whatDoYouWant

    if [[ "${ANSWER}" == "y" ]] ; then
        echo ""
        echo "Checkout '${EXT_BRANCH}'"
        git checkout "${EXT_BRANCH}"

        echo ""
        echo "Reset to '${REMOTE_REPO}/${EXT_BRANCH}'"
        git reset --hard "${REMOTE_REPO}/${EXT_BRANCH}"

        echo ""
        echo "Top 2 commits in '${EXT_BRANCH}'"
        echo "-------------------------------------------------------------------------------------------"
        git --no-pager log -2
        echo "-------------------------------------------------------------------------------------------"
        echo ""
        echo "You are about to rebase the TOP COMMIT ONLY(see above). Check that the "
        echo "'${EXT_BRANCH}' only have ONE commit that you want to keep."
        echo ""

        whatDoYouWant

        if [[ "${ANSWER}" == "y" ]] ; then
            echo ""
            echo "Rebase '${EXT_BRANCH}' onto '${DEVBRANCH}'"
            setStatus "${EXT_STATUS_REBASE}"
            git rebase --onto ${DEVBRANCH} HEAD~1
        fi
    fi

    if [[ "${STATUS}" == "${EXT_STATUS_REBASE}" || "${STATUS}" == "${EXT_STATUS_COMPILE}" ]] ; then
        # Reset status in case the test-compile fails. We need to do this because the status file
        # is deleted after reading the status in the setup() function.
        setStatus "${EXT_STATUS_COMPILE}"

        mvn clean test-compile
        clearStatus

        echo ""
        echo "Push '${EXT_BRANCH}'"
        if [[ -z "${DRY_RUN}" ]] ; then
          git push -f
        else
          echo "Skip: git push -f   (--dryRun)"
        fi

        echo ""
        echo "Checkout '${DEVBRANCH}' and merge in '${EXT_BRANCH}'"
        git checkout "${DEVBRANCH}"
        git merge "${EXT_BRANCH}"
    fi
}

function configDigitransitCI() {
    git checkout "${DEVBRANCH}"
    rm -rf .github
    git checkout origin/digitransit_ext_config .github
    git commit -a -m "Configure Digitransit CI actions"
    if [[ -z "${DRY_RUN}" ]] ; then
        git push -f
    fi
}

function logSuccess() {
    echo ""
    echo_green "## ------------------------------------------------------------------------------------- ##"
    echo_green "##   UPSTREAM MERGE DONE  --  SUCCESS"
    echo_green "## ------------------------------------------------------------------------------------- ##"
    echo "   - '${REMOTE_REPO}/${DEVBRANCH}' reset to '${OTP_BASE}'"
    echo "   - 'digitransit_ext_config' CI features added"
    echo ""
    echo ""
}

function whatDoYouWant() {
    echo ""
    ANSWER=""

    if [[ -n "${STATUS}" ]] ; then
      # Skip until process is resumed
      ANSWER="s"
    else
      while [[ ! "$ANSWER" =~ [ysx] ]]; do
        echo_yellow "Do you want to continue: [y:Yes, s:Skip, x:Exit]"
        read -r ANSWER
      done

      if [[ "${ANSWER}" == "x" ]] ; then
        exit 0
      fi
    fi
}

function setStatus() {
    STATUS="$1"
    echo "$STATUS" > "${STATUS_FILE}"
}

function readStatus() {
    if [[ -f "${STATUS_FILE}" ]] ; then
        STATUS=$(cat $STATUS_FILE)
        rm "$STATUS_FILE"
    else
         STATUS=""
    fi
}

function clearStatus() {
    STATUS=""
    rm "${STATUS_FILE}"
}

function printHelp() {
    echo ""
    echo -e "This script takes one argument, the base **${BOLD}branch${BASH_ENDFORMAT}** or **${BOLD}commit${BASH_ENDFORMAT}** to use for the"
    echo "release. The '${DEVBRANCH}' branch is reset to this commit and then the extension"
    echo "branches are rebased onto that. "
    echo "It tags and pushes all changes to remote git repo."
    echo ""
    echo_bold "Options:"
    echo ""
    echo "   --dryRun : Run script locally, nothing is pushed to remote server."
    echo ""
    echo_bold "Usage:"
    echo ""
    echo "  $ ./merge_upstream.sh otp/dev-2.x"
    echo "  $ ./merge_upstream.sh --dryRun otp/dev-2.x"
    echo ""
}

main "$@"
