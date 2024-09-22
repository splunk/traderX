# Use the official .NET SDK image to build the app
# ARG allows you to change the .NET version as needed
ARG VARIANT="8.0"
FROM mcr.microsoft.com/vscode/devcontainers/dotnet:${VARIANT} AS build-env

# Set the working directory inside the container
WORKDIR /app

# Copy the project files into the container
COPY . . 

# Restore the dependencies (equivalent to running 'dotnet restore')
RUN dotnet restore

# Build and publish the app in release mode to the /out directory
RUN dotnet publish -c Release -o /out

# Use the runtime image for .NET (lighter than the SDK image)
FROM mcr.microsoft.com/dotnet/aspnet:${VARIANT}-alpine AS runtime

# Set the working directory for the final runtime image
WORKDIR /app

# Copy the precompiled output from the build stage
COPY --from=build-env /out .

# Expose the necessary port
EXPOSE 18089

# Set the entry point to run the precompiled app
ENTRYPOINT ["dotnet", "PeopleService.WebApi.dll"]
#ENTRYPOINT ["tail", "-f", "/dev/null"]