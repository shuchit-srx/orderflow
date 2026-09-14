export const BASE_URL = __ENV.BASE_URL || 'http://orderflow-dev-alb-1545040109.ap-south-1.elb.amazonaws.com';

export const JSON_HEADERS = {
    'Content-Type': 'application/json',
};

export function bearerHeaders(token) {
    return {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
    };
}

export const COMMON_THRESHOLDS = {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750', 'p(99)<1500'],
};