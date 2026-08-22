const TOKEN_KEY = "myvpn.admin.token";
const USER_KEY = "myvpn.admin.user";

const NAV = [
  { id: "dashboard", href: "./index.html", label: "概览" },
  { id: "sessions", href: "./sessions.html", label: "在线会话" },
  { id: "users", href: "./users.html", label: "用户" },
  { id: "devices", href: "./devices.html", label: "设备" },
  { id: "nodes", href: "./nodes.html", label: "节点" },
];

function isLoginPage() {
  return location.pathname.endsWith("login.html");
}

function token() {
  return sessionStorage.getItem(TOKEN_KEY) || "";
}

function currentUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || "null");
  } catch {
    return null;
  }
}

window.Admin = {
  user: currentUser,
  esc(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  },
  fmt(v) {
    if (!v) return "";
    return String(v).replace("T", " ").replace(/\.\d+/, "").slice(0, 19);
  },
  fmtSpeed(bytes) {
    const n = Number(bytes || 0);
    if (n < 1024) return n + " B/s";
    if (n < 1024 * 1024) return (n / 1024).toFixed(1) + " KB/s";
    return (n / 1024 / 1024).toFixed(2) + " MB/s";
  },
  fmtDuration(sec) {
    const s = Number(sec || 0);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const r = s % 60;
    if (h > 0) return `${h}时${m}分`;
    if (m > 0) return `${m}分${r}秒`;
    return `${r}秒`;
  },
  toast(text) {
    let el = document.getElementById("toast");
    if (!el) {
      el = document.createElement("div");
      el.id = "toast";
      el.className = "toast";
      document.body.appendChild(el);
    }
    el.hidden = false;
    el.textContent = text;
    clearTimeout(this._t);
    this._t = setTimeout(() => { el.hidden = true; }, 2200);
  },
  saveLogin(data) {
    sessionStorage.setItem(TOKEN_KEY, data.token || "");
    sessionStorage.setItem(USER_KEY, JSON.stringify({ account: data.account, name: data.name }));
  },
  async logout() {
    try {
      await fetch("/api/admin/logout", { method: "POST", headers: { Authorization: "Bearer " + token() } });
    } catch { /* ignore */ }
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    location.href = "./login.html";
  },
  async api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (token()) headers.Authorization = "Bearer " + token();
    if (options.body && !headers["Content-Type"] && !(options.body instanceof FormData)) {
      headers["Content-Type"] = "application/json";
    }
    const res = await fetch(path, { ...options, headers });
    const data = await res.json().catch(() => ({}));
    if (res.status === 401) {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
      if (!isLoginPage()) location.href = "./login.html";
      throw new Error(data.message || "请先登录");
    }
    if (!res.ok) throw new Error(data.message || "请求失败");
    return data;
  },
};

function wrapLayout() {
  const page = document.getElementById("page");
  if (!page) return;
  const nav = page.dataset.nav || "";
  const title = page.dataset.title || "管理后台";
  const inner = page.innerHTML;
  const user = currentUser();
  document.getElementById("app").innerHTML = `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-logo">V</div>
          <div><h2>MyVPN</h2><p>管控后台</p></div>
        </div>
        <nav class="nav">
          ${NAV.map((n) => `<a class="nav-item${n.id === nav ? " active" : ""}" href="${n.href}">${n.label}</a>`).join("")}
        </nav>
      </aside>
      <div class="main">
        <header class="topbar">
          <h1>${title}</h1>
          <div class="topbar-right">
            <span>${Admin.esc(user?.name || user?.account || "")}</span>
            <button class="btn ghost" type="button" onclick="Admin.logout()">退出</button>
          </div>
        </header>
        <div class="content">${inner}</div>
      </div>
    </div>`;
}

document.addEventListener("DOMContentLoaded", async () => {
  if (isLoginPage()) return;
  wrapLayout();
  if (!token()) {
    location.href = "./login.html";
    return;
  }
  try {
    await Admin.api("/api/admin/me");
    document.dispatchEvent(new CustomEvent("admin:ready"));
  } catch {
    location.href = "./login.html";
  }
});
