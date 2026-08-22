import { app } from 'electron'
import { spawn, type ChildProcessWithoutNullStreams } from 'child_process'
import { chmodSync, existsSync, mkdirSync, writeFileSync } from 'fs'
import { join } from 'path'
import { disableSystemProxy, enableSystemProxy } from './system-proxy'

let xrayProc: ChildProcessWithoutNullStreams | null = null
let running = false

function xrayDir(): string {
  if (app.isPackaged) return join(process.resourcesPath, 'xray')
  return join(app.getAppPath(), 'resources', 'xray')
}

function xrayBinary(): string {
  const name = process.platform === 'win32' ? 'xray.exe' : 'xray'
  return join(xrayDir(), name)
}

export function isXrayInstalled(): boolean {
  return existsSync(xrayBinary())
}

export function isVpnRunning(): boolean {
  return running
}

export function buildXrayConfig(v2rayConfig: Record<string, unknown>) {
  const outbound = (v2rayConfig.outbound || v2rayConfig) as Record<string, unknown>
  return {
    log: { loglevel: 'warning' },
    inbounds: [
      {
        tag: 'socks-in',
        port: 10808,
        listen: '127.0.0.1',
        protocol: 'socks',
        settings: { udp: true },
      },
      {
        tag: 'http-in',
        port: 10809,
        listen: '127.0.0.1',
        protocol: 'http',
      },
    ],
    outbounds: [
      { ...outbound, tag: 'proxy' },
      { protocol: 'freedom', tag: 'direct' },
    ],
    routing: {
      domainStrategy: 'AsIs',
      rules: [{ type: 'field', inboundTag: ['socks-in', 'http-in'], outboundTag: 'proxy' }],
    },
  }
}

function waitForPort(ms = 2500): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function startVpn(v2rayConfig: Record<string, unknown>): Promise<void> {
  if (running) await stopVpn()

  const bin = xrayBinary()
  if (!existsSync(bin)) {
    throw new Error(
      '未找到 xray 核心，请在项目目录执行：pnpm run setup:xray',
    )
  }
  if (process.platform !== 'win32') {
    try { chmodSync(bin, 0o755) } catch { /* ignore */ }
  }

  const dir = xrayDir()
  mkdirSync(dir, { recursive: true })
  const configPath = join(dir, 'config.json')
  writeFileSync(configPath, JSON.stringify(buildXrayConfig(v2rayConfig), null, 2), 'utf-8')

  xrayProc = spawn(bin, ['run', '-c', configPath], {
    cwd: dir,
    stdio: ['ignore', 'pipe', 'pipe'],
  })

  const started = new Promise<void>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('xray 启动超时')), 8000)
    xrayProc?.once('spawn', () => {
      clearTimeout(timer)
      resolve()
    })
    xrayProc?.once('error', (err) => {
      clearTimeout(timer)
      reject(err)
    })
  })

  await started
  await waitForPort()
  await enableSystemProxy()
  running = true

  xrayProc.on('exit', () => {
    running = false
    xrayProc = null
    disableSystemProxy().catch(() => undefined)
  })
}

export async function stopVpn(): Promise<void> {
  if (xrayProc) {
    xrayProc.kill('SIGTERM')
    xrayProc = null
  }
  running = false
  await disableSystemProxy()
}
