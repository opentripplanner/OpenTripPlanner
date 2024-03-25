# Scripts

This folder is intended for various scripts used during the OTP development. They are provided 
"as is" and the "owner" may do whatever she/he likes with it. 

If you want to submit your own scripts, you need to include:
 - A header at the beginning of the script stating who the owner is.
 - The script should print some usage documentation if invoked with `--help` and `-h`.

The regular pull-request approval process is required for submitting new scripts and changing 
existing one. The reviewers are responsible for:
 - [ ] Is this script relevant for OTP and at least one active member of the OTP community?
 - [ ] Is the script harmful?
 - [ ] Does the script have sufficient documentation?
   - [ ] Owner section
   - [ ] Print help with `-h` and `--help`

### Example
```
# Owner: J. Brown, Fun & Fast Transit Inc

if [ "$1" == "-h" ]  || [ "$1" == "--help" ]; then
  echo "The purpose of the script is .."
  echo "Usage: ..."
  echo "Parameters: "
  :
fi

```

