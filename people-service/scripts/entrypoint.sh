#!/bin/sh
set -x

cd /
# Read in the file of environment settings
. /$HOME/.splunk-otel-dotnet/instrument.sh

# Then run the CMD
cd /app
dotnet PeopleService.WebApi.dll