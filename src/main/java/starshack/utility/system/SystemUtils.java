package starshack.utility.system;

import starshack.utility.Utils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.math.BigInteger;
import java.security.MessageDigest;

public class SystemUtils {
    /**
     * @deprecated 已停用，不再随任何请求发送（2026-09-27）。
     * <p>原因：StarShack 以「隐私优先」为承诺，不应向第三方脚本来源方发送可识别本机的值。
     * 另外注意：本方法的实现与命名不符 —— 读取 COMPUTERNAME / PROCESSOR_IDENTIFIER 等硬件信息的
     * 那一次 update 之后没有调用 digest()，返回值实际只是 MD5(20 秒时间窗口 + 固定盐)。
     * 请勿「修正」这个缺陷后重新启用，那会使客户端真正开始外发硬件指纹。
     */
    @Deprecated
    public static String getHardwareIdForLoad(String url) {
        String hashedId = "";
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(((System.currentTimeMillis() / 20000L + 29062381L) + "J{LlrPhHgj8zy:uB").getBytes("UTF-8"));
            hashedId = String.format("%032x", new BigInteger(1, instance.digest()));
            instance.update((System.getenv("COMPUTERNAME") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL") + Runtime.getRuntime().availableProcessors() + url).getBytes("UTF-8"));
            return hashedId;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return hashedId;
    }

    public static void addToClipboard(String string) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection(string);
            clipboard.setContents(stringSelection, null);
        } catch (Exception e) {
            Utils.sendMessage("&cFailed to copy &b" + string);
        }
    }
}