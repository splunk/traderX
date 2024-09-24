#!/bin/bash
set -x

cd /
# Read in the file of environment settings
. /$HOME/.splunk-otel-dotnet/instrument.sh

# Then run the CMD
cd /app
export ASPNETCORE_URLS="http://0.0.0.0:18089"
dotnet PeopleService.WebApi.dll 
