# Use the .NET 8.0 SDK as the base image to build the application
# [Choice] .NET version: 8.0 /8.0-bookworm, 8.0-jammy, 7.0 /7.0-bookworm, 7.0-bullseye, 7.0-jammy, 6.0 /6.0-bookworm, 6.0-bullseye, 6.0-jammy, 6.0-focal
ARG VARIANT="8.0"
FROM mcr.microsoft.com/dotnet/sdk:${VARIANT} AS build

# Set the working directory inside the container
WORKDIR /src

# Copy the project files to the container
COPY . .

# Build and publish the application in release mode
RUN dotnet publish PeopleService.WebApi/PeopleService.WebApi.csproj -c Release -o /app/publish

# Use a smaller runtime image for running the application
FROM mcr.microsoft.com/dotnet/aspnet:${VARIANT}-alpine AS runtime

# Set the working directory inside the container for the runtime
WORKDIR /app

# Copy the built application from the build stage to the runtime stage
COPY --from=build /app/publish .

# Expose the required port
EXPOSE 18089

# Set the entry point for the container to run the .NET app
ENTRYPOINT ["dotnet", "PeopleService.WebApi.dll"]