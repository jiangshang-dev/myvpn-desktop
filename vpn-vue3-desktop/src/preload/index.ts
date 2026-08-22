import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('api', {
  getDevice: () => ipcRenderer.invoke('device:get') as Promise<{
    deviceUuid: string
    deviceName: string
    platform: string
  }>,
  startVpn: (v2rayConfig: Record<string, unknown>) =>
    ipcRenderer.invoke('vpn:start', v2rayConfig) as Promise<{ running: boolean }>,
  stopVpn: () => ipcRenderer.invoke('vpn:stop') as Promise<{ running: boolean }>,
  getVpnStatus: () => ipcRenderer.invoke('vpn:status') as Promise<{ running: boolean; xrayInstalled: boolean }>,
})
