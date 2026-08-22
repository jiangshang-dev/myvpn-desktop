/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}

interface Window {
  api: {
    getDevice: () => Promise<{ deviceUuid: string; deviceName: string; platform: string }>
    startVpn: (v2rayConfig: Record<string, unknown>) => Promise<{ running: boolean }>
    stopVpn: () => Promise<{ running: boolean }>
    getVpnStatus: () => Promise<{ running: boolean; xrayInstalled: boolean }>
  }
}
