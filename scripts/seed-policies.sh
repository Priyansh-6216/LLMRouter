#!/usr/bin/env bash
# seed-policies.sh — Seeds the database with providers, models, policies, and a dev tenant.
# Usage: ./scripts/seed-policies.sh [BASE_URL]

BASE_URL="${1:-http://localhost:8080}"
API_KEY="dev-api-key"

echo "🌱 Seeding LLMRouter with providers, models, and policies..."
echo "   Gateway: $BASE_URL"
echo ""

# ──────────────────────────────────────────────
# 1. Register Providers
# ──────────────────────────────────────────────
echo "📡 Registering providers..."

curl -s -X POST "$BASE_URL/v1/admin/providers" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "openai",
    "baseUrl": "https://api.openai.com/v1",
    "status": "active",
    "priority": 1
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/providers" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "anthropic",
    "baseUrl": "https://api.anthropic.com",
    "status": "active",
    "priority": 2
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/providers" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "vllm",
    "baseUrl": "http://localhost:8000/v1",
    "status": "active",
    "priority": 3
  }' | jq .

# ──────────────────────────────────────────────
# 2. Register Models
# ──────────────────────────────────────────────
echo ""
echo "🤖 Registering models..."

curl -s -X POST "$BASE_URL/v1/admin/models" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "modelName": "gpt-4o",
    "qualityTier": "premium",
    "maxContextTokens": 128000,
    "supportsStreaming": true,
    "supportsJson": true,
    "inputCostPer1k": 0.005,
    "outputCostPer1k": 0.015,
    "status": "active"
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/models" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "modelName": "gpt-4o-mini",
    "qualityTier": "medium",
    "maxContextTokens": 128000,
    "supportsStreaming": true,
    "supportsJson": true,
    "inputCostPer1k": 0.00015,
    "outputCostPer1k": 0.0006,
    "status": "active"
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/models" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "anthropic",
    "modelName": "claude-3-5-sonnet-20241022",
    "qualityTier": "premium",
    "maxContextTokens": 200000,
    "supportsStreaming": true,
    "supportsJson": true,
    "inputCostPer1k": 0.003,
    "outputCostPer1k": 0.015,
    "status": "active"
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/models" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "anthropic",
    "modelName": "claude-3-haiku-20240307",
    "qualityTier": "medium",
    "maxContextTokens": 200000,
    "supportsStreaming": true,
    "supportsJson": true,
    "inputCostPer1k": 0.00025,
    "outputCostPer1k": 0.00125,
    "status": "active"
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/models" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "vllm",
    "modelName": "local-8b-instruct",
    "qualityTier": "low",
    "maxContextTokens": 8192,
    "supportsStreaming": true,
    "supportsJson": false,
    "inputCostPer1k": 0.0001,
    "outputCostPer1k": 0.0001,
    "status": "active"
  }' | jq .

# ──────────────────────────────────────────────
# 3. Create Routing Policies
# ──────────────────────────────────────────────
echo ""
echo "🗺 Creating routing policies..."

curl -s -X POST "$BASE_URL/v1/admin/policies" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "default-balanced",
    "policyType": "balanced",
    "configJson": {
      "qualityWeight": 0.35,
      "latencyWeight": 0.25,
      "costWeight": 0.20,
      "healthWeight": 0.20,
      "fallbackChain": ["anthropic", "vllm"]
    },
    "isActive": true
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/policies" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "cheapest-policy",
    "policyType": "cheapest",
    "configJson": {
      "fallbackChain": ["openai", "anthropic"]
    },
    "isActive": true
  }' | jq .

curl -s -X POST "$BASE_URL/v1/admin/policies" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "fastest-policy",
    "policyType": "fastest",
    "configJson": {
      "fallbackChain": ["anthropic", "vllm"]
    },
    "isActive": true
  }' | jq .

echo ""
echo "✅ Seeding complete!"
echo ""
echo "🔍 Check provider health:"
echo "   curl $BASE_URL/v1/providers/health"
echo ""
echo "🔍 List models:"
echo "   curl $BASE_URL/v1/models"
