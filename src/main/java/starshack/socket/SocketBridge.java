package starshack.socket;

import starshack.Stars;
import starshack.module.Module;
import starshack.module.setting.impl.SliderSetting;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SocketBridge {

    /**
     * 已连接的 GUI 客户端（package-private，供 ClientHandler 自注册/注销）
     */
    static final Set<ClientHandler> CLIENTS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    /**
     * 被外部 GUI 开启过的模块名（小写），用于自毁
     */
    private static final Set<String> externallyEnabled =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    /**
     * GUI 全部断开时，自动关闭由外部开启的模块
     */
    public static boolean selfDestructOnDisconnect = true;
    private static ServerSocket serverSocket;
    private static volatile boolean running = false;
    private static int port = 25575;

    public static int getPort() {
        return port;
    }

    // ==================== 生命周期 ====================

    public static void start(int port) {
        SocketBridge.port = port;
        if (running) return;
        running = true;

        Thread acceptor = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SocketBridge.port, 50, InetAddress.getByName("127.0.0.1"));
                System.out.println("[SocketBridge] Listening on 127.0.0.1:" + SocketBridge.getPort());
                while (running) {
                    java.net.Socket client;
                    try {
                        client = serverSocket.accept();
                    } catch (IOException se) {
                        break; // stop() 关闭了 serverSocket
                    }
                    client.setTcpNoDelay(true);
                    ClientHandler handler = new ClientHandler(client);
                    CLIENTS.add(handler);
                    new Thread(handler, "SocketBridge-Client").start();
                    System.out.println("[SocketBridge] GUI connected: " + client.getInetAddress());
                    handler.sendFullState();
                }
            } catch (IOException e) {
                if (running) System.err.println("[SocketBridge] acceptor error: " + e.getMessage());
            }
        }, "SocketBridge-Acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public static void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        for (ClientHandler c : new ArrayList<>(CLIENTS)) {
            c.close();
        }
        CLIENTS.clear();
        System.out.println("[SocketBridge] stopped");
    }

    public static void broadcast(String message) {
        for (ClientHandler c : CLIENTS) {
            c.send(message);
        }
    }

    // ==================== 对外 API（预留给游戏内事件调用）====================

    /**
     * 反作弊检测到 flag 时调用，实时推送到所有 GUI
     */
    @SuppressWarnings("unused")
    public static void onAnticheatFlag(String check, String player, double value) {
        broadcast(String.format("EVENT flag %s %s %.3f", check, player, value));
    }

    /**
     * 游戏内模块状态变化时调用，同步给所有 GUI
     */
    @SuppressWarnings("unused")
    public static void notifyModuleState(Module module, boolean enabled) {
        String name = getModuleName(module);
        if (name != null) broadcast("STATE " + name + " " + enabled);
    }

    /**
     * 游戏内参数变化时调用，同步给所有 GUI
     */
    @SuppressWarnings("unused")
    public static void notifyParam(String module, String param, String value) {
        broadcast("PARAM " + module + " " + param + " " + value);
    }

    // ==================== 指令处理 ====================

    static void handleCommand(ClientHandler sender, String raw) {
        raw = raw == null ? "" : raw.trim();
        if (raw.isEmpty()) return;
        String[] parts = raw.split("\\s+");
        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "PING":
                sender.send("PONG " + System.currentTimeMillis());
                break;

            case "SET_MODULE": {
                if (parts.length < 3) {
                    sender.send("ERROR usage: SET_MODULE <name> <true|false>");
                    return;
                }
                boolean on = Boolean.parseBoolean(parts[2]);
                setModuleState(parts[1], on);
                sender.send("OK STATE " + parts[1] + " " + on);
                broadcast("STATE " + parts[1] + " " + on);
                break;
            }

            case "SET_PARAM": {
                if (parts.length < 4) {
                    sender.send("ERROR usage: SET_PARAM <module> <param> <value>");
                    return;
                }
                setModuleParam(parts[1], parts[2], parts[3]);
                sender.send("OK PARAM " + parts[1] + " " + parts[2] + " " + parts[3]);
                broadcast("PARAM " + parts[1] + " " + parts[2] + " " + parts[3]);
                break;
            }

            case "GET_STATE": {
                for (Module m : getModules()) {
                    sender.send("STATE " + getModuleName(m) + " " + isEnabled(m));
                }
                break;
            }

            case "LIST_MODULES": {
                for (Module m : getModules()) {
                    sender.send("MODULE " + getModuleName(m) + " " + isEnabled(m));
                }
                break;
            }

            default:
                sender.send("ERROR unknown command: " + cmd);
        }
    }

    // ==================== 模块操作（全部反射，编译期零风险）=====================

    /**
     * 取所有模块（package-private，供 ClientHandler 用）
     */
    static List<Module> getModules() {
        try {
            return new ArrayList<>(Stars.getModuleManager().getModules());
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    /**
     * 模块显示名：依次尝试 getName / getLabel / getDisplayName
     */
    static String getModuleName(Module m) {
        for (String method : new String[]{"getName", "getLabel", "getDisplayName"}) {
            try {
                Method mm = m.getClass().getMethod(method);
                Object v = mm.invoke(m);
                if (v != null) return v.toString();
            } catch (Throwable ignored) {
            }
        }
        return m.getClass().getSimpleName();
    }

    /**
     * 模块是否开启
     */
    static boolean isEnabled(Module m) {
        try {
            Method mm = m.getClass().getMethod("isEnabled");
            Object v = mm.invoke(m);
            return v instanceof Boolean && (Boolean) v;
        } catch (Throwable ignored) {
        }
        return false;
    }

    static void setModuleState(String name, boolean enabled) {
        Module m = findModule(name);
        if (m == null) {
            System.out.println("[SocketBridge] Module not found: " + name);
            return;
        }
        // 优先 setEnabled(boolean)
        try {
            Method mm = m.getClass().getMethod("setEnabled", boolean.class);
            mm.invoke(m, enabled);
            applyResult(name, enabled);
            return;
        } catch (Throwable ignored) {
        }
        // 回退 enable() / disable()
        try {
            Method mm = m.getClass().getMethod(enabled ? "enable" : "disable");
            mm.invoke(m);
            applyResult(name, enabled);
        } catch (Throwable t) {
            System.out.println("[SocketBridge] toggle failed: " + t);
        }
    }

    private static void applyResult(String name, boolean enabled) {
        if (enabled) {
            externallyEnabled.add(name.toLowerCase());
        } else {
            externallyEnabled.remove(name.toLowerCase());
        }
    }

    static void setModuleParam(String moduleName, String param, String value) {
        Module m = findModule(moduleName);
        if (m == null) {
            System.out.println("[SocketBridge] Module not found: " + moduleName);
            return;
        }

        // 策略1：遍历 settings 按名字匹配
        Object target = null;
        try {
            Method gm = m.getClass().getMethod("getSettings");
            Object list = gm.invoke(m);
            if (list instanceof Iterable) {
                for (Object s : (Iterable<?>) list) {
                    String sname = settingName(s);
                    if (sname == null) continue;
                    if (sname.equalsIgnoreCase(param)
                            || (param.equalsIgnoreCase("range")
                            && (sname.equalsIgnoreCase("attackRange")
                            || sname.equalsIgnoreCase("attack range")))) {
                        target = s;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 策略2：KillAura 专用 getter
        if (target == null && moduleName.equalsIgnoreCase("killaura")) {
            String getter = null;
            if (param.equalsIgnoreCase("range") || param.equalsIgnoreCase("attackrange")) {
                getter = "getAttackRangeSetting";
            } else if (param.equalsIgnoreCase("swingrange")) {
                getter = "getSwingRangeSetting";
            } else if (param.equalsIgnoreCase("aimrange")) {
                getter = "getAimRangeSetting";
            }
            if (getter != null) {
                try {
                    target = m.getClass().getMethod(getter).invoke(m);
                } catch (Throwable ignored) {
                }
            }
        }

        if (target == null) {
            System.out.println("[SocketBridge] Param not found: " + moduleName + "." + param);
            return;
        }

        // SliderSetting 走已知 API
        if (target instanceof SliderSetting) {
            try {
                ((SliderSetting) target).setValue(Double.parseDouble(value));
                System.out.println("[SocketBridge] " + moduleName + "." + param + " = " + value);
                return;
            } catch (Throwable ignored) {
            }
        }

        // 反射尝试 setValue / set / setToggled
        Class<?>[] types = {double.class, Double.class, float.class, Float.class,
                boolean.class, Boolean.class, String.class};
        for (String mn : new String[]{"setValue", "set", "setToggled"}) {
            for (Class<?> pt : types) {
                try {
                    Method mm = target.getClass().getMethod(mn, pt);
                    Object arg = value;
                    if (pt == double.class || pt == Double.class) {
                        arg = Double.parseDouble(value);
                    } else if (pt == float.class || pt == Float.class) {
                        arg = Float.parseFloat(value);
                    } else if (pt == boolean.class || pt == Boolean.class) {
                        arg = Boolean.parseBoolean(value);
                    }
                    mm.invoke(target, arg);
                    System.out.println("[SocketBridge] " + moduleName + "." + param + " = " + value);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }

        // 兜底：直接改 value 字段
        try {
            Field f = target.getClass().getDeclaredField("value");
            f.setAccessible(true);
            Object cur = f.get(target);
            if (cur instanceof Float) {
                f.set(target, Float.parseFloat(value));
            } else if (cur instanceof Integer) {
                f.set(target, Integer.parseInt(value));
            } else if (cur instanceof Boolean) {
                f.set(target, Boolean.parseBoolean(value));
            } else if (cur instanceof Number) {
                f.set(target, Double.parseDouble(value));
            } else {
                f.set(target, value);
            }
            System.out.println("[SocketBridge] " + moduleName + "." + param + " = " + value + " (field)");
        } catch (Throwable t) {
            System.out.println("[SocketBridge] set param failed: " + t);
        }
    }

    private static String settingName(Object s) {
        for (String mn : new String[]{"getName", "getLabel", "getDisplayName"}) {
            try {
                Object v = s.getClass().getMethod(mn).invoke(s);
                if (v != null) return v.toString();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Module findModule(String name) {
        for (Module m : getModules()) {
            String n = getModuleName(m);
            if (n != null && n.equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    // ==================== 自毁（package-private，供 ClientHandler 调用）=====================

    static void selfDestruct() {
        if (!selfDestructOnDisconnect) return;
        System.out.println("[SocketBridge] All GUIs disconnected -> self destruct");
        for (Module m : getModules()) {
            String n = getModuleName(m);
            if (n != null && externallyEnabled.contains(n.toLowerCase()) && isEnabled(m)) {
                setModuleState(n, false);
            }
        }
        externallyEnabled.clear();
    }
}