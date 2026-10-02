import { apiFetch } from './apiClient'

export async function getTestRuns(projectId, options = {}) {
  const params = new URLSearchParams({
    projectId, page: String(options.page ?? 0), size: String(options.size ?? 10), sort: 'createdAt,desc',
  })
  for (const key of ['q', 'status', 'environment', 'executionType', 'testCaseId']) {
    if (options[key]) params.set(key, options[key])
  }
  if (options.finalOnly) params.set('finalOnly', 'true')
  return apiFetch(`/test-runs?${params}`)
}

export function updateTestRun(id, testRun) {
  return apiFetch(`/test-runs/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(testRun),
  })
}

export function updateTestRunDetails(id, details) {
  return apiFetch(`/test-runs/${id}/details`, {
    method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(details),
  })
}

export function deleteTestRun(id) {
  return apiFetch(`/test-runs/${id}`, { method: 'DELETE' })
}

export async function createTestRun(testRun) {
  return apiFetch('/test-runs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(testRun),
  })
}

export async function updateTestRunStatus(id, status) {
  return apiFetch(`/test-runs/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
}
