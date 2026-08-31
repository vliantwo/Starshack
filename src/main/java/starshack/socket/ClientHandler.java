package starshack.socket;

import starshack.module.Module;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 单个外部 GUI 连接的处理线程。
 * 独立顶层类，避免在 SocketBridge 内部类上出现 IDE 索引/增量编译问题。
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private volatile boolean closed = false;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try {
            String line;
            while (!closed && (line = in.readLine()) != null) {
                SocketBridge.handleCommand(this, line);
            }
        } catch (IOException ignored) {
        } finally {
            close();
        }
    }

    public void send(String msg) {
        if (closed || out == null) return;
        synchronized (out) {
            out.println(msg);
        }
    }

    /**
     * 新客户端连入时推送一次全量模块状态
     */
    public void sendFullState() {
        for (Module m : SocketBridge.getModules()) {
            send("STATE " + SocketBridge.getModuleName(m) + " " + SocketBridge.isEnabled(m));
        }
    }

    public void close() {
        if (closed) return;
        closed = true;
        boolean removed = SocketBridge.CLIENTS.remove(this);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        System.out.println("[SocketBridge] GUI disconnected");
        if (removed && SocketBridge.CLIENTS.isEmpty()) {
            SocketBridge.selfDestruct();
        }
    }

    public boolean isClosed() {
        return closed;
    }
}