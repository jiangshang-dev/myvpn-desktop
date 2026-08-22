import { execFile } from 'child_process'
import { promisify } from 'util'

const exec = promisify(execFile)

export const SOCKS_PORT = 10808
export const HTTP_PORT = 10809

let savedService: string | null = null

async function getActiveNetworkService(): Promise<string> {
  const { stdout } = await exec('networksetup', ['-listallnetworkservices'])
  const lines = stdout
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l && !l.startsWith('An asterisk') && !l.startsWith('*'))
  const wifi = lines.find((l) => /wi-?fi/i.test(l))
  return wifi || lines[0] || 'Wi-Fi'
}

export async function enableSystemProxy(): Promise<void> {
  if (process.platform === 'darwin') {
    const service = await getActiveNetworkService()
    savedService = service
    await exec('networksetup', ['-setwebproxy', service, '127.0.0.1', String(HTTP_PORT)])
    await exec('networksetup', ['-setsecurewebproxy', service, '127.0.0.1', String(HTTP_PORT)])
    await exec('networksetup', ['-setsocksfirewallproxy', service, '127.0.0.1', String(SOCKS_PORT)])
    await exec('networksetup', ['-setwebproxystate', service, 'on'])
    await exec('networksetup', ['-setsecurewebproxystate', service, 'on'])
    await exec('networksetup', ['-setsocksfirewallproxystate', service, 'on'])
    return
  }
  if (process.platform === 'win32') {
    await exec('reg', [
      'add',
      'HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings',
      '/v',
      'ProxyEnable',
      '/t',
      'REG_DWORD',
      '/d',
      '1',
      '/f',
    ])
    await exec('reg', [
      'add',
      'HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings',
      '/v',
      'ProxyServer',
      '/t',
      'REG_SZ',
      '/d',
      `127.0.0.1:${HTTP_PORT}`,
      '/f',
    ])
  }
}

export async function disableSystemProxy(): Promise<void> {
  if (process.platform === 'darwin') {
    const service = savedService || (await getActiveNetworkService())
    await exec('networksetup', ['-setwebproxystate', service, 'off'])
    await exec('networksetup', ['-setsecurewebproxystate', service, 'off'])
    await exec('networksetup', ['-setsocksfirewallproxystate', service, 'off'])
    savedService = null
    return
  }
  if (process.platform === 'win32') {
    await exec('reg', [
      'add',
      'HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings',
      '/v',
      'ProxyEnable',
      '/t',
      'REG_DWORD',
      '/d',
      '0',
      '/f',
    ])
  }
}
