# LLMRouter — Demo Scenarios

Five end-to-end scenarios that demonstrate the gateway's routing intelligence.

---

## Demo 1: Cheapest Route Selection

**Goal:** Show cost-aware routing selects vLLM for a budget-constrained summarization task.

### Request
```bash
curl -X POST http://localhost:8080/v1/generate \
  -H "Authorization: Bearer dev-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-prod",
    "taskType": "summarization",
    "messages": [
      {"role": "system", "content": "Summarize the following support ticket."},
      {"role": "user", "content": "Customer is unable to log in after password reset..."}
    ],
    "constraints": {
      "maxLatencyMs": 8000,
      "maxCostUsd": 0.005,
      "minQualityTier": "low"
    },
    "routingPolicy": "cheapest"
  }'
```

### Expected Response
```json
{
  "provider": "vllm",
  "model": "local-8b-instruct",
  "latencyMs": 1800,
  "cacheHit": false,
  "usage": { "estimatedCostUsd": 0.0009 }
}
```

**Why vLLM?** Cheapest cost-per-token, task requires only low quality tier, vLLM is healthy.

---

## Demo 2: High-Quality Premium Route

**Goal:** Show that a reasoning-heavy request with premium policy selects the top-tier model.

### Request
```bash
curl -X POST http://localhost:8080/v1/generate \
  -H "Authorization: Bearer dev-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-prod",
    "taskType": "code_generation",
    "messages": [
      {"role": "user", "content": "Design a thread-safe LRU cache in Java with full test coverage."}
    ],
    "constraints": {
      "maxLatencyMs": 10000,
      "maxCostUsd": 0.15,
      "minQualityTier": "premium"
    },
    "routingPolicy": "balanced"
  }'
```

### Expected Response
```json
{
  "provider": "openai",
  "model": "gpt-4o",
  "latencyMs": 4200,
  "cacheHit": false,
  "usage": { "estimatedCostUsd": 0.092 }
}
```

**Why OpenAI GPT-4o?** Only `premium` tier satisfies `minQualityTier`, OpenAI has best p95 latency among premium models.

---

## Demo 3: Provider Outage Fallback

**Goal:** Disable OpenAI and show automatic fallback to Anthropic.

### Step 1 — Disable OpenAI
```bash
curl -X POST http://localhost:8080/v1/admin/providers/openai/disable \
  -H "Authorization: Bearer admin-api-key" \
  -d '{"durationMinutes": 10, "reason": "Demo: simulated outage"}'
```

### Step 2 — Send request (same as Demo 2)
```bash
curl -X POST http://localhost:8080/v1/generate \
  -H "Authorization: Bearer dev-api-key" \
  ...same body as Demo 2...
```

### Expected Response
```json
{
  "provider": "anthropic",
  "model": "claude-3-5-sonnet-20241022",
  "latencyMs": 5100,
  "fallbackUsed": true,
  "cacheHit": false
}
```

**Why fallback?** OpenAI is disabled → routing engine skips it → Anthropic is next healthy provider in `resilient` fallback chain.

---

## Demo 4: Cache Hit on Repeated Request

**Goal:** Show that identical deterministic prompts are served from Redis cache.

### First call (cache miss)
```bash
curl -X POST http://localhost:8080/v1/generate \
  -H "Authorization: Bearer dev-api-key" \
  -d '{
    "taskType": "structured_extraction",
    "messages": [
      {"role": "system", "content": "Extract: {name, email, issue} as JSON."},
      {"role": "user", "content": "Hi, I am Alice (alice@acme.com). My order #1234 has not arrived."}
    ],
    "constraints": {"maxCostUsd": 0.01},
    "routingPolicy": "cheapest"
  }'
```

**Response 1:**
```json
{ "cacheHit": false, "latencyMs": 1900, "provider": "vllm" }
```

### Second call (cache hit — same prompt)
Same request body.

**Response 2:**
```json
{ "cacheHit": true, "latencyMs": 12, "provider": "vllm" }
```

**Why cache hit?** SHA-256 of (system prompt + user content + taskType + policy) matches Redis key → response served in ~12ms, zero LLM cost.

---

## Demo 5: Long-Context Routing

**Goal:** Show that a large document request routes to a provider/model that supports the context window.

### Request (12,000-token document)
```bash
curl -X POST http://localhost:8080/v1/generate \
  -H "Authorization: Bearer dev-api-key" \
  -d '{
    "taskType": "summarization",
    "messages": [
      {"role": "system", "content": "Summarize this legal contract."},
      {"role": "user", "content": "<12,000 token legal document text>"}
    ],
    "constraints": {
      "maxLatencyMs": 15000,
      "maxCostUsd": 0.10,
      "minQualityTier": "high"
    },
    "routingPolicy": "balanced"
  }'
```

### Expected Response
```json
{
  "provider": "anthropic",
  "model": "claude-3-haiku-20240307",
  "latencyMs": 6800,
  "cacheHit": false,
  "usage": { "inputTokens": 12450, "estimatedCostUsd": 0.014 }
}
```

**Why Anthropic?** vLLM local model has max context 8K → filtered out. Anthropic Claude supports 200K context. Among remaining candidates, Anthropic is cheapest + healthy.

---

## Running the Demos

```bash
# Start all services
docker compose up -d

# Seed test data (providers, models, policies)
./scripts/seed-policies.sh

# Run all demo scenarios
./scripts/demo-all.sh
```
