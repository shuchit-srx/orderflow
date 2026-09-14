import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from './lib/config.js';
import { sanitizedSummary } from './lib/summary.js';

export const options = {
    scenarios: {
        public_read_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 10 },
                { duration: '2m', target: 25 },
                { duration: '2m', target: 50 },
                { duration: '3m', target: 50 },
                { duration: '1m', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<750', 'p(99)<1500'],
        'http_req_duration{endpoint:products}': ['p(95)<750', 'p(99)<1500'],
    },
};

export default function () {
    const products = http.get(`${BASE_URL}/api/v1/products`, {
        tags: {
            endpoint: 'products',
        },
    });

    check(products, {
        'products status is 200': (r) => r.status === 200,
    });

    sleep(1);
}

export function handleSummary(data) {
    return sanitizedSummary(data);
}