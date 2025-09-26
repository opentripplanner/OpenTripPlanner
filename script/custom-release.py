#!/usr/bin/env python3

import json
import os
import re
import subprocess

import sys

## ------------------------------------------------------------------------------------ ##
##                                 Global constants                                     ##
## ------------------------------------------------------------------------------------ ##

POM_FILE_NAME = 'pom.xml'
# GitHub label to indicate that a PR needs to be bumped
LBL_BUMP_SER_VER_ID = '+Bump Serialization Id'
SER_VER_ID_PROPERTY = 'otp.serialization.version.id'
SER_VER_ID_PROPERTY_PTN = SER_VER_ID_PROPERTY.replace('.', r'\.')
SER_VER_ID_PATTERN = re.compile(
    '<' + SER_VER_ID_PROPERTY_PTN + r'>\s*(.*)\s*</' + SER_VER_ID_PROPERTY_PTN + '>')
OTP_GITHUB_PULLREQUEST_URL = "https://github.com/opentripplanner/OpenTripPlanner/pull/"
STATE_FILE = '.custom_release_resume_state.json'
SUMMARY_FILE = '.custom_release_summary.md'


## ------------------------------------------------------------------------------------ ##
##                                      Classes                                         ##
## ------------------------------------------------------------------------------------ ##

# Configuration read from the 'custom-release-env.json' file
class Config:
    def __init__(self):
        self.upstream_remote = None
        self.release_remote = None
        self.release_branch = None
        self.ext_branches = None
        self.include_prs_label = None
        self.ser_ver_id_prefix = None
        self.otp_production_url = None

    def release_path(self, branch):
        return f'{self.release_remote}/{branch}'

    def release_branch_path(self):
        return self.release_path(self.release_branch)

    def __str__(self):
        return (f"<"
                f"upstream_remote: '{self.upstream_remote}', "
                f"release_remote: '{self.release_remote}', "
                f"release_branch: '{self.release_branch}', "
                f"ext_branches: {self.ext_branches}, "
                f"include_prs_label: '{self.include_prs_label}', "
                f"ser_ver_id_prefix: '{self.ser_ver_id_prefix}'"
                f"otp_production_url: '{self.otp_production_url}'>")


# CLI Arguments and Options
class CliOptions:

    def __init__(self):
        self.base_revision = None
        self.dry_run = False
        self.debugging = False
        self.release_only = False
        self.bump_ser_ver_id = False
        self.skip_prs = False
        self.print_summary = False

    def verify(self):
        if self.release_only and self.base_revision:
            error(f"<base-revision> is not allowed with option '--release', was: {options.base_revision}")

    # Return the script <base revision> argument if set, if not use 'HEAD'(--release)
    def release_base(self):
        return self.base_revision if self.base_revision else 'HEAD'

    def release_base_git_hash(self):
        return git_commit_hash(self.release_base())

    def __str__(self):
        return (f"<"
                f"base_revision: {self.base_revision}, "
                f"dry_run: {self.dry_run}, "
                f"debugging: {self.debugging}, "
                f"release_only: '{self.release_only}', "
                f"bump_ser_ver_id: '{self.bump_ser_ver_id}', "
                f"skip_prs: '{self.skip_prs}', "
                f"print_summary: '{self.print_summary}'>")

# PR information
class PullRequest:
    def __init__(self):
        self.number = None
        self.title = None
        self.commit_hash = None
        self.labels = []
        self.ser_label_set = False

    def description(self):
        return f'{self.title} #{self.number}'

    def description_w_labels(self):
        return f'{self.description()} {self.labels}'

    def description_link(self):
        return f"[{self.description()}]({OTP_GITHUB_PULLREQUEST_URL}{self.number}) {self.labels}".replace("'", "`")

# The execution state of the script + the CLI arguments
class ScriptState:

    def __init__(self):
        self.latest_ser_ver_id = None
        self.next_ser_ver_id = None
        self.major_version = None
        self.latest_version = None
        self.next_version = None
        self.prs_bump_ser_ver_id = False
        self.goto_step = False
        self.step = None
        self.production_version = None
        self.production_ser_ver_id = None

    def latest_version_tag(self):
        return f'v{self.latest_version}'

    def latest_version_git_hash(self):
        return git_commit_hash(self.latest_version_tag())

    def next_version_tag(self):
        return f'v{self.next_version}'

    def next_version_description(self):
        return f'Version {self.next_version} ({self.next_ser_ver_id})'

    def production_version_tag(self):
        return f'v{self.production_version}'

    def is_ser_ver_id_next(self):
        return self.next_ser_ver_id != self.latest_ser_ver_id

    def run(self, step):
        if not self.goto_step:
            debug(f'Run step: {step}')
            return True
        if self.step == step:
            self.goto_step = False
            debug(f'Resume step: {step}')
            return True
        else:
            debug(f'Skip step: {step}')
            return False

    @staticmethod
    def do_resume():
        return os.path.exists(STATE_FILE)

    def delete_progress_file(self):
        execute('rm', STATE_FILE)
        self.goto_step = False
        self.step = None


## ------------------------------------------------------------------------------------ ##
##                                  Global Variables                                    ##
## ------------------------------------------------------------------------------------ ##

config: Config = Config()
options = CliOptions()
state = ScriptState()
pullRequests = []


## ------------------------------------------------------------------------------------ ##
##                                        Main                                          ##
## ------------------------------------------------------------------------------------ ##
def main():
    setup_and_verify()

    # Prepare release
    if not options.release_only:
        reset_release_branch_to_base_revision()
        merge_in_labeled_prs()
        merge_in_ext_branches()
        merge_in_old_release_with_no_changes()
        run_custom_release_extensions()

    merge_in_old_release_with_no_changes()
    set_maven_pom_version()
    set_ser_ver_id()
    run_maven_test()
    commit_next_versions()
    tag_release()
    push_release_branch_and_tag()
    print_summary()


## ------------------------------------------------------------------------------------ ##
##                                 Top level functions                                  ##
## ------------------------------------------------------------------------------------ ##

def setup_and_verify():
    section('Setting up release process and verifying the environment')
    parse_and_verify_cli_arguments_and_options()
    if state.do_resume():
        resume()
    else:
        load_config()
        verify_script_run_from_root()
        verify_git_installed()
        verify_maven_installed()
        fetch_all_git_remotes()
        verify_release_base_and_release_branch_exist()
        verify_no_local_git_changes()
        resolve_version_number()
        resolve_next_version()
        resolve_latest_ser_ver_id()
        read_pull_request_info_from_github()
        check_if_prs_exist_in_latest_release()
        resolve_next_ser_ver_id()
        resolve_production_versions()
    print_setup()


def reset_release_branch_to_base_revision():
    section('Reset release branch to base revision ...')
    # Fetching is normally not necessary, but the default depth is often just the top commit in a
    # CI environment. We need tags and ser.ver.id from older commits and common ancestor commit
    # when merging . If git fails with "fatal: refusing to merge unrelated histories" the depth
    # is not deep enough.
    git('fetch', config.release_remote, config.release_branch)
    git('checkout', '-B', config.release_branch, config.release_branch_path())
    git('reset', '--hard', options.base_revision)


def merge_in_labeled_prs():
    section('Merge in labeled PRs ...')
    for pr in pullRequests:
        # A temp branch is needed here since the PR is in the upstream remote repo
        temp_branch = f'pull-request-{pr.number}'
        if section_w_resume(temp_branch, f'Merge in PR {pr.description()}'):
            git('fetch', config.upstream_remote, f'pull/{pr.number}/head:{temp_branch}')
            git('merge', temp_branch)
            git('branch', '-D', temp_branch)


def merge_in_ext_branches():
    if len(config.ext_branches) == 0:
        info('\nNo config branch configured, mering is skipped.')
        return
    if section_w_resume('merge_in_ext_branches', 'Merge in config branch ...'):
        for branch in config.ext_branches:
            # See reset_release_branch_to_base_revision() for why wee ned to fetch.
            git('fetch', config.release_remote, branch)
            git('merge', config.release_path(branch))
            info(f'Config branch merged: {config.release_path(branch)}')


# Merge the old version into the next version. This only keep a reference to the old version, the
# resulting git tree of the merge is that of the new branch head, effectively ignoring all changes
# from the old release. This creates a continuous line of releases in the release branch.
def merge_in_old_release_with_no_changes():
    section('Merge old release into the release branch ...')
    git('merge', '-s', 'ours', config.release_branch_path(), '-m',
        'Merge old release into the release branch - NO CHANGES COPIED OVER')
    info('Merged - NO CHANGES COPIED OVER')


def run_custom_release_extensions():
    section('Run custom release extensions bash script ...')
    ext_script = 'script/custom-release-extension'
    if not os.path.exists(ext_script):
        warn(f"Script '{ext_script}' not found!")
    else:
        p = subprocess.run(ext_script, text=True, timeout=20)
        if p.returncode != 0:
            error(f'Custom script {ext_script} failed. Return code: {p.returncode}')


def set_maven_pom_version():
    section('Set Maven project version ...')
    execute('mvn', 'versions:set', f'-DnewVersion={state.next_version}',
            '-DgenerateBackupPoms=false', quiet=True)
    info(f'New version set: {state.next_version}')


def set_ser_ver_id():
    section('Set serialization.version.id ...')
    next_ser_ver_id_element = f'<{SER_VER_ID_PROPERTY}>{state.next_ser_ver_id}</{SER_VER_ID_PROPERTY}>'

    with open(POM_FILE_NAME, 'r') as input_stream:
        pom_file = input_stream.read()
        pom_file = re.sub(SER_VER_ID_PATTERN, next_ser_ver_id_element, pom_file, count=1)
    with open(POM_FILE_NAME, 'w') as output:
        output.write(pom_file)
    prefix = 'New' if (state.is_ser_ver_id_next()) else 'Same'
    info(f'{prefix} serialization.version.id set: {state.next_ser_ver_id}')


def commit_next_versions():
    section('Commit pom.xml with next version and serialization version id set ...')
    git('add', '.')
    git_dr('commit', '--all', '-m', state.next_version_description())
    info(f'Commit done: {state.next_version_description()}')


def tag_release():
    section(f'Tag release with {state.next_version} ...')
    git_im('tag', '-a', state.next_version_tag(), '-m', state.next_version_description())
    info(f'Tag done: {state.next_version_tag()}')


def push_release_branch_and_tag():
    section('Push new release with pom.xml changes and new tag')
    git_dr('push', '-f', f'{config.release_remote}', f'{config.release_branch}')
    git_dr('push', '-f', f'{config.release_remote}', f'v{state.next_version}')
    info(f'Release pushed to: {config.release_branch_path()}')
    delete_script_state()
    info('\nRELEASE SUCCESS!\n')


def print_summary():
    if not options.print_summary:
        return
    section(f'Print summary file: {SUMMARY_FILE}')

    release_version_summary = f"""
# OTP Release Summary

## Version

  - New version/git tag: `{state.next_version}` 
  - Production version/git tag : `{state.production_version}` 
  - New serialization version: `{state.next_ser_ver_id}` 
  - Old serialization version: `{state.latest_ser_ver_id}`
  - Prod serialization version: `{state.production_ser_ver_id}`

"""
    with open(SUMMARY_FILE, mode="w", encoding="UTF-8") as f:
        print(release_version_summary, file=f)
        if len(pullRequests) > 0:
            print("## Pull Requests", file=f)
            print(f"These PRs are tagged with {config.include_prs_label}.\n", file=f)
        for pr in pullRequests:
            print(f"  -  {pr.description_link()}", file=f)

        if state.production_version:
            p = execute(
                "./script/changelog-diff.py",
                state.production_version_tag(),
                state.latest_version_tag(),
                "Changelog production 🦋"
            )
            print(p.stdout, file=f)

        p = execute(
            "./script/changelog-diff.py",
            state.latest_version_tag(),
            state.next_version_tag(),
            "Changelog previous release 🐛"
        )
        print(p.stdout, file=f)


## ------------------------------------------------------------------------------------ ##
##                                   Setup and verify                                   ##
## ------------------------------------------------------------------------------------ ##

def parse_and_verify_cli_arguments_and_options():
    if os.getenv('CUSTOM_RELEASE_LOG_LEVEL') == 'debug':
        options.debugging = True
    args = []
    for arg in sys.argv[1:]:
        if re.match(r'(-h|--help)', arg):
            print_help()
        elif re.match(r'(--dryRun)', arg):
            options.dry_run = True
        elif re.match(r'(--debug)', arg):
            options.debugging = True
        elif re.match(r'(--release)', arg):
            options.release_only = True
        elif re.match(r'(--serVerId)', arg):
            options.bump_ser_ver_id = True
        elif re.match(r'(--skipPRs)', arg):
            options.skip_prs = True
        elif re.match(r'(--summary)', arg):
            options.print_summary = True
        else:
            options.base_revision = arg
            args.append(arg)
    if len(args) > 1:
        error(f'Expected one argument, <base-revision>, but got: {args}')
    options.verify()


def resume():
    section('Resume')
    print(f'''
      Do you want to resume the previous execution of the script?
       - You must first fix the merge conflict or unit-test failing.
       - Then commit your changes.
       - Then run this script again, the script will automatically resume the release
         process at the same place it failed.
         
      | NOTE!  The script entered resume because a progress tracking file exist. To
      |        avoid this you may delete the {STATE_FILE} before starting the script.
      |
      |        > rm {STATE_FILE}
      
      Press
        'y' to resume release.
        'x' to exit.
        'd' to exit and delete progress file. 
    ''')
    answer = 'NOT_SET'
    p = re.compile(r'[\syYxXdD]')
    while not p.match(answer):
        answer = input("Press 'x', 'y', or 'd' then <enter>: ").lower()
    if answer == 'x':
        exit(0)
    if answer == 'd':
        state.delete_progress_file()
        exit(0)
    read_script_state()


def load_config():
    section('Load configuration...')
    with open('script/custom-release-env.json', 'r') as f:
        doc = json.load(f)
        config.upstream_remote = doc['upstream_remote']
        config.release_remote = doc['release_remote']
        config.release_branch = doc['release_branch']
        config.ext_branches = doc['ext_branches']
        config.include_prs_label = doc['include_prs_label']
        config.ser_ver_id_prefix = doc['ser_ver_id_prefix']
        config.otp_production_url = doc['otp_production_url']
        debug(f'Config loaded: {config}')

    if len(config.ser_ver_id_prefix) > 2:
        error(f"Configure the 'ser_ver_id_prefix'. The prefix must be maximum two characters long. "
              f"Value: <{config.ser_ver_id_prefix}>")


def verify_script_run_from_root():
    # Assume we are in the root directory if the 'LICENSE' file exist
    execute('ls', 'LICENSE', error_msg=f'Run script from project root directory.')


def verify_git_installed():
    execute('git', '--version', quiet=False)


def verify_maven_installed():
    execute('mvn', '--version')


def verify_release_base_and_release_branch_exist():
    if options.release_only:
        info('Verify release branch/commit exist ...')
    else:
        info('Verify base revision and release branch/commit exist ...')
        git('rev-parse', '--quiet', '--verify', options.release_base(),
            error_msg='Base revision not found!')
    git('rev-parse', '--quiet', '--verify', config.release_branch_path(),
        error_msg='Release branch not found!')


def verify_no_local_git_changes():
    info('Verify no local changes exist ...')
    git('diff-index', '--quiet', 'HEAD', error_msg='There are local changes!')


# Make sure all remote repos are up-to date
def fetch_all_git_remotes():
    git('fetch', '--all', error_msg='Git fetch all remotes failed!')


def resolve_version_number():
    info(f'Resolve version number ...')
    version_qualifier = config.release_remote
    state.major_version = read_major_version_from_pom(version_qualifier, options.release_base())


def resolve_next_version():
    info('Resolve next version number ...')
    p = git('tag', '--list', '--sort=-v:refname', error_msg='Fetch git tags failed!')
    tags = p.stdout.splitlines()

    prefix = f'{state.major_version}-{config.release_remote}-'
    pattern = re.compile('v' + prefix.replace('.', r'\.') + r'(\d+)')
    max_tag_version = max((int(m.group(1)) for tag in tags if (m := pattern.match(tag))), default=0)
    state.latest_version = prefix + str(max_tag_version)
    state.next_version = prefix + str(1 + max_tag_version)


def resolve_latest_ser_ver_id():
    info('Resolve last ser.ver.id ...')
    p = git('tag', '--list', '--sort=-v:refname', error_msg='Fetch git tags failed!')
    all_tags = p.stdout.splitlines()
    prefix = f'{state.major_version}-{config.release_remote}-\\d+'
    pattern = re.compile('v' + prefix.replace('.', r'\.') + r'\d+')
    tags = [item for item in all_tags if re.search(pattern, item)]
    tags = tags[:60]
    maxSId = ' '
    for tag in tags:
        maxSId = max(maxSId, read_ser_ver_id_from_pom_file(tag))

    state.latest_ser_ver_id = maxSId


def read_pull_request_info_from_github():
    if options.skip_prs or (not config.include_prs_label):
        info('Skip merging in GitHub PRs.')
        return
    info('Get PRs to include and their labels from GitHub - This requires authentication ...')


    query_text = '''
    query ReadOpenPullRequests {
      repository(owner:\\"opentripplanner\\", name:\\"OpenTripPlanner\\") {
        pullRequests(first: 100, states: OPEN, labels: \\"''' + config.include_prs_label + '''\\") {
          nodes {
            number, 
            title, 
            headRefOid,
            labels(first: 20) {
              nodes {
                name
              }
            }
          }
        }
      }
    }
    '''

    # The query body needs to be on one line, for an unknown reason. Replace 2 or more whitespace
    # with a singe space.
    query_text = re.sub(r'(?s)\s{2,}', ' ', query_text)
    post_body = '''
    {
      "query":"''' + query_text + '''",
      "operationName":"ReadOpenPullRequests"
    }
    '''
    git_hub_access_token = os.environ['CUSTOM_RELEASE_GIT_HUB_API_TOKEN']
    result = execute('curl', '-H', f'Authorization: Bearer {git_hub_access_token}', '-X', 'POST',
                     '-d', post_body, 'https://api.github.com/graphql',
                     error_msg='GitHub GraphQL Query failed!', quiet_err=True)

    # Example response
    #   {"data":{"repository":{"pullRequests":{"nodes":[{"number":2222,"labels":{"nodes":[{"name":"+Bump Serialization Id"},{"name":"Entur Test"}]}}]}}}}
    json_doc = json.loads(result.stdout)
    defined_labels = [LBL_BUMP_SER_VER_ID.lower(), config.include_prs_label.lower()]

    for node in json_doc['data']['repository']['pullRequests']['nodes']:
        pr = PullRequest()
        pr.number = node['number']
        pr.title = node['title']
        pr.commit_hash = node['headRefOid']
        labels = node['labels']['nodes']
        # GitHub labels are not case-sensitive, hence using 'lower()'
        for label in labels:
            lbl_name = label['name']
            lbl_name_lc= lbl_name.lower()
            if lbl_name_lc in defined_labels:
                pr.labels.append(lbl_name)
        pullRequests.append(pr)


def check_if_prs_exist_in_latest_release():
    info(f'Check if one of the PRs labeled with {LBL_BUMP_SER_VER_ID} does not exist in the '
         f'latest release. If so, bump the ser.ver.id ...')
    latest_release_hash = state.latest_version_git_hash()

    for pr in pullRequests:
        if pr.ser_label_set and not git_is_commit_ancestor(pr.commit_hash, latest_release_hash):
            info(f'  - The top commit does not exist in the latest release. Bumping ser.ver.id. ({pr.description()})')
            state.prs_bump_ser_ver_id = True
            return


# The script will resolve what the next serialization version id (SID) should be. This is a complex
# task. The `latest_ser_ver_id` is allready resolved - to the highest existing id for all
# matching git tags. Here is an overview:

#  1. If the --serVerId option exist, then the latest SID is bumped and used.
#  2. If the --release option exist, then the current pom.xml SID is validated, if ok it is used,
#     if not the script exit.
#  3. All merged in PRs are checked. If a PR is labeled with '+Bump Serialization Id' and the the
#     HEAD commit is not in the latest release, then the last release SID is bumped and used.
#  4. Finally, the script look at the upstream Git Repo SIDs for both this release(base) and the
#     last release. If the SIDs are differnt the SID is bumped. To find the *upstream* SIDs we
#     look at the git history/log NOT matching the project serialization version id prefix - this
#     is assumed to be the latest SID upstream.
#
# Tip! If the '--release' option is used, then the serialization version id is NOT updated. Use the
# '--serVerId' option together with the '--release' to force update the serialization version id.
#
def resolve_next_ser_ver_id():
    info('Resolve the next serialization version id ...')

    if options.bump_ser_ver_id:
        state.next_ser_ver_id = bump_release_ser_ver_id(state.latest_ser_ver_id)
    elif options.release_only:
        current_ser_ver_id = read_ser_ver_id_from_pom_file("HEAD")
        if current_ser_ver_id.startswith(config.ser_ver_id_prefix):
            state.next_ser_ver_id = current_ser_ver_id
        else:
            error(f'The serialization-version-id ({current_ser_ver_id}) in the pom.xml must start with {config.ser_ver_id_prefix}.')
    elif state.prs_bump_ser_ver_id:
        state.next_ser_ver_id = bump_release_ser_ver_id(state.latest_ser_ver_id)
    else:
        latest_version_tag = state.latest_version_tag()
        info('  - Find upstream serialization version id for latest release ...')
        latest_upstream_ser_id = find_upstream_ser_ver_id_in_history(latest_version_tag)

        info(f'  - Find upstream serialization version id for base ...')
        base_hash = options.release_base_git_hash()
        base_upstream_ser_id = find_upstream_ser_ver_id_in_history(base_hash)

        # Update serialization version id in release if serialization version id has changed
        if latest_upstream_ser_id != base_upstream_ser_id:
            info(f'  - The latest upstream serialization.ver.id {latest_upstream_ser_id} '
                 f'and the base upstream id {base_upstream_ser_id} is diffrent. '
                 'The serialization.ver.id is bumped.')
            state.next_ser_ver_id = bump_release_ser_ver_id(state.latest_ser_ver_id)
        else:
            state.next_ser_ver_id = read_ser_ver_id_from_pom_file(latest_version_tag)


def resolve_production_versions():
    url = config.otp_production_url
    if not url:
        info("The 'otp_production_url' config parameter is not set. Summary diff is skipped.")
        return

    p = (execute('curl', url, error_msg=f'Unable to connect to: {url}', quiet_err=False))
    text = p.stdout

    # "version":"2.8.0-entur-160"
    match = re.search(r"\"version\":\"([-\w\.]+)\"", text)
    if match:
        state.production_version = match.group(1)

    # "otpSerializationVersionId":"EN-0111"
    match = re.search(r"\"otpSerializationVersionId\":\"([-\w\.]+)\"", text)
    if match:
        state.production_ser_ver_id = match.group(1)

# Find the serialization-version-id for the upstream git project using the git log starting
# with the given revision (abort if not found in previous 20 commits)
def find_upstream_ser_ver_id_in_history(revision: str):
    output = git('log', '-20', '--format=oneline', revision).stdout
    for line in output.splitlines():
        git_hash = line.split()[0]
        ser_ver_id = read_ser_ver_id_from_pom_file(git_hash)
        if not ser_ver_id.startswith(config.ser_ver_id_prefix):
            return ser_ver_id
    error(f'No ser.ver.id found for revision {revision} and previous 20 commits.')


def print_setup():
    section('Configuration')
    info(f'''
CLI Arguments
  - Base revision for release ... : {options.base_revision}

CLI Options
  - Bump ser.ver.id ............. : {options.bump_ser_ver_id}
  - Dry run  .................... : {options.dry_run}
  - Debugging ................... : {options.debugging}
  - Release ..................... : {options.release_only}

Config
  - Upstream git repo remote name : {config.upstream_remote}
  - Release to remote git repo .. : {config.release_remote}
  - Release branch .............. : {config.release_branch}
  - Configuration branches ...... : {config.ext_branches}
  - Ser.ver.id prefix ........... : {config.ser_ver_id_prefix}
  - Otp production url .......... : {config.otp_production_url}  
''')
    if config.include_prs_label:
        info(f'PRs to merge')
        for pr in pullRequests:
            info(f'  - {pr.description_w_labels()}')
    info(f'''
Release info
  - Project major version ....... : {state.major_version}
  - Latest version .............. : {state.latest_version}
  - Next version ................ : {state.next_version}
  - Prod version ................ : {state.production_version}
  - Latest ser.ver.id ........... : {state.latest_ser_ver_id}
  - Next ser.ver.id ............. : {state.next_ser_ver_id}
  - Prod ser.ver.id ............. : {state.production_ser_ver_id}
''')


## ------------------------------------------------------------------------------------ ##
##                                        Resume                                        ##
## ------------------------------------------------------------------------------------ ##

def read_script_state():
    info('The script will resume and print the state of the previous process.')
    global options, config, state, pullRequests
    with (open(STATE_FILE, 'r') as f):
        doc = json.load(f)
        options.__dict__ = doc['options']
        config.__dict__ = doc['config']
        state.__dict__ = doc['state']
        for it in doc['pull-requests']:
            pr = PullRequest()
            pr.__dict__ = it
            pullRequests.append(pr)
        state.goto_step = True


def save_script_state(step):
    state.step = step
    doc = {
        'options': options.__dict__,
        'config': config.__dict__,
        'state': state.__dict__,
        'pull-requests' : list(pr.__dict__ for pr in pullRequests)
    }
    with open(STATE_FILE, 'w') as f:
        json.dump(doc, f)

# Delete the script state file - this avoids going into resume the nest time the
# script is run.
def delete_script_state():
    execute('rm', '-f', STATE_FILE)


## ------------------------------------------------------------------------------------ ##
##                                   Utility functions                                  ##
## ------------------------------------------------------------------------------------ ##


def bump_release_ser_ver_id(latest_id):
    # The id format can be either 'A-00053' or 'AA-0053'
    if len(config.ser_ver_id_prefix) == 1:
        ver_number = int(latest_id[2:])
        ser_format = '{:05d}'
    else:
        ver_number = int(latest_id[3:])
        ser_format = '{:04d}'
    v = config.ser_ver_id_prefix + '-' + ser_format.format(ver_number + 1)
    debug(f'Next serialization version id: {v}')
    return v


def read_major_version_from_pom(version_qualifier: str, revision: str):
    pom_xml = execute('git', 'show', f'{revision}:{POM_FILE_NAME}').stdout
    token_ptn = re.compile(r'<version>(.*)</version>')
    ver_ptn = re.compile(r'(\d+\.\d+\.\d+)-(' + version_qualifier + r'-\d+|SNAPSHOT)')

    # Find the first <version>... in the pom_file.
    match = token_ptn.search(pom_xml)
    if match:
        debug(f'Version found: {match.group(1)}')
        match = ver_ptn.match(match.group(1).strip())
        if match:
            debug('Main version number: {m.group(1)}')
            return match.group(1)
    error(f"Version not found in '{POM_FILE_NAME}'.")


def read_ser_ver_id_from_pom_file(git_hash):
    pom_xml = execute('git', 'show', f'{git_hash}:{POM_FILE_NAME}').stdout
    m = SER_VER_ID_PATTERN.search(pom_xml)
    return m.group(1)


def run_maven_test():
    if section_w_resume('run_maven_test', 'Run unit tests'):
        mvn('clean', '-PprettierSkip', 'test')


# Get the full git hash for a qualified branch name, tag or hash
def git_commit_hash(ref):
    if re.compile(r'[0-9a-f]{40}').match(ref):
        return ref
    output = execute('git', 'show-ref', ref).stdout
    return output.split()[0]


def git_is_commit_ancestor(childCommit: str, parentCommit : str):
    p = subprocess.run("git", "merge-base" "--is-ancestor", childCommit, parentCommit, timeout=5)
    return p.returncode == 0


def git(*cmd, error_msg=None):
    return execute('git', *cmd, error_msg=error_msg)


# Git dru-run should be used if the command change or update the system, this command
# is NOT run if the '--dryRun' flag is set. Only some git commands has this flag, for example
# 'merge' and 'commit'.
def git_dr(*cmd, error_msg=None):
    if options.dry_run:
        return git(*cmd, '--dry-run', error_msg=error_msg)
    else:
        return git(*cmd, error_msg=error_msg)


# Similar as 'git_dr()', but the command is just printed, not executed with the git '--dry-run'
# flag set. This is required if the git command do not support the '--dry-run' flag. Note!
# Return None if the command is not run.
def git_im(*cmd, error_msg=None):
    if options.dry_run:
        info(f'=> {cmd}  (--dryRun SKIPPED)')
        return None
    return git(*cmd, error_msg=error_msg)


# Run maven and pipe the output to sdtout and stderr
def mvn(*cmd):
    cmd_line = ['mvn'] + list(cmd)
    info(f'Run: {cmd_line}')
    p = subprocess.run(cmd_line)
    if p.returncode:
        exit(p.returncode)


def execute(*cmd, quiet=True, quiet_err=False, error_msg=None):
    info(f'Run: {cmd}')
    p = subprocess.run(args=list(cmd), capture_output=True, text=True, timeout=20)

    if options.debugging:
        if p.stdout:
            debug(crop(p.stdout))
        if p.stderr:
            info(p.stderr)
        debug(f'  <= {p.returncode}')
    else:
        if p.stdout and (options.debugging or not quiet):
            info(p.stdout)
        if p.stderr and not quiet_err:
            info(p.stderr)
    if p.returncode:
        if error_msg:
            error(f'{error_msg} Command {cmd} failed.')
        else:
            error(f'Command {cmd} failed.')
    return p


## ------------------------------------------------------------------------------------ ##
##                                     Log functions                                    ##
## ------------------------------------------------------------------------------------ ##

def section_w_resume(step: str, msg: str) -> bool:
    if state.run(step):
        save_script_state(step)
        section(msg)
        return True
    else:
        save_script_state(step)
        section(f'{msg}  (SKIP STEP)')
        return False


def section(msg: str):
    hr = '-------------------------------------------------------------------------------------------'
    print('')
    print(hr)
    print(f'  {msg}')
    print(hr)


def info(msg):
    print(f'{msg}', flush=True)


def warn(msg):
    print(f'\nWARNING {msg}\n', flush=True)


def error(msg):
    print(f'\nERROR {msg}\n', flush=True)
    exit(1)


def debug(msg):
    if options.debugging:
        print(f'DEBUG {msg}', flush=True)


def crop(text):
    return text if len(text) <= 1600 else ("%s..." % (text[:1597]))


## ------------------------------------------------------------------------------------ ##
##                                     Help function                                    ##
## ------------------------------------------------------------------------------------ ##

def print_help():
    section('Help')
    print(f"""
    This script is used to create a new release in a downstream fork of OTP. It will set both
    the Maven version(2.7.0-entur-23) and the serialization-version-id(EN-0020) to a unique id
    using the provided configuration.

    Release process overview
      1. The configured release-branch is reset hard to the <base-revision> script argument.
      2. Then the labeled PRs are merged into the release-branch [if configured].
      3. The config-branches are merged into the release-branch [if configured].
      4. The pom.xml file is updated with a new version and serialization version id [if requiered].
      5. The release is tested, tagged and pushed to Git repo.

    See the RELEASE_README.md for more details.

    Usage
      script/custom-release.py [options] [<base-revision>]

    Arguments
        <base-revision> : The base branch or commit to use as the base for the release. The
                          'otp/dev-2.x' is the most common base branch to use, but you can create a
                          new release on top of any <commit>. This parameter is required unless
                          option --release is used.

    Options
      -h, --help : Print this help.
      --debug    : Run script with debug output enabled.
      --dryRun   : Run script locally, nothing is pushed to remote server.
      --release  : Create a new release from the checked out local Git repo HEAD. It updates the
                   maven-project-version, but NOT the serialization-version-id. To increment the 
                   serialization-version-id use the --serVerId option as well. This create a new 
                   tag and push the release. You should apply all fixes and commit BEFORE running
                   this script. Can not be used with the <base-revision> argument set. The 
                   'custom-release-extension' script is NOT run.
      --serVerId : Force incrementation of the serialization version id.
      --skipPRs  : Skip PRs labeled with the configured 'include_prs_label'.
      --summary  : Print a markdown summary to '.custom_release_summary.md'

    Examples
      # script/custom-release.py otp/dev-2.x --printSummary
      # script/custom-release.py 0715be88
      # script/custom-release.py --dryRun --debug entur/my-feature-branch
      # script/custom-release.py --release --serVerId


    Failure
      If a release fails, you may resume the release process after fixing the problem by running
      the custom-release script again. The script will automatically detect that the script failed
      and resume the release. Typical errors are merge conflicts and unit-test failures. Remember
      to commit you changes after the problem is fixed, before running the script.

      If you do not want to resume, and instead start over, delete the {STATE_FILE} and run the
      script again. You also need to delete the tag, if the release was tagged - this is last step
      before the release is pushed to the remote git repo.
    """)
    exit(0)


if __name__ == '__main__':
    main()
