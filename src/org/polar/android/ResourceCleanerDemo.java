package org.polar.android;

import org.polar.android.resoptimization.DrawableCleaner;
import org.polar.android.resoptimization.IResourceCleaner;
import org.polar.android.resoptimization.LayoutCleaner;
import org.polar.android.resoptimization.Logger;

/**
 * Created by xiangdong.wu on 2016/4/7.
 */
public class ResourceCleanerDemo {
    public static void main(String args[]) {

        String rootPath = "C:\\workspace_auto\\demo";
        ResourceCleanerDemo.run(rootPath);
    }

    public static void run(final String rootPath){
        Logger.D("hello world");
        String line = "<item android:drawable=\"@drawable/v4_list_item_bg_hl\" android:state_pressed=\"true\" />";
        String param = "@drawable/v4_list_item_bg_hl";
        System.out.println(line.contains(param));
        new Thread(new Runnable() {
            @Override
            public void run() {
                int repeat = 0;
                int deleteNumber = 0;
                int deleteDrawableFileNumber = 0;
                int deleteXmlFileNumber = 0;
                long startTime = System.currentTimeMillis();
                /**
                 * 由于第一次删除可能删除不干净，所以进行二次删除
                 */
                do {
                    Logger.D("start clean =========================================.{?}", (++repeat));
                    deleteNumber = 0;;
                    IResourceCleaner layoutCleaner = new LayoutCleaner(rootPath);
                    layoutCleaner.start(true);
                    deleteNumber += layoutCleaner.getDeleteNumber();
                    deleteXmlFileNumber += layoutCleaner.getDeleteNumber();
                    Logger.D("end....={?}", layoutCleaner.getDeleteNumber());
                    Logger.D("=====================================================.");
                    Logger.D("start check drawable......");

                    IResourceCleaner resourceCleaner = new DrawableCleaner(rootPath);
                    resourceCleaner.start(true);
                    deleteNumber += resourceCleaner.getDeleteNumber();
                    deleteDrawableFileNumber += resourceCleaner.getDeleteNumber();
                    Logger.D("end check drawable......{?}", resourceCleaner.getDeleteNumber());
                } while (deleteNumber > 0);
                long endTime = System.currentTimeMillis();
                Logger.D("clean done  deleteXmlFileNumber= {?},deleteDrawableFileNumber= {?} cost = {?} seconds",
                        deleteXmlFileNumber, deleteDrawableFileNumber, (endTime - startTime));
            }
        }).start();
    }
}
