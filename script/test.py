#!/usr/bin/env python3


def bump_release_ser_ver_id(current_id):
    # The id can be either 'N-00001' or 'EN-0001'
    if len(ser_ver_id_prefix) == 1:
        ver_number = int(current_id[2:])
        ser_format = '{:05d}'
    else:
        ver_number = int(current_id[3:])
        ser_format = '{:04d}'
    v = ser_ver_id_prefix + '-' + ser_format.format(ver_number + 1)
    print(f'New serialization version id: {v}')
    return v

ser_ver_id_prefix="E"
bump_release_ser_ver_id("E-00067")
ser_ver_id_prefix="EN"
bump_release_ser_ver_id("EN-0067")
