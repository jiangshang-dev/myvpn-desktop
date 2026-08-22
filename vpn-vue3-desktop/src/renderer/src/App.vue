<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useVpnStore } from './stores/vpn'
import * as api from './api/client'

const store = useVpnStore()
const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const nickname = ref('')
const err = ref('')
const ok = ref('')
const xrayReady = ref(true)

onMounted(async () => {
  try {
    const status = await window.api.getVpnStatus()
    xrayReady.value = status.xrayInstalled
  } catch {
    xrayReady.value = false
  }
})

async function onLogin() {
  err.value = ''
  ok.value = ''
  try {
    await store.doLogin(email.value, password.value)
  } catch (e) {
    err.value = e instanceof Error ? e.message : '登录失败'
  }
}

async function onRegister() {
  err.value = ''
  ok.value = ''
  try {
    await api.register(email.value, password.value, nickname.value)
    await onLogin()
  } catch (e) {
    err.value = e instanceof Error ? e.message : '注册失败'
  }
}

function switchMode(next: 'login' | 'register') {
  mode.value = next
  err.value = ''
  ok.value = ''
}

async function onConnect() {
  err.value = ''
  try {
    await store.connect()
  } catch (e) {
    err.value = e instanceof Error ? e.message : '连接失败'
  }
}

async function onTestNode() {
  if (!store.selectedNodeId) {
    err.value = '请先选择节点'
    return
  }
  err.value = ''
  ok.value = '正在测速…'
  try {
    const data = await api.testNode(store.selectedNodeId)
    if (data.proxyOk) {
      ok.value = `延迟 ${data.proxyLatencyMs} ms · ${(data.speedBytesPerSec / 1024 / 1024).toFixed(1)} MB/s`
    } else {
      const hint = [...(data.warnings || []), data.error].filter(Boolean).join('；')
      err.value = hint || '测速失败'
      ok.value = data.tcpOk ? `TCP 可达 ${data.tcpLatencyMs} ms，但代理不通` : ''
    }
  } catch (e) {
    ok.value = ''
    err.value = e instanceof Error ? e.message : '测速失败'
  }
}

async function onDisconnect() {
  err.value = ''
  try {
    await store.disconnect()
  } catch (e) {
    err.value = e instanceof Error ? e.message : '断开失败'
  }
}

function copyShareLink() {
  if (!store.shareLink) return
  navigator.clipboard.writeText(store.shareLink).then(() => {
    ok.value = '已复制 vmess 链接'
    setTimeout(() => { ok.value = '' }, 3000)
  }).catch(() => {
    err.value = '复制失败，请手动复制下方链接'
  })
}

function fmtSpeed(n: number) {
  if (n < 1024) return `${n} B/s`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB/s`
  return `${(n / 1024 / 1024).toFixed(2)} MB/s`
}
</script>

<template>
  <div class="app">
    <header class="top">
      <h1>MyVPN</h1>
      <p v-if="store.loggedIn">{{ store.nickname || store.email }}</p>
    </header>

    <section v-if="!store.loggedIn" class="card">
      <div class="tabs">
        <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
      </div>

      <template v-if="mode === 'login'">
        <input v-model="email" type="email" placeholder="邮箱" autocomplete="username" />
        <input v-model="password" type="password" placeholder="密码（至少 6 位）" autocomplete="current-password" />
        <button class="primary" type="button" @click="onLogin">登录</button>
      </template>

      <template v-else>
        <input v-model="email" type="email" placeholder="邮箱" autocomplete="username" />
        <input v-model="nickname" type="text" placeholder="昵称（可选）" />
        <input v-model="password" type="password" placeholder="密码（至少 6 位）" autocomplete="new-password" />
        <button class="primary" type="button" @click="onRegister">注册并登录</button>
        <p class="hint">注册成功后会自动登录本机设备。</p>
      </template>

      <p class="err">{{ err }}</p>
      <p class="ok">{{ ok }}</p>
    </section>

    <template v-else>
      <section class="card status" :class="{ on: store.connected }">
        <div class="dot"></div>
        <div>
          <strong>{{ store.connected ? '已连接' : '未连接' }}</strong>
          <p>{{ store.statusText || '选择节点后点击连接' }}</p>
          <p v-if="!xrayReady" class="warn">⚠ 未安装 xray 核心，请在终端执行：pnpm run setup:xray</p>
          <p v-if="store.connected">时长 {{ store.durationText }}</p>
          <p v-if="store.connected">↓ {{ fmtSpeed(store.downloadSpeed) }} · ↑ {{ fmtSpeed(store.uploadSpeed) }}</p>
        </div>
      </section>

      <section class="card">
        <h2>节点</h2>
        <button class="ghost" type="button" @click="store.refreshNodes()">刷新</button>
        <button class="ghost" type="button" @click="onTestNode">⚡ 测速</button>
        <ul>
          <li
            v-for="n in store.nodes"
            :key="n.id"
            :class="{ active: store.selectedNodeId === n.id, disabled: !n.available }"
            @click="n.available && (store.selectedNodeId = n.id)"
          >
            <span>{{ n.name }}</span>
            <small>{{ n.region }} · 在线 {{ n.onlineCount }}</small>
          </li>
        </ul>
      </section>

      <div class="actions">
        <button v-if="!store.connected" class="primary" type="button" :disabled="!xrayReady" @click="onConnect">连接</button>
        <button v-else class="danger" type="button" @click="onDisconnect">断开</button>
        <button class="ghost" type="button" @click="store.logout()">退出</button>
      </div>

      <p v-if="err" class="err">{{ err }}</p>

      <section v-if="store.shareLink" class="card config-card">
        <h2>节点配置</h2>
        <p class="hint">vmess 分享链接（备用，可导入 v2rayN）</p>
        <textarea class="share-link" readonly :value="store.shareLink" rows="3"></textarea>
        <button class="ghost" type="button" @click="copyShareLink">复制 vmess 链接</button>
      </section>

      <p v-if="ok" class="ok">{{ ok }}</p>
    </template>
  </div>
</template>
