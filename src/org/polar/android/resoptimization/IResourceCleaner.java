package org.polar.android.resoptimization;

/**
 * Created by xiangdong.wu on 2016/4/7.
 */
public interface IResourceCleaner {
    /**
     * 开始执行过滤操作。
     * 这里只支持删除layout和drawble 2种类型的文件
     *
     * @param isDelete 是否执行删除操作
     */
    void start(boolean isDelete);

    /**
     * 获取已经删除的数据个数
     *
     * @return
     */
    int getDeleteNumber();
}
