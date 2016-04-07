package org.polar.android.resoptimization;

/**
 * Created by xiangdong.wu on 2016/4/7.
 */

import java.io.File;

public class DrawableCleaner extends ResourceCleaner {
    public DrawableCleaner(String rootPath) {
        super(rootPath);
    }

    /**
     * 获取要删除的java里面的正则表达式
     *
     * @param fileName
     * @return
     */
    protected String getDeleteJavaParam(String fileName) {
        return "R.drawable." + fileName;
    }

    /**
     * 获取要删除的java里面的正则表达式
     *
     * @param fileName
     * @return
     */
    protected String getDeleteXmlParam(String fileName) {
        return "@drawable/" + fileName;
    }

    /**
     * 是否要删除的目录
     *
     * @param f
     * @return
     */
    protected boolean isDeleteDir(File f) {
        return getFileName(f).startsWith("drawable");
    }
}
