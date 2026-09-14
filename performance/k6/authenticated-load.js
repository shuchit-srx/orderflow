import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, bearerHeaders } from './lib/config.js';
import { login } from './lib/auth.js';
import { sanitizedSummary } from './lib/summary.js';

export const options = {
    scenarios: {
        authenticated_read_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 5 },
                { duration: '2m', target: 15 },
                { duration: '2m', target: 30 },
                { duration: '3m', target: 30 },
                { duration: '1m', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        'http_req_duration{endpoint:orders-list}': ['p(95)<1000', 'p(99)<2000'],
    },
};

export function setup() {
    return {
        token: login(),
    };
}

export default function (data) {
    const orders = http.get(`${BASE_URL}/api/v1/orders`, {
        headers: bearerHeaders(data.token),
        tags: {
            endpoint: 'orders-list',
        },
    });

    check(orders, {
        'orders status is 200': (r) => r.status === 200,
        'orders response is JSON': (r) => {
            try {
                r.json();
                return true;
            } catch (error) {
                return false;
            }
        },
    });

    sleep(1);
}

export function handleSummary(data) {
    return sanitizedSummary(data);
}