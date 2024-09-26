using FluentValidation.AspNetCore;
using Microsoft.OpenApi.Models;
using PeopleService.Core.Infrastructure;
using Serilog;
using Serilog.Enrichers.Span;
using Serilog.Formatting.Json;
using System.IO;
using System.Reflection;


var builder = WebApplication.CreateBuilder(args);
// Read service name from environment variables
var serviceName = Environment.GetEnvironmentVariable("OTEL_SERVICE_NAME") ?? "people-service";
var deploymentEnv = Environment.GetEnvironmentVariable("SPLUNK_ENV_NAME") ?? "unknown";
// Add services to the container.

builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(
                c =>
                {
                    c.SwaggerDoc("v1", new OpenApiInfo { Title = "PeopleService.WebApi", Version = "v1" });
                    var xmlFilename = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
                    c.CustomSchemaIds(t => t.FullName);
                    c.IncludeXmlComments(Path.Combine(AppContext.BaseDirectory, xmlFilename));
                });
builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
builder.Services.AddPeopleServiceCore(builder.Configuration.GetSection("PeopleJsonFilePath"));
builder.Services.AddFluentValidationAutoValidation();
builder.Services.AddFluentValidationClientsideAdapters();

builder.Host.UseSerilog((hostContext, services, configuration) =>
{
    configuration
        .MinimumLevel.Information()  // Set default severity level to Information
        .Enrich.WithProperty("service.name", serviceName)  // Add service.name to all logs
        .Enrich.WithProperty("deployment.environment",deploymentEnv )
        .Enrich.WithSpan()  // Include TraceId and SpanId
       .WriteTo.Console(new SplunkO11YLoCustomJsonFormatter());  // Use custom formatter to change Level to Severity and map level names

});

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(c => c.SwaggerEndpoint("/swagger/v1/swagger.json", "PeopleService.WebApi v1"));
}

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.UseCors(builder => builder
    .AllowAnyOrigin()
    .AllowAnyMethod()
    .AllowAnyHeader());

app.Run();
 