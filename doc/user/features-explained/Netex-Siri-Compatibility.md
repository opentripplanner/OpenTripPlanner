# NeTEx and SIRI compatibility

NeTEx and SIRI are two European public transport data specifications that are comparable to GTFS and
GTFS-RT but have a broader scope. Support for both was added by Entur in OTP version 2 and you can
find examples of those in their [examples repo](https://github.com/entur/profile-examples).

## Profiles

### Nordic profile

Different countries are currently using different incompatible "profiles" of NeTEx, but an effort is
underway to converge on a single European standard profile. This is based in large part on the
Nordic profile used by Entur.

The Nordic profile is the only one that has been thoroughly tested in production in OTP and is 
used in Norway, Finland and Sweden.

### EPIP

The [European Passenger Information Profile](http://netex.uk/netex/doc/2019.05.07-v1.1_FinalDraft/prCEN_TS_16614-PI_Profile_FV_%28E%29-2019-Final-Draft-v3.pdf) 
is an attempt to unify other country profiles and support in OTP is adequate, but it is difficult 
to tell how much of EPIP is supported since it is a very large profile. The current status
of the support is tracked on [Github](https://github.com/opentripplanner/OpenTripPlanner/issues/3640).

Sometimes it is not easy to figure out if a file conforms to EPIP so to find out, you can run the following
commands:

```
git clone git@github.com:NeTEx-CEN/NeTEx-Profile-EPIP.git
xmllint --noout --schema NeTEx-Profile-EPIP/NeTEx_publication_EPIP.xsd your-filename.xml
```

### Other profiles

It is the goal of the community to support both the Nordic profile and EPIP. If you have another
profile, we encourage to get in touch with the community to find a way forward.