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
