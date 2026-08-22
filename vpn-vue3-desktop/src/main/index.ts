import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'path'
import { randomUUID } from 'crypto'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs'
import os from 'os'
import { isVpnRunning, isXrayInstalled, startVpn, stopVpn } from './xray-manager'

const isDev = !app.isPackaged
const deviceFile = () => join(app.getPath('userData'), 'device.json')

function loadDeviceId(): string {
  const file = deviceFile()
  if (existsSync(file)) {
    try {
      const data = JSON.parse(readFileSync(file, 'utf-8'))
      if (data.deviceUuid) return data.deviceUuid
    } catch { /* ignore */ }
  }
  const id = randomUUID()
  mkdirSync(app.getPath('userData'), { recursive: true })
  writeFileSync(deviceFile(), JSON.stringify({ deviceUuid: id }), 'utf-8')
  return id
}

function createWindow(): void {
  const win = new BrowserWindow({
    width: 420,
    height: 720,
    minWidth: 380,
    minHeight: 600,
    title: 'MyVPN',
    backgroundColor: '#0f172a',
    autoHideMenuBar: true,
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  })
  win.on('ready-to-show', () => win.show())
  if (isDev && process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    win.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(() => {
  const deviceUuid = loadDeviceId()
  ipcMain.handle('device:get', () => ({
    deviceUuid,
    deviceName: os.hostname(),
    platform: process.platform === 'darwin' ? 'macos' : 'windows',
  }))
  ipcMain.handle('vpn:status', () => ({
    running: isVpnRunning(),
    xrayInstalled: isXrayInstalled(),
  }))
  ipcMain.handle('vpn:start', async (_e, v2rayConfig: Record<string, unknown>) => {
    await startVpn(v2rayConfig)
    return { running: true }
  })
  ipcMain.handle('vpn:stop', async () => {
    await stopVpn()
    return { running: false }
  })
  createWindow()
})

app.on('before-quit', () => {
  stopVpn().catch(() => undefined)
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
