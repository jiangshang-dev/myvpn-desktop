const BASE = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:9010'
const TOKEN_KEY = 'myvpn.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(t: string) {
  localStorage.setItem(TOKEN_KEY, t)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(options.headers as Record<string, string>) }
  if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, { ...options, headers })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error((data as { message?: string }).message || `请求失败 ${res.status}`)
  return data as T
}

export async function register(email: string, password: string, nickname = '') {
  return request<{ email: string; nickname: string }>(
    '/api/client/register',
    {
      method: 'POST',
      body: JSON.stringify({ email, password, nickname }),
    },
  )
}

export async function login(email: string, password: string) {
  const device = await window.api.getDevice()
  return request<{ token: string; email: string; nickname: string; deviceId: number }>(
    '/api/client/login',
    {
      method: 'POST',
      body: JSON.stringify({ email, password, ...device }),
    },
  )
}

export async function listNodes() {
  return request<Array<{
    id: number
    name: string
    region: string
    onlineCount: number
    available: boolean
  }>>('/api/client/nodes')
}

export async function testNode(nodeId: number) {
  return request<{
    nodeId: number
    nodeName: string
    tcpOk: boolean
    tcpLatencyMs: number
    proxyOk: boolean
    proxyLatencyMs: number
    speedBytesPerSec: number
    error: string | null
    warnings: string[]
  }>(`/api/client/nodes/${nodeId}/test`, { method: 'POST' })
}

export async function connect(nodeId: number) {
  return request<{ sessionId: number; status: string; shareLink: string; v2rayConfig: Record<string, unknown> }>(
    '/api/client/connect',
    { method: 'POST', body: JSON.stringify({ nodeId }) },
  )
}

export async function heartbeat(payload: {
  sessionId: number
  uploadBytes: number
  downloadBytes: number
  uploadSpeed: number
  downloadSpeed: number
}) {
  return request<{ command: string; message: string }>(
    '/api/client/heartbeat',
    { method: 'POST', body: JSON.stringify(payload) },
  )
}

export async function disconnect(sessionId: number) {
  return request('/api/client/disconnect', {
    method: 'POST',
    body: JSON.stringify({ sessionId, reason: 'user' }),
  })
}
