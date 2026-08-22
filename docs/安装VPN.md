# 安装VPN

https://github\.com/2dust/v2rayN/releases

https://github\.com/v2fly/fhs\-install\-v2ray

https://github\.com/v2fly/v2ray\-examples?tab=readme\-ov\-file

先有一个港区或者其他地区云服务器

第一步

```Plain Text
// 安裝執行檔和 .dat 資料檔
# bash <(curl -L https://raw.githubusercontent.com/v2fly/fhs-install-v2ray/master/install-release.sh)
```

第二步：配置环境

修改配置文件：

/usr/local/etc/v2ray/config\.json

注意：clients\.id后面随机生成一个uuid

```JSON
{
    "log": {
        "loglevel": "warning"
    },
    "routing": {
        "domainStrategy": "AsIs",
        "rules": [
            {
                "ip": [
                    "geoip:private"
                ],
                "outboundTag": "blocked",
                "type": "field"
            }
        ]
    },
    "inbounds": [
        {
            "port": 1234,
            "protocol": "vmess",
            "settings": {
                "clients": [
                    {
                        "id": "d885f48d-d7ab-4e02-ba2a-7fe13a2367bb"
                    }
                ]
            }
        }
    ],
    "outbounds": [
        {
            "protocol": "freedom"
        },
        {
            "protocol": "blackhole",
            "tag": "blocked"
        }
    ]
}


```

第三步：查看状态，重启服务

systemctl status v2ray\.service

systemctl start v2ray\.service

检查json是否正确

jq \. /usr/local/etc/v2ray/config\.json



解决方案

问题：如果ping不通网络

![Image](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=ZjlmNWNiNGU4N2Q5MDNjZDVhZjZhZTJjM2VmM2I5NTNfYjk2NDA2MmM5YWY3NzY4MjVkMDg0NzhmNjVlZjc3ZmFfSUQ6NzY0MjE1NzgxOTEyODY5NTc0MF8xNzg3Mzk5NzQ1OjE3ODc0ODYxNDVfVjM)

解决：vim /etc/resolv\.conf

添加配置

```SQL
nameserver 8.8.8.8
nameserver 1.1.1.1
```

![Image](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=MmUyNjkyZjk5MGE1ZDRiNDJmN2JlNGE5NWQ3MmY5ZWNfZDdmZGQyZjYyYmEyY2QyZjNkMDNjNTI1NDI5NTI3ZjNfSUQ6NzY0MjE1ODAyMjEzMTI0MDEyMl8xNzg3Mzk5NzQ1OjE3ODc0ODYxNDVfVjM)

![Image](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=ZmNmMDQzOTFmYmI3ZjVmMjQwMzk3NTIxZTdhZTM3ZjhfZTNlZDUxM2RkMTg0MjZjYjFhNTBlZmE4MzdlMThlMjJfSUQ6NzY0MjE1ODE2MDY5MDEwNTUzMl8xNzg3Mzk5NzQ1OjE3ODc0ODYxNDVfVjM)



