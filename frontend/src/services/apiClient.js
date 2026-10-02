export const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8081/api'

let csrf = null

async function parseResponse(response) {
  if (response.status === 204) return null
  const body = await response.json().catch(() => null)
  if (response.ok) return body

  const validationMessage = body?.validationErrors
    ? Object.values(body.validationErrors).join(' ')
    : null
  const error = new Error(validationMessage || body?.message || `Request failed (${response.status})`)
  error.status = response.status
  throw error
}

export async function refreshCsrf() {
  const response = await fetch(`${API_BASE_URL}/auth/csrf`, { credentials: 'include' })
  csrf = await parseResponse(response)
  return csrf
}

export async function apiFetch(path, options = {}) {
  const method = options.method ?? 'GET'
  const headers = { ...(options.headers ?? {}) }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())) {
    if (!csrf) await refreshCsrf()
    headers[csrf.headerName] = csrf.token
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: 'include',
  })
  return parseResponse(response)
}

export function clearCsrf() {
  csrf = null
}
