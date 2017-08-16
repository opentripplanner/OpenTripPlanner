#!/usr/bin/env python3
"""Github issue adder

This script adds links to GitHub issues if they appear in text.
Issues can appear in lines which start with - and can be in () brackets or
without brackets. They need to start with #.

Script replaces those issues with Markdown link markup.
Script can replace existing files or create new file. By default new file is
created. Script is idempotent. It can be run on already replaced files and it
won't replace same issues again.

"""
import argparse
import re
import os
import io

#Regex searches for all #numbers with optional brackets arround It skipps issue
#numbers in [] brackes Since those are already replaced links.
regex = re.compile(r"\(?(?<!\[)#(?P<issue_number>\d+)\)?")

ISSUE_URL = "https://github.com/opentripplanner/OpenTripPlanner/issues/"

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('-i', '--input_fn', action='store', default='Changelog.md',
        type=argparse.FileType('r'),
        help='Input filename Default:Changelog.md', dest='input_file')

parser.add_argument('-o', '--output_fn', action='store',
        help='Output filename (by default _new is added to input filename',
        type=argparse.FileType('w'),
        dest='output_file')

parser.add_argument('-r', '--replace', action='store_true', default=False, dest='replace',
        help='Replace existing input file with updated file.')

parser.add_argument('-u', '--url', action='store', default=ISSUE_URL,
        dest='issue_url', help='Issue URL Default:'+ISSUE_URL)

results = parser.parse_args()

"""Adds _new to filename before extension"""
def create_output_fn(input_fn):
    path, ext = os.path.splitext(input_fn)
    return "".join([path, "_new", ext])

input_fn = results.input_file.name

if results.replace:
    results.output_file = io.StringIO()
else:
    if results.output_file is None:
        output_fn = create_output_fn(input_fn)
        results.output_file = open(output_fn, "w")
    else:
        output_fn = results.output_file.name

if results.replace:
    print("Reading and replacing issue with links to URL:{} in file: {}".format(results.issue_url, input_fn))
else:
    print("Reading file: {} and replacing issue with links to URL:{} and writing to {}".format(
            input_fn, results.issue_url, output_fn))

# \g<issue_number> is replaced with issue number This creates Markdown link to
# Github issue number
FULL_ISSUE_URL = "[#\g<issue_number>]({}\g<issue_number>)".format(ISSUE_URL)

with results.input_file as in_file, \
    results.output_file as out_file:
    for line in in_file:
        if line.startswith("-"):
            # replaces #issue number with link to Github issue with regex
            # replacement
            changed=regex.sub(FULL_ISSUE_URL,
                    line)
            out_file.write(changed)
        else:
            out_file.write(line)

    #If we are replacing existing file
    if results.replace:
        in_file.close()
        # Writes to input file from memory out_file
        with open(input_fn, "w") as out_file1:
            out_file1.write(out_file.getvalue())
