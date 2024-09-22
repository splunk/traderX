# See here for image contents: https://github.com/microsoft/vscode-dev-containers/tree/v0.245.2/containers/dotnet/.devcontainer/base.Dockerfile

# [Choice] .NET version: 8.0 /8.0-bookworm, 8.0-jammy, 7.0 /7.0-bookworm, 7.0-bullseye, 7.0-jammy, 6.0 /6.0-bookworm, 6.0-bullseye, 6.0-jammy, 6.0-focal
#ARG VARIANT="0.4.4" 
FROM mcr.microsoft.com/devcontainers/dotnet:8.0 AS build
# Use the official .NET 8.0 SDK image for building the application
#FROM mcr.microsoft.com/dotnet/sdk:8.0.401-1-noble AS build
# WORKDIR /app
# WORKDIR /people-service
COPY . .

# Restore NuGet packages
RUN dotnet restore
RUN dotnet publish -c Release -o /app

EXPOSE 18089


# Add dependencies for splunk-otel-dotnet-install.sh
RUN apt-get update && \
	apt-get install -y unzip

# Download Splunk OTel .NET installer
RUN curl -sSfL https://github.com/signalfx/splunk-otel-dotnet/releases/latest/download/splunk-otel-dotnet-install.sh -O

# Install the distribution
RUN sh ./splunk-otel-dotnet-install.sh


FROM build AS publish
# Copy instrumentation file tree
WORKDIR "/home/app/.splunk-otel-dotnet"
COPY --from=build /root/.splunk-otel-dotnet/ .

# [Choice] Node.js version: none, lts/*, 18, 16, 14
ARG NODE_VERSION="18"
RUN if [ "${NODE_VERSION}" != "none" ]; then su vscode -c "umask 0002 && . /usr/local/share/nvm/nvm.sh && nvm install ${NODE_VERSION} 2>&1"; fi

# [Optional] Uncomment this section to install additional OS packages.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>

# [Optional] Uncomment this line to install global node packages.
# RUN su vscode -c "source /usr/local/share/nvm/nvm.sh && npm install -g <your-package-here>" 2>&1

WORKDIR /people-service
COPY  ./scripts/entrypoint.sh .
ENTRYPOINT ["sh", "entrypoint.sh"]
#CMD ["dotnet", "PeopleService.WebApi.dll"]
#CMD ["cd", "PeopleService.WebApi", "&&", "dotnet", "run"]
#ENTRYPOINT [ "tail", "-f", "/dev/null" ]