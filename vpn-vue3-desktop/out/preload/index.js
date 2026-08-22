"use strict";
const electron = require("electron");
electron.contextBridge.exposeInMainWorld("api", {
  getDevice: () => electron.ipcRenderer.invoke("device:get"),
  startVpn: (v2rayConfig) => electron.ipcRenderer.invoke("vpn:start", v2rayConfig),
  stopVpn: () => electron.ipcRenderer.invoke("vpn:stop"),
  getVpnStatus: () => electron.ipcRenderer.invoke("vpn:status")
});
