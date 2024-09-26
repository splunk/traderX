using Serilog.Events;
using Serilog.Formatting;
using System.IO;
using Newtonsoft.Json;
using System.Collections.Generic;
using System.Linq;

public class SplunkO11YLoCustomJsonFormatter : ITextFormatter
{
    public void Format(LogEvent logEvent, TextWriter output)
    {
        // Flatten properties into the root JSON object
        var properties = new Dictionary<string, object>();

        foreach (var property in logEvent.Properties)
        {
            var key = property.Key switch
            {
                "TraceId" => "trace_id",
                "SpanId" => "span_id",
                "ParentId" => "parent_id",
                _ => property.Key  // Keep other keys as they are
            };

            properties[key] = SimplifyLogEventPropertyValue(property.Value);
        }

        // Create the final log object, placing flattened properties in the root
        var logObject = new Dictionary<string, object>
        {
            ["level"] = logEvent.Level switch
            {
                LogEventLevel.Information => "info",
                LogEventLevel.Warning => "warn",
                LogEventLevel.Error => "error",
                _ => logEvent.Level.ToString().ToLower()
            },
            ["message"] = logEvent.RenderMessage(),
            ["service.name"] = properties.ContainsKey("service.name") ? properties["service.name"] : "unknown",
            ["span_id"] = properties.ContainsKey("span_id") ? properties["span_id"] : null,
            ["trace_id"] = properties.ContainsKey("trace_id") ? properties["trace_id"] : null,
            ["trace_flags"] = properties.ContainsKey("trace_flags") ? properties["trace_flags"] : null,
            ["service.version"] = properties.ContainsKey("service.version") ? properties["service.version"] : "unknown"
        };

        // Merge in remaining properties (excluding the ones already added)
        foreach (var (key, value) in properties)
        {
            if (!logObject.ContainsKey(key))
            {
                logObject[key] = value;
            }
        }

        // Serialize to JSON and write to output
        var json = JsonConvert.SerializeObject(logObject, Formatting.None);
        output.WriteLine(json);
    }

    // Helper method to simplify property values
    private static object SimplifyLogEventPropertyValue(LogEventPropertyValue propertyValue)
    {
        return propertyValue switch
        {
            ScalarValue scalarValue => scalarValue.Value,
            SequenceValue sequenceValue => sequenceValue.Elements.Select(SimplifyLogEventPropertyValue).ToList(),
            StructureValue structureValue => structureValue.Properties.ToDictionary(p => p.Name, p => SimplifyLogEventPropertyValue(p.Value)),
            DictionaryValue dictionaryValue => dictionaryValue.Elements.ToDictionary(kvp => kvp.Key.Value?.ToString(), kvp => SimplifyLogEventPropertyValue(kvp.Value)),
            _ => propertyValue.ToString()
        };
    }
}