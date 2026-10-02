import { apiFetch } from './apiClient'

export async function getTestCases(projectId, options = {}) {
  const params = new URLSearchParams({
    projectId, page: String(options.page ?? 0), size: String(options.size ?? 10), sort: 'createdAt,desc',
  })
  if (options.q) params.set('q', options.q)
  if (options.method) params.set('method', options.method)
  return apiFetch(`/test-cases?${params}`)
}

export async function getAllTestCases(projectId) {
  const items = []
  let page = 0
  while (true) {
    const result = await getTestCases(projectId, { page, size: 100 })
    items.push(...result.content)
    page += 1
    if (page >= result.page.totalPages) break
  }
  return items
}

export function updateTestCase(id, testCase) {
  return apiFetch(`/test-cases/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(testCase),
  })
}

export function deleteTestCase(id) {
  return apiFetch(`/test-cases/${id}`, { method: 'DELETE' })
}

export async function createTestCase(testCase) {
  return apiFetch('/test-cases', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(testCase),
  })
}
