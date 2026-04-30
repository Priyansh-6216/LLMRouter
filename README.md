<p align="center">
  <h1 align="center">🔀 LLMRouter</h1>
  <p align="center"><strong>Production-grade multi-model AI gateway for routing, fallback, caching, cost control, and observability</strong></p>
  <p align="center">
    <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" />
    <img src="https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?logo=spring" />
    <img src="https://img.shields.io/badge/Redis-7-red?logo=redis" />
    <img src="https://img.shields.io/badge/Kafka-3-black?logo=apachekafka" />
    <img src="https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql" />
    <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker" />
    <img src="https://img.shields.io/badge/OpenTelemetry-enabled-7B45E7?logo=opentelemetry" />
  </p>
</p>

---

> **LLMRouter** is a production-style AI gateway that routes requests across multiple LLM providers based on cost, latency, health, and policy constraints. It supports streaming, automatic fallback, Redis-backed caching, Kafka-based usage analytics, and provider-agnostic request normalization for OpenAI, Anthropic, and self-hosted vLLM endpoints.

---

## 🧩 Problem

Every team building AI-powered products hits the same problem: **hardcoding LLM providers**.

- App A uses OpenAI directly → breaks on rate limits
- App B calls Anthropic → no cost controls
- App C runs vLLM locally → no observability
- Nobody knows which model costs what, or why one failed

**LLMRouter solves this.** A single API gateway that abstracts all provider logic, enforces cost and latency policies, and gives you full observability — so your apps just call `/v1/generate` and the gateway handles the rest.

---

## 🏢 Why Companies Need an AI Gateway

| Problem Without Gateway | Solution With LLMRouter |
|---|---|
| Hardcoded API keys in every service | Centralized tenant-scoped auth |
| No fallback when OpenAI goes down | Automatic fallback chain |
| Paying for repeated identical prompts | Redis response cache |
| No cost tracking per team/product | Per-tenant usage + billing analytics |
| Streaming implemented differently everywhere | Unified SSE proxy |
| Different providers, different request formats | Provider-agnostic internal schema |
| Can't A/B test models | Policy-driven routing with dry-run preview |

---

## 🏗 Architecture

```text
Clients / Apps
   │
   ▼
┌─────────────────────────────────────────────┐
│       API Gateway (Spring Boot)             │
│  Auth · Rate Limit · Tenant Policy          │
│  Request Normalization · SSE Proxy          │
└───────────────┬─────────────────┬───────────┘
                │                 │
                ▼                 ▼
┌───────────────────┐   ┌─────────────────────┐
│   Routing Engine  │   │   Cache Layer       │
│  Policy Evaluator │   │   (Redis)           │
│  Cost Estimator   │   │  Semantic-safe key  │
│  Token Estimator  │   │  TTL: 1h–24h        │
│  Health Selector  │   └─────────────────────┘
│  Fallback Planner │
└────────┬──────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│           Provider Adapter Layer            │
│  ┌──────────────┐ ┌─────────────┐ ┌──────┐ │
│  │ OpenAI       │ │ Anthropic   │ │ vLLM │ │
│  │ Adapter      │ │ Adapter     │ │ Adap │ │
│  └──────────────┘ └─────────────┘ └──────┘ │
└───────────────────────┬─────────────────────┘
                        │
                        ▼
             ┌──────────────────┐
             │  Streaming       │
             │  Aggregator      │
             └────────┬─────────┘
                      │  (async)
                      ▼
             ┌─────────────────────────────────┐
             │        Kafka Event Bus          │
             │  llm.requests · llm.fallback    │
             │  llm.cache · llm.health         │
             └──────┬─────────────┬────────────┘
                    │             │
           ┌────────┘         ┌───┘
           ▼                  ▼
  ┌─────────────────┐  ┌────────────────────┐
  │ Analytics       │  │ Cost + Alert       │
  │ Consumer        │  │ Consumers          │
  │ → PostgreSQL    │  │ → PostgreSQL       │
  └─────────────────┘  └────────────────────┘
```

[📐 Full Architecture Diagram →](./docs/architecture.svg)

---

## ✨ Features

### Routing
- **5 routing policies**: Fastest, Cheapest, Balanced, Privacy-first, Resilient
- **Constraint filtering**: latency budget, cost budget, min quality tier, context window
- **Dry-run preview**: `POST /v1/policies/route-preview` — inspect routing decision without calling a provider
- **Token estimation**: pre-call token count for budget checks (Anthropic token-counting API / tiktoken)

### Reliability
- **Automatic fallback**: configurable fallback chain per policy
- **Circuit breaker**: Resilience4j failure-rate + timeout breakers
- **Smart retry rules**: different behavior for 429, timeout, 5xx, malformed output
- **Provider disable API**: instantly pull a misbehaving provider from routing

### Performance
- **Redis response cache**: semantic-safe, hash-keyed, task-aware TTL
- **SSE streaming**: real-time output proxy via `POST /v1/generate/stream`
- **Java 21 virtual threads**: high-throughput non-blocking I/O
- **Async observability**: Kafka decouples analytics from request latency

### Observability
- **Kafka event pipeline**: `llm.requests.*`, `llm.fallback.*`, `llm.cache.*`, `llm.provider.health`
- **Per-request trace**: provider, model, latency, fallback chain, attempts log
- **Usage analytics**: token counts, cost, cache hit rate, fallback rate by tenant/model/date
- **OpenTelemetry**: distributed tracing across all layers
- **Prometheus + Grafana**: live dashboards for p95 latency, RPM, error rate, cost/hour

### Multi-tenancy
- API key per tenant with budget and rate limit enforcement
- Per-tenant routing policy assignment
- Per-tenant usage and cost reporting

---

## 🔌 Provider Support

| Provider | Transport | Streaming | Token Counting | Prompt Caching |
|---|---|---|---|---|
| **OpenAI** | REST | ✅ SSE | ✅ | — |
| **Anthropic** | REST | ✅ SSE | ✅ Native API | ✅ |
| **vLLM** | REST | ✅ SSE | ✅ (OpenAI-compat) | — |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.x + WebFlux |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Messaging | Apache Kafka 3 |
| Circuit Breaker | Resilience4j |
| DB Migrations | Flyway |
| Observability | OpenTelemetry + Prometheus + Grafana |
| Build | Maven |
| Containerization | Docker + Docker Compose |

---

## 🚀 Quick Start

### Prerequisites
- Docker + Docker Compose
- Java 21
- Maven 3.9+
- (Optional) OpenAI / Anthropic API keys

### 1. Clone
```bash
git clone https://github.com/Priyansh-6216/LLMRouter.git
cd LLMRouter
```

### 2. Configure
```bash
cp .env.example .env
# Edit .env:
# OPENAI_API_KEY=sk-...
# ANTHROPIC_API_KEY=sk-ant-...
# VLLM_BASE_URL=http://localhost:11434/v1  (or leave blank)
```

### 3. Start infrastructure
```bash
docker compose up -d postgres redis kafka
```

### 4. Run the gateway
```bash
cd llmrouter-api
mvn spring-boot:run
```

### 5. Seed test data
```bash
./scripts/seed-policies.sh
```

### 6. Test it
```bash
curl -X POST http://localhost:8080/v1/generate \
  -H "Authorization: Bearer dev-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "dev-tenant",
    "taskType": "chat",
    "messages": [{"role": "user", "content": "Hello, who are you?"}],
    "constraints": {"maxCostUsd": 0.01, "maxLatencyMs": 5000},
    "routingPolicy": "cheapest"
  }'
```

---

## 📡 API Examples

### Blocking inference
```bash
POST /v1/generate
Authorization: Bearer <api-key>

{
  "tenantId": "acme-prod",
  "taskType": "summarization",
  "messages": [
    {"role": "system", "content": "Summarize concisely."},
    {"role": "user", "content": "Long document text here..."}
  ],
  "constraints": {
    "maxLatencyMs": 5000,
    "maxCostUsd": 0.02,
    "minQualityTier": "medium"
  },
  "routingPolicy": "balanced"
}
```

### Streaming inference
```bash
POST /v1/generate/stream   # Returns text/event-stream
```

### Dry-run route preview
```bash
POST /v1/policies/route-preview
{
  "taskType": "structured_extraction",
  "estimatedInputTokens": 6000,
  "constraints": {"maxCostUsd": 0.03, "maxLatencyMs": 5000, "minQualityTier": "medium"},
  "routingPolicy": "balanced"
}
# → {"selectedProvider":"vllm","reason":["meets cost budget","healthy"],"fallbackOrder":["anthropic","openai"]}
```

---

## ⚡ Routing Policies

| Policy | Strategy |
|---|---|
| `fastest` | Lowest p95 latency among healthy providers |
| `cheapest` | Minimum cost that satisfies task constraints |
| `balanced` | Weighted score: quality + latency + cost + health |
| `privacy-first` | Prefer local vLLM for eligible task classes |
| `resilient` | Best success rate + defined fallback chain |

[📋 Full Policy Docs →](./docs/routing-policies.md)

---

## 🔁 Failure Handling

| Failure | Action |
|---|---|
| Timeout | Retry once, then fallback |
| HTTP 429 Rate Limit | Immediate provider switch |
| HTTP 500/502/503 | Retry once, then fallback |
| Malformed JSON output | Retry with stricter instruction |
| Budget violation risk | Pre-call model downgrade |
| Circuit breaker open | Skip to next healthy provider |

---

## 📊 Observability

- **`GET /v1/providers/health`** — live provider health snapshot
- **`GET /v1/requests/{id}`** — full request trace with attempt history
- **`GET /v1/analytics/usage`** — token / cost / cache / fallback analytics
- **Kafka topics:** `llm.requests.started/completed/failed`, `llm.fallback.triggered`, `llm.cache.hit/miss`, `llm.provider.health`
- **Prometheus + Grafana** dashboards for p95 latency, RPM, error rate, cost/hour

---

## 🗺 Roadmap

### ✅ MVP (Days 1–10)
- [x] Day 1: Repo structure, README, architecture diagram
- [x] Day 2: Spring Boot project, DTOs, `/v1/generate` stub
- [x] Day 3: OpenAI adapter
- [x] Day 4: Anthropic adapter
- [x] Day 5: vLLM adapter
- [x] Day 6: Routing engine + policy scoring
- [x] Day 7: Redis cache + cache metrics
- [x] Day 8: Fallback + circuit breaker + health checks
- [x] Day 9: Kafka events + PostgreSQL analytics
- [x] Day 10: Streaming endpoint + demo video + polished README

### 🔜 V2
- [ ] Per-tenant rate limits and budget enforcement
- [ ] Prompt template management
- [ ] JSON schema output enforcement
- [ ] Admin dashboard (React)
- [ ] Benchmarking / eval runner
- [ ] Prompt caching (Anthropic native prefix caching)

---

## 💼 Impact

- Designed and built a multi-model AI gateway in Java/Spring Boot to route inference traffic across OpenAI, Anthropic, and self-hosted vLLM based on cost, latency, health, and policy constraints
- Implemented automatic fallback, Redis-backed response caching, and Kafka-based usage telemetry — improving resilience and reducing repeated inference overhead for deterministic workloads
- Built provider-agnostic request normalization and streaming APIs, enabling centralized LLM access, tenant policy enforcement, and analytics-driven model selection

---

## 📄 Documentation

- [Architecture Deep Dive](./docs/architecture.md)
- [API Specification](./docs/api-spec.md)
- [Routing Policies](./docs/routing-policies.md)
- [Demo Scenarios](./docs/demo-scenarios.md)

---

## 📝 License

MIT — see [LICENSE](./LICENSE)
