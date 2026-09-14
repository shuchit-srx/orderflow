import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from './lib/config.js';
import { sanitizedSummary } from './lib/summary.js';

export const options = {
    scenarios: {
        smoke: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 10,
            maxDuration: '1m',
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000', 'p(99)<1500'],
    },
};

export default function () {
    const health = http.get(`${BASE_URL}/actuator/health`, {
        tags: {
            endpoint: 'health',
        },
    });

    check(health, {
        'health status is 200': (r) => r.status === 200,
        'health body reports UP': (r) => r.body && r.body.includes('"status":"UP"'),
    });

    const products = http.get(`${BASE_URL}/api/v1/products`, {
        tags: {
            endpoint: 'products',
        },
    });

    check(products, {
        'products status is 200': (r) => r.status === 200,
        'products response is JSON': (r) => {
            try {
                r.json();
                return true;
            } catch (error) {
                return false;
            }
        },
    });

    sleep(0.5);
}

export function handleSummary(data) {
    return sanitizedSummary(data);
}