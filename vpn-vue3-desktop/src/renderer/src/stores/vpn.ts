import { defineStore, acceptHMRUpdate } from 'pinia'
import { ref, computed } from 'vue'
import * as api from '../api/client'

export const useVpnStore = defineStore('vpn', () => {
  const email = ref('')
  const nickname = ref('')
  const loggedIn = ref(!!api.getToken())
  const nodes = ref<Array<{ id: number; name: string; region: string; onlineCount: number; available: boolean }>>([])
  const selectedNodeId = ref<number | null>(null)
  const sessionId = ref<number | null>(null)
  const connected = ref(false)
  const connectedAt = ref<number | null>(null)
  const uploadSpeed = ref(0)
  const downloadSpeed = ref(0)
  const statusText = ref('')
  const shareLink = ref('')
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let mockBytes = { up: 0, down: 0 }

  const durationText = computed(() => {
    if (!connectedAt.value) return '00:00:00'
    const sec = Math.floor((Date.now() - connectedAt.value) / 1000)
    const h = String(Math.floor(sec / 3600)).padStart(2, '0')
    const m = String(Math.floor((sec % 3600) / 60)).padStart(2, '0')
    const s = String(sec % 60).padStart(2, '0')
    return `${h}:${m}:${s}`
  })

  async function doRegister(e: string, p: string, nickname: string) {
    await api.register(e, p, nickname)
    await doLogin(e, p)
  }

  async function doLogin(e: string, p: string) {
    const data = await api.login(e, p)
    api.setToken(data.token)
    email.value = data.email
    nickname.value = data.nickname
    loggedIn.value = true
    await refreshNodes()
  }

  function logout() {
    stopHeartbeat()
    window.api.stopVpn().catch(() => undefined)
    api.clearToken()
    loggedIn.value = false
    connected.value = false
    sessionId.value = null
    shareLink.value = ''
  }

  async function refreshNodes() {
    nodes.value = await api.listNodes()
    if (!selectedNodeId.value && nodes.value.length) {
      selectedNodeId.value = nodes.value[0].id
    }
  }

  async function connect() {
    if (!selectedNodeId.value) throw new Error('请选择节点')
    statusText.value = '正在连接…'
    const data = await api.connect(selectedNodeId.value)
    try {
      await window.api.startVpn(data.v2rayConfig)
    } catch (e) {
      try { await api.disconnect(data.sessionId) } catch { /* ignore */ }
      throw e
    }
    sessionId.value = data.sessionId
    shareLink.value = data.shareLink
    connected.value = true
    connectedAt.value = Date.now()
    mockBytes = { up: 0, down: 0 }
    statusText.value = '已连接 · 系统代理已开启 (127.0.0.1:10809)'
    startHeartbeat()
  }

  async function disconnect() {
    try { await window.api.stopVpn() } catch { /* ignore */ }
    if (sessionId.value) {
      try { await api.disconnect(sessionId.value) } catch { /* ignore */ }
    }
    stopHeartbeat()
    connected.value = false
    sessionId.value = null
    connectedAt.value = null
    shareLink.value = ''
    statusText.value = '已断开 · 系统代理已关闭'
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(async () => {
      if (!sessionId.value) return
      mockBytes.up += Math.floor(Math.random() * 50000)
      mockBytes.down += Math.floor(Math.random() * 200000)
      uploadSpeed.value = Math.floor(Math.random() * 80000)
      downloadSpeed.value = Math.floor(Math.random() * 300000)
      try {
        const res = await api.heartbeat({
          sessionId: sessionId.value,
          uploadBytes: mockBytes.up,
          downloadBytes: mockBytes.down,
          uploadSpeed: uploadSpeed.value,
          downloadSpeed: downloadSpeed.value,
        })
        if (res.command === 'KICK') {
          statusText.value = res.message || '已被管理员断开'
          await disconnect()
        }
      } catch (e) {
        statusText.value = e instanceof Error ? e.message : '心跳失败'
      }
    }, 15000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }

  return {
    email, nickname, loggedIn, nodes, selectedNodeId, sessionId, connected,
    connectedAt, uploadSpeed, downloadSpeed, statusText, shareLink, durationText,
    doLogin, doRegister, logout, refreshNodes, connect, disconnect,
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useVpnStore, import.meta.hot))
}
