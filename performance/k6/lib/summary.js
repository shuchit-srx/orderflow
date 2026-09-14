export function sanitizedSummary(data) {
    const report = JSON.parse(JSON.stringify(data));

    delete report.setup_data;

    const file =
        __ENV.SUMMARY_FILE ||
        'performance/reports/k6-summary.json';

    return {
        [file]: JSON.stringify(report, null, 2),
    };
}