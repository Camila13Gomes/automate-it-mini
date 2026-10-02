import { apiFetch, clearCsrf, refreshCsrf } from './apiClient'

export async function getCurrentUser() {
  return apiFetch('/auth/me')
}

export async function login(credentials) {
  await refreshCsrf()
  return apiFetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  })
}

export async function logout() {
  await apiFetch('/auth/logout', { method: 'POST' })
  clearCsrf()
}
