package com.heima.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.dto.AdminDtos;
import com.heima.entity.VpnNode;
import com.heima.mapper.VpnNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NodeTestService {

    private static final int HTTP_TEST_PORT = 10819;
    private static final int SOCKS_TEST_PORT = 10818;

    private final VpnNodeMapper nodeMapper;
    private final VpnConnectService connectService;
    private final ObjectMapper objectMapper;

    public AdminDtos.NodeTestResult test(Long nodeId) {
        VpnNode node = nodeMapper.findById(nodeId);
        if (node == null) throw new IllegalArgumentException("节点不存在");

        List<String> warnings = validateConfig(node);
        AdminDtos.TcpTestResult tcp = tcpPing(node.getHost(), node.getPort());
        AdminDtos.ProxyTestResult proxy = testViaLocalXray(node);

        return new AdminDtos.NodeTestResult(
                node.getId(),
                node.getName(),
                tcp,
                proxy,
                warnings,
                configSummary(node)
        );
    }

    public List<AdminDtos.NodeTestResult> testAll() {
        return nodeMapper.listAll().stream()
                .map(r -> test(r.id()))
                .toList();
    }

    private List<String> validateConfig(VpnNode node) {
        List<String> warnings = new ArrayList<>();
        if (node.getUserId() == null || node.getUserId().isBlank()) {
            warnings.add("用户 ID 未填写，客户端无法连接");
        }
        String network = node.getNetwork() == null ? "tcp" : node.getNetwork();
        if ("ws".equalsIgnoreCase(network) && (node.getPath() == null || node.getPath().isBlank())) {
            warnings.add("传输为 ws 时必须填写 path");
        }
        if (tlsEnabled(node) && (node.getSni() == null || node.getSni().isBlank())) {
            warnings.add("已启用 TLS 但未填 SNI，握手可能失败");
        }
        if (node.getPort() != null && node.getPort() == 1234) {
            if (!"tcp".equalsIgnoreCase(network)) {
                warnings.add("v2rayN 中端口 1234 通常是 tcp，当前为 " + network + "，请改为 tcp");
            }
            if (tlsEnabled(node)) {
                warnings.add("端口 1234 通常不使用 TLS，请改为「传输层 TLS = 无」");
            }
            if (node.getPath() != null && !node.getPath().isBlank()) {
                warnings.add("tcp 模式不需要 path，请清空 path（当前: " + node.getPath() + "）");
            }
        }
        return warnings;
    }

    private Map<String, String> configSummary(VpnNode node) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("address", node.getHost() + ":" + node.getPort());
        m.put("id", node.getUserId() == null ? "" : node.getUserId());
        m.put("alterId", String.valueOf(node.getAlterId() == null ? 0 : node.getAlterId()));
        m.put("security", node.getSecurity() == null ? "auto" : node.getSecurity());
        m.put("network", node.getNetwork() == null ? "tcp" : node.getNetwork());
        m.put("type", node.getHeaderType() == null ? "none" : node.getHeaderType());
        m.put("path", node.getPath() == null ? "" : node.getPath());
        m.put("tls", tlsEnabled(node) ? "tls" : "");
        return m;
    }

    private AdminDtos.TcpTestResult tcpPing(String host, Integer port) {
        if (host == null || host.isBlank() || port == null || port <= 0) {
            return new AdminDtos.TcpTestResult(false, -1, "地址或端口无效");
        }
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            long latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return new AdminDtos.TcpTestResult(true, latency, null);
        } catch (Exception e) {
            return new AdminDtos.TcpTestResult(false, -1, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private AdminDtos.ProxyTestResult testViaLocalXray(VpnNode node) {
        Path xray = findXrayBinary();
        if (xray == null) {
            return new AdminDtos.ProxyTestResult(false, -1, -1, "未找到本机 xray（在 vpn-vue3-desktop 执行 pnpm run setup:xray）");
        }
        Path workDir = null;
        Process proc = null;
        try {
            workDir = Files.createTempDirectory("myvpn-node-test-");
            Map<String, Object> root = connectService.buildTestOutbound(node);
            Map<String, Object> outbound = new LinkedHashMap<>((Map<String, Object>) root.get("outbound"));
            outbound.put("tag", "proxy");

            Map<String, Object> config = Map.of(
                    "log", Map.of("loglevel", "warning"),
                    "inbounds", List.of(
                            Map.of("tag", "socks-in", "port", SOCKS_TEST_PORT, "listen", "127.0.0.1",
                                    "protocol", "socks", "settings", Map.of("udp", true)),
                            Map.of("tag", "http-in", "port", HTTP_TEST_PORT, "listen", "127.0.0.1", "protocol", "http")
                    ),
                    "outbounds", List.of(
                            outbound,
                            Map.of("protocol", "freedom", "tag", "direct")
                    ),
                    "routing", Map.of(
                            "domainStrategy", "AsIs",
                            "rules", List.of(Map.of(
                                    "type", "field",
                                    "inboundTag", List.of("socks-in", "http-in"),
                                    "outboundTag", "proxy"
                            ))
                    )
            );

            Path configFile = workDir.resolve("config.json");
            Files.writeString(configFile, objectMapper.writeValueAsString(config), StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(xray.toString(), "run", "-c", configFile.toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            proc = pb.start();
            drainQuietly(proc.getInputStream());

            if (!waitPort(HTTP_TEST_PORT, 8000)) {
                return new AdminDtos.ProxyTestResult(false, -1, -1, "xray 启动失败，请检查节点参数是否与 v2rayN 一致");
            }

            HttpClient client = HttpClient.newBuilder()
                    .proxy(java.net.ProxySelector.of(new InetSocketAddress("127.0.0.1", HTTP_TEST_PORT)))
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            long t0 = System.nanoTime();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.gstatic.com/generate_204"))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
            long latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            if (res.statusCode() != 204 && res.statusCode() != 200) {
                return new AdminDtos.ProxyTestResult(false, latency, -1, "代理响应异常 HTTP " + res.statusCode());
            }

            long speed = measureDownloadSpeed(client);
            return new AdminDtos.ProxyTestResult(true, latency, speed, null);
        } catch (Exception e) {
            return new AdminDtos.ProxyTestResult(false, -1, -1, e.getMessage());
        } finally {
            if (proc != null) proc.destroyForcibly();
            if (workDir != null) deleteDir(workDir);
        }
    }

    private long measureDownloadSpeed(HttpClient client) {
        try {
            long t0 = System.nanoTime();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://speed.cloudflare.com/__down?bytes=1048576"))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            long ms = Math.max(1, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
            int bytes = res.body() == null ? 0 : res.body().length;
            return bytes * 1000L / ms;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean waitPort(int port, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 300);
                return true;
            } catch (Exception ignored) {
                Thread.sleep(200);
            }
        }
        return false;
    }

    private void drainQuietly(InputStream in) {
        Thread t = new Thread(() -> {
            try {
                byte[] buf = new byte[256];
                while (in.read(buf) >= 0) { /* drain */ }
            } catch (Exception ignored) { }
        });
        t.setDaemon(true);
        t.start();
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir).sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) { }
            });
        } catch (Exception ignored) { }
    }

    private Path findXrayBinary() {
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "xray.exe" : "xray";
        String userDir = System.getProperty("user.dir");
        List<Path> candidates = List.of(
                Path.of(userDir, "vpn-vue3-desktop/resources/xray", name),
                Path.of(userDir, "../vpn-vue3-desktop/resources/xray", name),
                Path.of("vpn-vue3-desktop/resources/xray", name)
        );
        for (Path p : candidates) {
            if (Files.isExecutable(p)) return p.toAbsolutePath();
        }
        return null;
    }

    private boolean tlsEnabled(VpnNode node) {
        if (node.getTls() != null && "tls".equalsIgnoreCase(node.getTls())) return true;
        return node.getSecurity() != null && "tls".equalsIgnoreCase(node.getSecurity());
    }
}
