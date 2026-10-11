import { apiFetch } from './apiClient'

export function getProjects() {
  return apiFetch('/projects')
}

export function createProject(project) {
  return apiFetch('/projects', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(project),
  })
}

export function getUsers() {
  return apiFetch('/users')
}

export function createUser(user) {
  return apiFetch('/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  })
}

export function updateUser(id, user) {
  return apiFetch(`/users/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  })
}

export function setUserEnabled(id, enabled) {
  return apiFetch(`/users/${id}/enabled`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled }),
  })
}

export function resetUserPassword(id, password) {
  return apiFetch(`/users/${id}/password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  })
}

export function deleteUser(id) {
  return apiFetch(`/users/${id}`, { method: 'DELETE' })
}
