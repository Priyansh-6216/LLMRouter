# LLMRouter — API Specification

## Base URL
```
http://localhost:8080/v1
```

## Authentication

All endpoints require an `Authorization` header:
```
Authorization: Bearer <tenant-api-key>
```

---

## POST /v1/generate

Unified blocking inference endpoint.

### Request Body

```json
{
  "tenantId": "acme-prod",
  "taskType": "chat",
  "messages": [
    {"role": "system", "content": "You are a concise support assistant."},
    {"role": "user", "content": "Summarize this ticket thread."}
  ],
  "constraints": {
    "maxLatencyMs": 4000,
    "maxCostUsd": 0.02,
    "minQualityTier": "medium",
    "preferStreaming": false
  },
  "routingPolicy": "balanced",
  "metadata": {
    "userId": "u_123",
    "sessionId": "s_789"
  }
}
```

### Response Body

```json
{
  "requestId": "req_01",
  "provider": "anthropic",
  "model": "claude-3-haiku-20240307",
  "latencyMs": 2310,
  "cacheHit": false,
  "fallbackUsed": false,
  "usage": {
    "inputTokens": 1250,
    "outputTokens": 310,
    "estimatedCostUsd": 0.0134
  },
  "output": {
    "text": "Here is the summary..."
  }
}
```

### Task Types
| Value | Description |
|---|---|
| `chat` | Conversational assistant |
| `summarization` | Document/text summarization |
| `structured_extraction` | JSON schema extraction |
| `code_generation` | Code completion / generation |
| `classification` | Text classification |
| `embedding` | Vector embedding (future) |

### Quality Tiers
| Value | Description |
|---|---|
| `low` | Small/fast models (≤7B) |
| `medium` | Mid-tier models (7–70B) |
| `high` | Large frontier models |
| `premium` | GPT-4o, Claude 3.5 Sonnet, etc. |

---

## POST /v1/generate/stream

SSE streaming inference endpoint.

Same request body as `/v1/generate`. Returns `text/event-stream`.

### Stream Chunks

```
data: {"id":"req_01","chunk":"Here ","provider":"openai","model":"gpt-4o-mini","done":false}

data: {"id":"req_01","chunk":"is the ","provider":"openai","model":"gpt-4o-mini","done":false}

data: {"id":"req_01","chunk":"summary.","provider":"openai","model":"gpt-4o-mini","done":true,"usage":{"inputTokens":1250,"outputTokens":310,"estimatedCostUsd":0.0134}}
```

---

## GET /v1/providers/health

Returns health snapshot for all registered providers.

### Response

```json
{
  "providers": [
    {
      "name": "openai",
      "status": "healthy",
      "p95LatencyMs": 1420,
      "successRate": 0.987,
      "timeoutRate": 0.003,
      "rpm": 240,
      "updatedAt": "2025-01-15T10:23:00Z"
    },
    {
      "name": "anthropic",
      "status": "healthy",
      "p95LatencyMs": 2100,
      "successRate": 0.994,
      "timeoutRate": 0.001,
      "rpm": 180,
      "updatedAt": "2025-01-15T10:23:00Z"
    },
    {
      "name": "vllm",
      "status": "degraded",
      "p95LatencyMs": 3800,
      "successRate": 0.91,
      "timeoutRate": 0.05,
      "rpm": 60,
      "updatedAt": "2025-01-15T10:23:00Z"
    }
  ]
}
```

---

## GET /v1/models

Returns active model catalog.

### Response

```json
{
  "models": [
    {
      "id": "uuid",
      "provider": "openai",
      "modelName": "gpt-4o-mini",
      "qualityTier": "medium",
      "maxContextTokens": 128000,
      "supportsStreaming": true,
      "supportsJson": true,
      "inputCostPer1kTokens": 0.00015,
      "outputCostPer1kTokens": 0.0006,
      "status": "active"
    }
  ]
}
```

---

## POST /v1/policies/route-preview

Dry-run route selection without calling a provider.

### Request

```json
{
  "taskType": "structured_extraction",
  "estimatedInputTokens": 6000,
  "constraints": {
    "maxLatencyMs": 5000,
    "maxCostUsd": 0.03,
    "minQualityTier": "medium"
  },
  "routingPolicy": "balanced"
}
```

### Response

```json
{
  "selectedProvider": "vllm",
  "selectedModel": "local-8b-instruct",
  "estimatedCostUsd": 0.0021,
  "estimatedLatencyMs": 1200,
  "reason": ["meets cost budget", "healthy provider", "supports structured output"],
  "fallbackOrder": ["anthropic", "openai"],
  "candidatesEvaluated": 3
}
```

---

## GET /v1/requests/{id}

Returns full request trace.

### Response

```json
{
  "requestId": "req_01",
  "tenantId": "acme-prod",
  "taskType": "summarization",
  "selectedProvider": "anthropic",
  "selectedModel": "claude-3-haiku-20240307",
  "fallbackUsed": false,
  "cacheHit": false,
  "status": "completed",
  "latencyMs": 2310,
  "inputTokens": 1250,
  "outputTokens": 310,
  "estimatedCostUsd": 0.0134,
  "createdAt": "2025-01-15T10:22:58Z",
  "attempts": [
    {
      "attemptNo": 1,
      "provider": "anthropic",
      "model": "claude-3-haiku-20240307",
      "status": "success",
      "httpStatus": 200,
      "latencyMs": 2310
    }
  ]
}
```

---

## GET /v1/analytics/usage

Usage analytics by tenant/provider/model/date range.

### Query Parameters
| Param | Type | Description |
|---|---|---|
| `tenantId` | string | Filter by tenant |
| `provider` | string | Filter by provider |
| `from` | ISO 8601 | Start date |
| `to` | ISO 8601 | End date |
| `groupBy` | string | `day`, `provider`, `model` |

### Response

```json
{
  "summary": {
    "totalRequests": 14820,
    "totalInputTokens": 48200000,
    "totalOutputTokens": 9100000,
    "totalCostUsd": 184.32,
    "cacheHitRate": 0.23,
    "fallbackRate": 0.04
  },
  "breakdown": [
    {
      "date": "2025-01-15",
      "provider": "anthropic",
      "model": "claude-3-haiku-20240307",
      "requests": 5200,
      "inputTokens": 18000000,
      "outputTokens": 3400000,
      "costUsd": 72.10
    }
  ]
}
```

---

## POST /v1/admin/providers/{provider}/disable

Temporarily disable a provider.

### Path Parameters
| Param | Values |
|---|---|
| `provider` | `openai`, `anthropic`, `vllm` |

### Request

```json
{
  "durationMinutes": 30,
  "reason": "Elevated error rate detected"
}
```

### Response
```json
{
  "provider": "openai",
  "status": "disabled",
  "reenableAt": "2025-01-15T11:00:00Z"
}
```

---

## POST /v1/admin/policies

Create or update a routing policy.

### Request

```json
{
  "name": "my-balanced-policy",
  "policyType": "balanced",
  "configJson": {
    "qualityWeight": 0.35,
    "latencyWeight": 0.25,
    "costWeight": 0.20,
    "healthWeight": 0.20,
    "fallbackChain": ["anthropic", "vllm"]
  },
  "isActive": true
}
```

---

## Error Responses

All errors follow the same envelope:

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Tenant acme-prod exceeded 100 RPM limit",
    "requestId": "req_01",
    "timestamp": "2025-01-15T10:22:58Z"
  }
}
```

### Error Codes
| Code | HTTP | Description |
|---|---|---|
| `INVALID_REQUEST` | 400 | Malformed request body |
| `UNAUTHORIZED` | 401 | Invalid or missing API key |
| `RATE_LIMIT_EXCEEDED` | 429 | Tenant rate limit hit |
| `NO_ELIGIBLE_PROVIDER` | 503 | All providers failed or filtered |
| `BUDGET_EXCEEDED` | 422 | Request would exceed cost budget |
| `PROVIDER_TIMEOUT` | 504 | Provider did not respond in time |
