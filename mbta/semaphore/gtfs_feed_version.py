#/usr/bin/env python
import csv
import zipfile
import io

with zipfile.ZipFile("var/graphs/mbta/1_MBTA_GTFS.zip", "r") as zf:
    with zf.open("feed_info.txt") as feed_info:
        feed_info = io.TextIOWrapper(feed_info)
        for row in csv.DictReader(feed_info):
            print(row["feed_version"])
