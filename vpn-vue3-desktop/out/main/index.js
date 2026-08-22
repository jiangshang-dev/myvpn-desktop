"use strict";
const electron = require("electron");
const path = require("path");
const crypto = require("crypto");
const fs = require("fs");
const os = require("os");
const child_process = require("child_process");
const util = require("util");
const exec = util.promisify(child_process.execFile);
const SOCKS_PORT = 10808;
const HTTP_PORT = 10809;
let savedService = null;
async function getActiveNetworkService() {
  const { stdout } = await exec("networksetup", ["-listallnetworkservices"]);
  const lines = stdout.split("\n").map((l) => l.trim()).filter((l) => l && !l.startsWith("An asterisk") && !l.startsWith("*"));
  const wifi = lines.find((l) => /wi-?fi/i.test(l));
  return wifi || lines[0] || "Wi-Fi";
}
async function enableSystemProxy() {
  if (process.platform === "darwin") {
    const service = await getActiveNetworkService();
    savedService = service;
    await exec("networksetup", ["-setwebproxy", service, "127.0.0.1", String(HTTP_PORT)]);
    await exec("networksetup", ["-setsecurewebproxy", service, "127.0.0.1", String(HTTP_PORT)]);
    await exec("networksetup", ["-setsocksfirewallproxy", service, "127.0.0.1", String(SOCKS_PORT)]);
    await exec("networksetup", ["-setwebproxystate", service, "on"]);
    await exec("networksetup", ["-setsecurewebproxystate", service, "on"]);
    await exec("networksetup", ["-setsocksfirewallproxystate", service, "on"]);
    return;
  }
  if (process.platform === "win32") {
    await exec("reg", [
      "add",
      "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
      "/v",
      "ProxyEnable",
      "/t",
      "REG_DWORD",
      "/d",
      "1",
      "/f"
    ]);
    await exec("reg", [
      "add",
      "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
      "/v",
      "ProxyServer",
      "/t",
      "REG_SZ",
      "/d",
      `127.0.0.1:${HTTP_PORT}`,
      "/f"
    ]);
  }
}
async function disableSystemProxy() {
  if (process.platform === "darwin") {
    const service = savedService || await getActiveNetworkService();
    await exec("networksetup", ["-setwebproxystate", service, "off"]);
    await exec("networksetup", ["-setsecurewebproxystate", service, "off"]);
    await exec("networksetup", ["-setsocksfirewallproxystate", service, "off"]);
    savedService = null;
    return;
  }
  if (process.platform === "win32") {
    await exec("reg", [
      "add",
      "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
      "/v",
      "ProxyEnable",
      "/t",
      "REG_DWORD",
      "/d",
      "0",
      "/f"
    ]);
  }
}
let xrayProc = null;
let running = false;
function xrayDir() {
  if (electron.app.isPackaged) return path.join(process.resourcesPath, "xray");
  return path.join(electron.app.getAppPath(), "resources", "xray");
}
function xrayBinary() {
  const name = process.platform === "win32" ? "xray.exe" : "xray";
  return path.join(xrayDir(), name);
}
function isXrayInstalled() {
  return fs.existsSync(xrayBinary());
}
function isVpnRunning() {
  return running;
}
function buildXrayConfig(v2rayConfig) {
  const outbound = v2rayConfig.outbound || v2rayConfig;
  return {
    log: { loglevel: "warning" },
    inbounds: [
      {
        tag: "socks-in",
        port: 10808,
        listen: "127.0.0.1",
        protocol: "socks",
        settings: { udp: true }
      },
      {
        tag: "http-in",
        port: 10809,
        listen: "127.0.0.1",
        protocol: "http"
      }
    ],
    outbounds: [
      { ...outbound, tag: "proxy" },
      { protocol: "freedom", tag: "direct" }
    ],
    routing: {
      domainStrategy: "AsIs",
      rules: [{ type: "field", inboundTag: ["socks-in", "http-in"], outboundTag: "proxy" }]
    }
  };
}
function waitForPort(ms = 2500) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
async function startVpn(v2rayConfig) {
  if (running) await stopVpn();
  const bin = xrayBinary();
  if (!fs.existsSync(bin)) {
    throw new Error(
      "未找到 xray 核心，请在项目目录执行：pnpm run setup:xray"
    );
  }
  if (process.platform !== "win32") {
    try {
      fs.chmodSync(bin, 493);
    } catch {
    }
  }
  const dir = xrayDir();
  fs.mkdirSync(dir, { recursive: true });
  const configPath = path.join(dir, "config.json");
  fs.writeFileSync(configPath, JSON.stringify(buildXrayConfig(v2rayConfig), null, 2), "utf-8");
  xrayProc = child_process.spawn(bin, ["run", "-c", configPath], {
    cwd: dir,
    stdio: ["ignore", "pipe", "pipe"]
  });
  const started = new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("xray 启动超时")), 8e3);
    xrayProc?.once("spawn", () => {
      clearTimeout(timer);
      resolve();
    });
    xrayProc?.once("error", (err) => {
      clearTimeout(timer);
      reject(err);
    });
  });
  await started;
  await waitForPort();
  await enableSystemProxy();
  running = true;
  xrayProc.on("exit", () => {
    running = false;
    xrayProc = null;
    disableSystemProxy().catch(() => void 0);
  });
}
async function stopVpn() {
  if (xrayProc) {
    xrayProc.kill("SIGTERM");
    xrayProc = null;
  }
  running = false;
  await disableSystemProxy();
}
const isDev = !electron.app.isPackaged;
const deviceFile = () => path.join(electron.app.getPath("userData"), "device.json");
function loadDeviceId() {
  const file = deviceFile();
  if (fs.existsSync(file)) {
    try {
      const data = JSON.parse(fs.readFileSync(file, "utf-8"));
      if (data.deviceUuid) return data.deviceUuid;
    } catch {
    }
  }
  const id = crypto.randomUUID();
  fs.mkdirSync(electron.app.getPath("userData"), { recursive: true });
  fs.writeFileSync(deviceFile(), JSON.stringify({ deviceUuid: id }), "utf-8");
  return id;
}
function createWindow() {
  const win = new electron.BrowserWindow({
    width: 420,
    height: 720,
    minWidth: 380,
    minHeight: 600,
    title: "MyVPN",
    backgroundColor: "#0f172a",
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, "../preload/index.js"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  win.on("ready-to-show", () => win.show());
  if (isDev && process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    win.loadFile(path.join(__dirname, "../renderer/index.html"));
  }
}
electron.app.whenReady().then(() => {
  const deviceUuid = loadDeviceId();
  electron.ipcMain.handle("device:get", () => ({
    deviceUuid,
    deviceName: os.hostname(),
    platform: process.platform === "darwin" ? "macos" : "windows"
  }));
  electron.ipcMain.handle("vpn:status", () => ({
    running: isVpnRunning(),
    xrayInstalled: isXrayInstalled()
  }));
  electron.ipcMain.handle("vpn:start", async (_e, v2rayConfig) => {
    await startVpn(v2rayConfig);
    return { running: true };
  });
  electron.ipcMain.handle("vpn:stop", async () => {
    await stopVpn();
    return { running: false };
  });
  createWindow();
});
electron.app.on("before-quit", () => {
  stopVpn().catch(() => void 0);
});
electron.app.on("window-all-closed", () => {
  if (process.platform !== "darwin") electron.app.quit();
});
