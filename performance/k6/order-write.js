import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';
import { BASE_URL, bearerHeaders } from './lib/config.js';
import { login } from './lib/auth.js';
import { sanitizedSummary } from './lib/summary.js';

export const options = {
    scenarios: {
        order_write: {
            executor: 'constant-arrival-rate',
            rate: 20,
            timeUnit: '1m',
            duration: '5m',
            preAllocatedVUs: 5,
            maxVUs: 20,
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1500', 'p(99)<3000'],
        'http_req_duration{endpoint:order-create}': ['p(95)<1500', 'p(99)<3000'],
    },
};

export function setup() {
    if (!__ENV.ORDER_BODY) {
        fail('ORDER_BODY is required');
    }

    try {
        JSON.parse(__ENV.ORDER_BODY);
    } catch (error) {
        fail('ORDER_BODY must be valid JSON');
    }

    return {
        token: login(),
    };
}

export default function (data) {
    const idempotencyKey = `k6-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;

    const response = http.post(
        `${BASE_URL}/api/v1/orders`,
        __ENV.ORDER_BODY,
        {
            headers: {
                ...bearerHeaders(data.token),
                'Idempotency-Key': idempotencyKey,
            },
            tags: {
                endpoint: 'order-create',
            },
        },
    );

    check(response, {
        'order create status is 201': (r) => r.status === 201,
        'order is confirmed': (r) => {
            try {
                return r.json('status') === 'CONFIRMED';
            } catch (_) {
                return false;
            }
        },
    });
}

export function handleSummary(data) {
    return sanitizedSummary(data);
}