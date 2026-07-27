// backend/src/test/performance/flash_sale.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TENANT_ID = __ENV.TENANT_ID || '00000000-0000-0000-0000-000000000000';
const PRODUCT_ID = __ENV.PRODUCT_ID || '00000000-0000-0000-0000-000000000001';
const VENDOR_TOKEN = __ENV.VENDOR_TOKEN || '';

const errorRateExclConflict = new Rate('error_rate_excl_conflict');

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 1000 },
    { duration: '2m', target: 1000 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<200'],
    'error_rate_excl_conflict': ['rate<0.01'],
  },
};

export function setup() {
  let token = VENDOR_TOKEN;
  let productId = PRODUCT_ID;
  let tenantId = TENANT_ID;

  if (!token) {
    const registerPayload = JSON.stringify({
      email: `perf-${Date.now()}@vantage.com`,
      password: 'securePassword123',
      name: 'Perf Test Vendor'
    });

    const registerRes = http.post(`${BASE_URL}/api/v1/vendors/register`, registerPayload, {
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-ID': tenantId
      }
    });

    if (registerRes.status === 201) {
      token = registerRes.json().token;
      tenantId = registerRes.json().tenantId;

      const productPayload = JSON.stringify({
        name: 'Flash Sale Product',
        description: 'High demand item',
        price: 99.99
      });

      const productRes = http.post(`${BASE_URL}/api/v1/products`, productPayload, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'X-Tenant-ID': tenantId
        }
      });

      if (productRes.status === 200) {
        productId = productRes.json().id;
      }
    }
  }

  return { token, productId, tenantId };
}

export default function (data) {
  const { token, productId, tenantId } = data;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Tenant-ID': tenantId
  };

  const productRes = http.get(`${BASE_URL}/api/v1/products/${productId}`, { headers });
  check(productRes, { 'product fetched': (r) => r.status === 200 });

  const orderPayload = JSON.stringify({
    productId: productId,
    quantity: 1,
    productName: 'Flash Sale Product'
  });

  const orderRes = http.post(`${BASE_URL}/api/v1/orders`, orderPayload, { headers });
  const isConflict = orderRes.status === 409;
  const isFailed = orderRes.status >= 400 && !isConflict;

  check(orderRes, {
    'order placed or conflict': (r) => r.status === 202 || r.status === 409,
  });

  errorRateExclConflict.add(isFailed ? 1 : 0);
  sleep(0.01);
}
