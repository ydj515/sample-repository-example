import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '1m',
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKeys = [
  'demo-key-client-01',
  'demo-key-client-02',
  'demo-key-client-03',
  'demo-key-client-04',
  'demo-key-client-05',
  'demo-key-client-06',
  'demo-key-client-07',
  'demo-key-client-08',
  'demo-key-client-09',
  'demo-key-client-10',
];

export default function () {
  const index = Math.floor(Math.random() * apiKeys.length);
  const apiKey = apiKeys[index];
  const headers = { 'X-API-Key': apiKey, 'Content-Type': 'application/json' };
  const scenario = Math.floor(Math.random() * 100);

  let response;
  if (scenario < 35) {
    response = http.get(`${baseUrl}/api/products`, { headers });
  } else if (scenario < 55) {
    const productId = 1 + Math.floor(Math.random() * 50);
    response = http.get(`${baseUrl}/api/products/${productId}`, { headers });
  } else if (scenario < 70) {
    response = http.post(`${baseUrl}/api/orders`, JSON.stringify({ productId: 1, quantity: 1 }), { headers });
  } else if (scenario < 82) {
    response = http.get(`${baseUrl}/api/reports/sales`, { headers });
  } else if (scenario < 90) {
    response = http.get(`${baseUrl}/api/admin/health-check`, { headers });
  } else if (scenario < 96) {
    response = http.get(`${baseUrl}/api/unstable`, { headers });
  } else if (scenario < 98) {
    response = http.get(`${baseUrl}/api/products`);
  } else {
    response = http.get(`${baseUrl}/api/products`, { headers: { 'X-API-Key': 'invalid-demo-key' } });
  }

  check(response, {
    'status is expected sample result': (res) => [200, 201, 401, 403, 500].includes(res.status),
  });

  sleep(0.2);
}
