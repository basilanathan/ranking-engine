# Ranking Engine

A modular Scala ranking system built with sbt that applies configurable ranking strategies.

## Modules

- **ranking-core**: Core logic that applies ranking strategies from configs
- **ranking-config**: Logic to parse, validate, and interpret YAML configs  
- **ranking-api**: Sample HTTP and CLI interfaces for using the ranker
- **ranking-tests**: Unit and integration tests

## Quick Start

```bash
# Build the project
sbt compile

# Run tests
sbt test

# Start HTTP server
sbt "ranking-api/run"

# Use CLI interface
sbt "ranking-api/runMain com.example.ranking.api.CliApp --items '[{\"id\":\"1\",\"score\":0.8},{\"id\":\"2\",\"score\":0.5}]'"
```

## Configuration

Example YAML config:

```yaml
strategy: weighted
weights:
  popularity: 0.6
  recency: 0.4
parameters:
  threshold: "0.5"
```

## API Endpoints

- `GET /health` - Health check
- `POST /rank` - Rank items with optional config

Example request:
```json
{
  "items": [
    {"id": "1", "score": 0.8, "metadata": {"popularity": "0.9"}},
    {"id": "2", "score": 0.5, "metadata": {"popularity": "0.3"}}
  ],
  "config": {
    "strategy": "weighted",
    "weights": {"popularity": 0.6}
  }
}
```