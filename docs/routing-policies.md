# LLMRouter — Routing Policies

## Overview

Routing policies determine how LLMRouter selects a provider and model for each request. Policies can be assigned at the tenant level (default) or overridden per-request via the `routingPolicy` field.

---

## Built-in Policies

### 1. `fastest`

**Goal:** Minimize latency above all else.

**Algorithm:**
1. Filter providers by health state (`healthy`)
2. Sort by p95 latency (ascending) from rolling health window
3. Select provider with lowest p95 that satisfies `maxLatencyMs` constraint
4. Fallback to next-fastest on unavailability

**Best for:** Interactive chat, real-time assistant, low-latency UX

---

### 2. `cheapest`

**Goal:** Minimize cost while satisfying task constraints.

**Algorithm:**
1. Estimate input + output tokens (pre-call estimation)
2. Compute `estimatedCost = (inputTokens / 1000) × inputCostPer1k + (outputTokens / 1000) × outputCostPer1k`
3. Filter by `estimatedCost ≤ maxCostUsd`
4. Filter by `qualityTier ≥ minQualityTier`
5. Select cheapest passing candidate

**Best for:** Batch summarization, internal tools, FAQ generation

---

### 3. `balanced`

**Goal:** Optimize across cost, latency, quality, and health simultaneously.

**Scoring formula:**
```
score = (w_quality × qualityScore)
      - (w_latency × latencyPenalty)
      - (w_cost   × costPenalty)
      + (w_health × healthScore)
```

**Score components:**
| Component | Source |
|---|---|
| `qualityScore` | Model tier: low=0.25, medium=0.50, high=0.75, premium=1.0 |
| `latencyPenalty` | `p95LatencyMs / maxLatencyMs` (capped at 1.0) |
| `costPenalty` | `estimatedCost / maxCostUsd` (capped at 1.0) |
| `healthScore` | Rolling success rate (0.0–1.0) |

**Default weights (configurable in `config_json`):**
```json
{
  "qualityWeight": 0.35,
  "latencyWeight": 0.25,
  "costWeight": 0.20,
  "healthWeight": 0.20
}
```

**Best for:** General production traffic, multi-tenant SaaS gateways

---

### 4. `privacy-first`

**Goal:** Route to local/self-hosted vLLM for eligible task classes.

**Algorithm:**
1. Check if task type is in tenant's `allowedLocalTasks` list
2. If yes → prefer vLLM adapter (self-hosted)
3. If vLLM unhealthy → fallback to cloud provider with lowest data-retention risk
4. Log routing decision for compliance audit

**Best for:** Enterprises with data residency requirements, PII-sensitive workloads

---

### 5. `resilient`

**Goal:** Maximize availability; route based on historical success rate.

**Algorithm:**
1. Sort providers by rolling success rate (descending)
2. Pre-define fallback chain in policy config
3. On failure: follow explicit fallback chain immediately
4. Circuit breaker opens after `failureRateThreshold` in sliding window

**Example policy config:**
```json
{
  "primaryProvider": "openai",
  "fallbackChain": ["anthropic", "vllm"],
  "failureRateThreshold": 0.3,
  "slidingWindowSize": 20,
  "retryOnTimeout": true,
  "maxRetries": 1
}
```

**Best for:** Critical workflows, external-facing products, SLA-bound tenants

---

## Fallback Rules

| Failure Reason | Action |
|---|---|
| Timeout | Retry same provider once, then fallback |
| HTTP 429 (Rate Limit) | Immediate provider switch, no retry |
| HTTP 500/502/503 | Retry once, then fallback |
| Malformed JSON output | Retry once with stricter system prompt |
| Budget violation risk | Downgrade model pre-call |
| Circuit breaker open | Skip to next healthy provider |

---

## Custom Policy Configuration

Policies are stored in the `routing_policies` table as JSONB. Example:

```json
{
  "policyType": "balanced",
  "config": {
    "qualityWeight": 0.40,
    "latencyWeight": 0.30,
    "costWeight": 0.15,
    "healthWeight": 0.15,
    "fallbackChain": ["anthropic", "vllm"],
    "maxRetries": 1,
    "retryOnTimeout": true
  }
}
```

Create or update via:
```
POST /v1/admin/policies
GET  /v1/admin/policies/{id}
```

---

## Dry-Run Route Preview

Test routing without invoking a provider:

```http
POST /v1/policies/route-preview

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

Response:
```json
{
  "selectedProvider": "vllm",
  "selectedModel": "local-8b-instruct",
  "estimatedCostUsd": 0.0021,
  "estimatedLatencyMs": 1200,
  "reason": ["meets cost budget", "healthy provider", "supports structured output"],
  "fallbackOrder": ["anthropic", "openai"]
}
```
