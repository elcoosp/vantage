#!/bin/bash
for f in TenantIsolationIT InventoryConcurrencyIT OrderOutboxIT; do
  echo "=================================================="
  echo "--- $f ---"
  echo "=================================================="
  grep -o 'message="[^"]*"' backend/build/test-results/test/TEST-com.vantage.*.${f}.xml
done
