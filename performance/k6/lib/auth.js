import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE_URL, JSON_HEADERS } from './config.js';

export function login() {
    const email = __ENV.PERF_EMAIL;
    const password = __ENV.PERF_PASSWORD;

    if (!email || !password) {
        fail('PERF_EMAIL and PERF_PASSWORD are required');
    }

    const response = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email,
            password,
        }),
        {
            headers: JSON_HEADERS,
            tags: {
                endpoint: 'login',
            },
        },
    );

    const ok = check(response, {
        'login status is 200': (r) => r.status === 200,
    });

    if (!ok) {
        fail(`Login failed with status ${response.status}`);
    }

    let body;

    try {
        body = response.json();
    } catch (error) {
        fail('Login response is not valid JSON');
    }

    if (!body.accessToken) {
        fail('Login response does not contain accessToken');
    }

    return body.accessToken;
}