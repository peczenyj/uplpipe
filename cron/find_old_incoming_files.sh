#!/bin/bash
# use @daily in crontab
find /uploads -name 'incoming*' -ctime 24 -exec rm -f {} +
