-- Stale-running recovery (review of #45): record when a scan was claimed (Pending →
-- Running). The scans table is the work queue and only Pending scans are picked up,
-- so a worker that claimed a scan and then died would leave it Running forever. This
-- timestamp lets the worker fail scans stuck Running past a timeout.
ALTER TABLE api_service.scans ADD COLUMN started_at TIMESTAMP WITH TIME ZONE;
