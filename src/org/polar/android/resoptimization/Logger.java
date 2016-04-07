package org.polar.android.resoptimization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xiangdong.wu on 2016/4/7.
 */
public
class Logger {

    /**
     * 日期格式
     */
    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    /**
     * 默认日志的tag
     */
    private final static String DEFAULT_TAG = "wuxd";

    private final static String LOG_NAME = "log_" + System.currentTimeMillis() + ".log";

    /**
     * 打印debug级别的日志
     *
     * @param tag
     * @param msg    日期信息，参数采用{?}的形式， 例如 "这是个{?},继续打印其他参数"
     * @param params 参数信息，用来替换msg中的{?}
     */
    public static void d(String tag, String msg, Object... params) {
        //是否在控制台写日志
        String s = msgFromParams(msg, params);
        Date now = new Date();
        StringBuffer bf = new StringBuffer();
        bf.append(tag).append("/").append(df.format(now)).append(":").append(s).append("\n");
        System.out.print(bf.toString());
        writeLog2File(bf.toString());
    }

    /**
     * 打印debug级别的日志， 默认tag是@DEFAULT_TAG
     *
     * @param msg    日期信息，参数采用{?}的形式， 例如 "这是个{?},继续打印其他参数"
     * @param params 参数信息，用来替换msg中的{?}
     */
    public static void D(String msg, Object... params) {
        d(DEFAULT_TAG, msg, params);
    }

    /**
     * 将消息重新组合格式化处理
     *
     * @param msg
     * @param params
     * @return
     */
    private static String msgFromParams(String msg, Object... params) {
        StringBuffer bf = new StringBuffer();
        if (msg == null) {
            return null;
        }
        if (params == null) {
            return msg;
        }
        String msgArray[] = msg.split("\\{\\?\\}");
        int minLen = Math.min(msgArray.length, params.length);
        for (int i = 0; i < minLen; i++) {
            Object param = params[i];
            bf.append(msgArray[i]).append(param);
        }
        for (int i = minLen; i < msgArray.length; i++) {
            bf.append(msgArray[i]);
        }
        return bf.toString();
    }

    private static void writeLog2File(String log) {
        File f = new File(LOG_NAME);
        OutputStream os = null;
        try {
            os = new FileOutputStream(f, true);

            os.write(log.getBytes("utf8"));
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
