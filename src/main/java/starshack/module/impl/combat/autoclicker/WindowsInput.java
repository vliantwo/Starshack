package starshack.module.impl.combat.autoclicker;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.BaseTSD;

import java.util.Collections;

final class WindowsInput {

    private static final boolean AVAILABLE;
    private static final int INPUT_MOUSE = 0;
    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final int MOUSEEVENTF_MOVE = 0x0001;

    static {
        boolean ok = false;
        try {
            String os = System.getProperty("os.name", "");
            if (os.toLowerCase().contains("win")) {
                Native.loadLibrary("user32", User32.class);
                ok = true;
            }
        } catch (Throwable ignored) {
            ok = false;
        }
        AVAILABLE = ok;
    }

    private WindowsInput() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static void sendLeftClick() {
        if (!AVAILABLE) return;
        try {
            User32 u = Native.loadLibrary("user32", User32.class);
            u.SendInput(1, new INPUT[]{mouseInput(MOUSEEVENTF_LEFTDOWN)}, INPUT.sizeOf());
            u.SendInput(1, new INPUT[]{mouseInput(MOUSEEVENTF_LEFTUP)}, INPUT.sizeOf());
        } catch (Throwable ignored) {
        }
    }

    static void moveRelative(int dx, int dy) {
        if (!AVAILABLE) return;
        try {
            User32 u = Native.loadLibrary("user32", User32.class);
            INPUT input = new INPUT();
            input.type = INPUT_MOUSE;
            input.input = new INPUT_UNION();
            input.input.setType("mi");
            input.input.mi = new MOUSEINPUT();
            input.input.mi.dx = dx;
            input.input.mi.dy = dy;
            input.input.mi.dwFlags = MOUSEEVENTF_MOVE;
            u.SendInput(1, new INPUT[]{input}, INPUT.sizeOf());
        } catch (Throwable ignored) {
        }
    }

    private static INPUT mouseInput(int flags) {
        INPUT input = new INPUT();
        input.type = INPUT_MOUSE;
        input.input = new INPUT_UNION();
        input.input.setType("mi");
        input.input.mi = new MOUSEINPUT();
        input.input.mi.dwFlags = flags;
        return input;
    }

    interface User32 {
        int SendInput(int nInputs, INPUT[] inputs, int cbSize);
    }

    public static class INPUT extends Structure {
        public int type;
        public INPUT_UNION input;

        public static int sizeOf() {
            return new INPUT().size();
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("type", "input");
        }
    }

    public static class INPUT_UNION extends Union {
        public MOUSEINPUT mi;

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