#!/usr/bin/env bash
# load-test.sh — Basic load test for LLMRouter using curl parallel requests
# Usage: ./scripts/load-test.sh [BASE_URL] [CONCURRENCY] [TOTAL_REQUESTS]

BASE_URL="${1:-http://localhost:8080}"
CONCURRENCY="${2:-10}"
TOTAL="${3:-100}"
API_KEY="dev-api-key"

echo "🔥 LLMRouter Load Test"
echo "   Gateway:     $BASE_URL"
echo "   Concurrency: $CONCURRENCY"
echo "   Total:       $TOTAL requests"
echo ""

PAYLOAD='{
  "tenantId": "dev-tenant",
  "taskType": "chat",
  "messages": [
    {"role": "user", "content": "What is the capital of France?"}
  ],
  "constraints": {
    "maxCostUsd": 0.01,
    "maxLatencyMs": 8000
  },
  "routingPolicy": "cheapest"
}'

start_time=$(date +%s%N)

for i in $(seq 1 $TOTAL); do
  (
    curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
      -X POST "$BASE_URL/v1/generate" \
      -H "Authorization: Bearer $API_KEY" \
      -H "Content-Type: application/json" \
      -d "$PAYLOAD"
  ) &

  # Throttle concurrency
  if (( i % CONCURRENCY == 0 )); then
    wait
  fi
done

wait

end_time=$(date +%s%N)
elapsed=$(( (end_time - start_time) / 1000000 ))

echo ""
echo "✅ Load test complete"
echo "   Total time: ${elapsed}ms"
echo "   Est. RPS:   $(echo "scale=1; $TOTAL * 1000 / $elapsed" | bc)"
