package starshack.module.impl.combat.autoclicker;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.BaseTSD;

import java.util.Collections;

/**
 * Windows 原生鼠标输入（模仿 OpenVape 的 JNI/DLL 桥接层，但用 JNA 实现，无需编译原生代码）。
 * <p>
 * 作用：绕过 Minecraft 的 KeyBinding / ReflectionUtils，直接向 OS 发送真实的鼠标硬件事件，
 * 让点击变成"操作系统级输入"——这是 OpenVape "ghost client" 反检测的核心思路。
 * <p>
 * ⚠️ 使用前提：
 * 1. 仅 Windows 有效（Windows-only）
 * 2. Minecraft 窗口必须是前台窗口（有焦点），否则事件会发到别的窗口
 * 3. 需要 JNA 依赖（build.gradle 加 jna + jna-platform）
 * <p>
 * 若 JNA 不可用或当前非 Windows，所有方法为 no-op，可安全降级到兼容模式。
 * <p>
 * 兼容说明：Minecraft 1.8.9 自带 JNA 4.x，classpath 上解析到的是 4.x API，
 * 因此这里使用 Native.loadLibrary(...) + Library，而非 JNA 5.x 的 Native.load(...) / StdCallLibrary。
 */
final class WindowsInput {

    /**
     * 缓存的 User32 实例。
     * ⚠️ JNA 4.x 的 Native.loadLibrary 返回 Object，需要强制转型；
     * 为兼容 4.x/5.x 两套 API，这里统一写 (User32) 强转，并在 static 块里一次性加载。
     */
    private static final User32 INSTANCE;

    private static final boolean AVAILABLE;
    private static final int INPUT_MOUSE = 0;
    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final int MOUSEEVENTF_MOVE = 0x0001;

    static {
        User32 instance = null;
        boolean ok = false;
        try {
            String os = System.getProperty("os.name", "");
            if (os.toLowerCase().contains("win")) {
                // 关键：loadLibrary 返回 Object，必须强转（兼容 JNA 4.x）
                instance = (User32) Native.loadLibrary("user32", User32.class);
                ok = (instance != null);
            }
        } catch (Throwable ignored) {
            instance = null;
            ok = false;
        }
        INSTANCE = instance;
        AVAILABLE = ok;
    }

    private WindowsInput() {}

    /**
     * 是否可用（Windows + JNA 加载成功）
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    /** 发送一次完整左键点击（down + up） */
    static void sendLeftClick() {
        if (!AVAILABLE) return;
        try {
            // 复用 static 块里已加载的实例，避免每次点击都 loadLibrary
            INSTANCE.SendInput(1, new INPUT[]{mouseInput(MOUSEEVENTF_LEFTDOWN)}, INPUT.sizeOf());
            INSTANCE.SendInput(1, new INPUT[]{mouseInput(MOUSEEVENTF_LEFTUP)}, INPUT.sizeOf());
        } catch (Throwable ignored) {
            // 降级：忽略，调用方会 fallback 到反射模式
        }
    }

    /** 相对移动鼠标（像素），配合 Jitter 模拟人手微颤 */
    static void moveRelative(int dx, int dy) {
        if (!AVAILABLE) return;
        try {
            // 复用 static 块里已加载的实例，避免每次移动都 loadLibrary
            INPUT input = new INPUT();
            input.type = INPUT_MOUSE;
            input.input = new INPUT_UNION();
            input.input.setType("mi");
            input.input.mi = new MOUSEINPUT();
            input.input.mi.dx = dx;
            input.input.mi.dy = dy;
            input.input.mi.dwFlags = MOUSEEVENTF_MOVE;
            INSTANCE.SendInput(1, new INPUT[]{input}, INPUT.sizeOf());
        } catch (Throwable ignored) {
        }
    }

    // ================= 内部构造 =================
    private static INPUT mouseInput(int flags) {
        INPUT input = new INPUT();
        input.type = INPUT_MOUSE;
        input.input = new INPUT_UNION();
        input.input.setType("mi");
        input.input.mi = new MOUSEINPUT();
        input.input.mi.dwFlags = flags;
        return input;
    }

    // ================= JNA 映射 =================
    interface User32 extends Library {
        int SendInput(int nInputs, INPUT[] inputs, int cbSize);
    }

    public static class INPUT extends Structure {
        public int type;
        public INPUT_UNION input;
        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("type", "input");
        }

        public static int sizeOf() {
            return new INPUT().size(); }
    }

    public static class INPUT_UNION extends Union {
        public MOUSEINPUT mi;
        // 以后加键盘可以加：public KEYBDINPUT ki;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return Collections.singletonList("mi");
        }
    }

    public static class MOUSEINPUT extends Structure {
        public int dx, dy;
        public int mouseData;
        public int dwFlags;
        public int time;
        public BaseTSD.ULONG_PTR dwExtraInfo;
        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("dx", "dy", "mouseData", "dwFlags", "time", "dwExtraInfo");
        }
    }
}
