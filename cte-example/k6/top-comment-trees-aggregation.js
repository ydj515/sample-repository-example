import http from 'k6/http';
import {check} from 'k6';
import {Trend} from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const POST_ID = __ENV.POST_ID || '1';
const LIMIT_MIN = Number(__ENV.LIMIT_MIN || '1');
const LIMIT_MAX = Number(__ENV.LIMIT_MAX || __ENV.LIMIT || '3');

export const options = {
    stages: buildStages(),
    tags: {endpoint: 'aggregation'},
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        aggregation_service_duration: ['p(95)<300'],
    },
};

const aggregationServiceDuration = new Trend('aggregation_service_duration');

export default function () {
    const limit = nextLimit();
    const response = http.get(
        `${BASE_URL}/api/posts/${POST_ID}/comments/top-trees/aggregation?limit=${limit}`,
        {tags: {endpoint: 'aggregation'}}
    );

    let payload;
    try {
        payload = response.json();
    } catch (error) {
        payload = null;
    }

    check(response, {
        'status 200': (res) => res.status === 200,
        'has duration metric': () => Boolean(payload && payload.durationMillis !== undefined),
    });

    if (payload && payload.durationMillis !== undefined) {
        aggregationServiceDuration.add(payload.durationMillis);
    }
}

function nextLimit() {
    if (LIMIT_MIN === LIMIT_MAX) {
        return LIMIT_MIN;
    }
    const min = Math.min(LIMIT_MIN, LIMIT_MAX);
    const max = Math.max(LIMIT_MIN, LIMIT_MAX);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function buildStages() {
    const stageEnv = __ENV.STAGES || __ENV.RAMP_STAGES;
    if (!stageEnv) {
        return [
            {duration: '30s', target: Number(__ENV.START_VUS || 5)},
            {duration: '1m', target: Number(__ENV.PEAK_VUS || 25)},
            {duration: '30s', target: Number(__ENV.PEAK_VUS || 100)},
            {duration: '30s', target: 0},
        ];
    }

    return stageEnv.split(',').map((segment) => {
        const [duration, target] = segment.split(':');
        const trimmedDuration = duration.trim();
        const trimmedTarget = Number(target);
        if (!trimmedDuration || Number.isNaN(trimmedTarget)) {
            throw new Error(`Invalid stage segment "${segment}". Expected format "duration:vus" (e.g., "30s:20").`);
        }
        return {duration: trimmedDuration, target: trimmedTarget};
    });
}
