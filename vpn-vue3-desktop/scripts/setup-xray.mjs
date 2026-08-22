import { execSync } from 'child_process'
import { createWriteStream, existsSync, mkdirSync } from 'fs'
import { chmodSync, cpSync, rmSync } from 'fs'
import { pipeline } from 'stream/promises'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import { Readable } from 'stream'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const OUT_DIR = join(ROOT, 'resources', 'xray')
const VERSION = '25.8.3'

function assetName() {
  const { platform, arch } = process
  if (platform === 'darwin') {
    return arch === 'arm64' ? 'Xray-macos-arm64-v8a.zip' : 'Xray-macos-64.zip'
  }
  if (platform === 'win32') return 'Xray-windows-64.zip'
  if (platform === 'linux') return arch === 'arm64' ? 'Xray-linux-arm64-v8a.zip' : 'Xray-linux-64.zip'
  throw new Error(`不支持的平台: ${platform} ${arch}`)
}

async function download(url, dest) {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`下载失败 ${res.status}: ${url}`)
  await pipeline(Readable.fromWeb(res.body), createWriteStream(dest))
}

async function main() {
  const zipName = assetName()
  const url = `https://github.com/XTLS/Xray-core/releases/download/v${VERSION}/${zipName}`
  const zipPath = join(OUT_DIR, zipName)
  mkdirSync(OUT_DIR, { recursive: true })

  console.log(`下载 xray ${VERSION} ...`)
  console.log(url)
  await download(url, zipPath)

  const tmp = join(OUT_DIR, '_tmp')
  rmSync(tmp, { recursive: true, force: true })
  mkdirSync(tmp, { recursive: true })
  execSync(`unzip -o "${zipPath}" -d "${tmp}"`, { stdio: 'inherit' })

  const binName = process.platform === 'win32' ? 'xray.exe' : 'xray'
    for (const name of [binName, 'geoip.dat', 'geosite.dat']) {
    const src = join(tmp, name)
    if (existsSync(src)) cpSync(src, join(OUT_DIR, name), { force: true })
  }
  const binPath = join(OUT_DIR, binName)
  if (!existsSync(binPath)) throw new Error('解压后未找到 xray 可执行文件')
  if (process.platform !== 'win32') chmodSync(binPath, 0o755)

  rmSync(tmp, { recursive: true, force: true })
  rmSync(zipPath, { force: true })
  console.log(`xray 已安装到 ${OUT_DIR}`)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
