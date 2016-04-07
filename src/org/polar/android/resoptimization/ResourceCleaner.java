package org.polar.android.resoptimization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by xiangdong.wu on 2016/4/7.
 */
public abstract class ResourceCleaner implements IResourceCleaner{
    /**
     * 要开始处理的跟目录
     */
    private String rootPath;
    /**
     * 项目中的java文件
     */
    private List<File> javaFiles = new ArrayList<File>();
    /**
     * 项目中的xml文件
     */
    private List<File> xmlFiles = new ArrayList<File>();
    /**
     * java文件缓存的内容。
     * key: 文件路径
     * value: 文件的每行位置的内容
     */
    private Map<String, Set<String>> javaFileCacheDatas = new HashMap<String, Set<String>>();
    /**
     * xml文件缓存的内容。
     * key: 文件路径
     * value: 文件的每行位置的内容
     */
    private Map<String, Set<String>> xmlFileCacheDatas = new HashMap<String, Set<String>>();
    /**
     * 寻找要删除的资源的最深的目录信息。
     */
    private int maxDeep = ResDeep.FOUR.ordinal();

    /**
     * 找到无用的数据，是否执行删除操作
     */
    private boolean isDelete;

    /**
     * 已经删除的文件个数
     */
    private int deleteNumber;
    /**
     * 是否采用内存加载文件的方式.
     * 这样效率高，但是占用的内存大
     */
    private boolean isLoadFileCache = true;

    public ResourceCleaner(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * 获取要删除的java里面的正则表达式
     *
     * @param fileName
     * @return
     */
    abstract protected String getDeleteJavaParam(String fileName);

    /**
     * 获取要删除的java里面的正则表达式
     *
     * @param fileName
     * @return
     */
    abstract protected String getDeleteXmlParam(String fileName);

    /**
     * 是否要删除的目录
     *
     * @param f
     * @return
     */
    abstract protected boolean isDeleteDir(File f);

    /**
     * 获取已经删除的数据个数
     *
     * @return
     */
    public int getDeleteNumber() {
        return deleteNumber;
    }

    /**
     * 设置要过滤的res的最深的目录，没必要全部目录穷举。
     * 目前默认值得，是4
     *
     * @param maxDeep
     */
    public void setMaxDeep(ResDeep maxDeep) {
        this.maxDeep = maxDeep.ordinal();
    }

    /**
     * 开始执行过滤操作。
     * 这里只支持删除layout和drawble 2种类型的文件
     *
     * @param isDelete 是否执行删除操作
     */
    public void start(boolean isDelete) {
        this.isDelete = isDelete;
        File f = new File(rootPath);
        Logger.D("start isDelete = {?} ,rootPath = {?} ,isExist={?}", isDelete, f.getAbsoluteFile(), f.exists());
        scan(f);
        Logger.D("java file : {?}", javaFiles.size());
        Logger.D("xml file : {?}" + xmlFiles.size());
        excute(f);
        Logger.D("delete file {?} :", getDeleteNumber());
    }


    /**
     * 获取文件名称，之前的设计是如果有后缀名称，就把后缀一起去掉。
     * 现在展示发现没有必要
     *
     * @param f
     * @return
     */
    public final String getFileName(File f) {
        String fileName = f.getName();
        return fileName;
    }

    /**
     * 扫描工程中的java和 xml文件。
     * 判断资源是否被引用，主要在java，xml是否被使用。
     *
     * @param f
     */
    private void scan(File f) {
        if (f == null) {
            return;
        }
        if (!f.isDirectory()) {
            return;
        }
        File subFiles[] = f.listFiles();
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (subFile == null) {
                continue;
            }
            if (subFile.isDirectory()) {
                scan(subFile);
                continue;
            }
            if (subFile.isFile()) {
                String fileName = subFile.getName();
                String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (prefix.equals("xml")) {
                    if (isLoadFileCache) {
                        cacheXmlFile(subFile);
                    } else {
                        xmlFiles.add(subFile);
                    }
                }
                if (prefix.equals("java")) {
                    if (!fileName.equals("R.java")) {
                        if (isLoadFileCache) {
                            cacheJavaFile(subFile);
                        } else {
                            javaFiles.add(subFile);
                        }
                    }
                }
            }
        }
    }


    /**
     * 执行删除无用资源的操作
     *
     * @param parentFile
     */
    private void excute(File parentFile) {
        excute(parentFile, 0);
    }

    /**
     * 过滤当前目录下的res目录，我们要删除的时候，只删除res目录下的资源和文件信息
     *
     * @param parentFile
     * @param deep
     */
    private void excute(File parentFile, int deep) {
        if (parentFile == null) {
            return;
        }
        if (deep >= maxDeep) {
            return;
        }
        File childFiles[] = parentFile.listFiles();
        if (childFiles == null) {
            return;
        }
        deep++;
        for (File f : childFiles) {
            if (f == null || !f.isDirectory()) {
                continue;
            }
            String fileName = getFileName(f);
            Logger.D("deep = {?} fileName= {?}", deep, fileName);
            if (fileName.startsWith("res")) {
                cleanResDir(f);
            } else {
                excute(f, deep);
            }
        }
    }

    /**
     * 开始清理资源文件。
     * 先获取当前文件下要先过滤的文件夹，然后判断过滤的文件夹下的文件是否被使用。
     * 例如，获取res下全部以layout开头的文件夹，然后，判断layout下的文件是否被引用
     *
     * @param resFile
     */
    private void cleanResDir(File resFile) {
        File subFiles[] = resFile.listFiles();
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (subFile == null) {
                continue;
            }
            if (!subFile.isDirectory()) {
                continue;
            }
            if (isDeleteDir(subFile)) {
                deleteResFileUnderCurrentResDir(subFile);
            }
        }
    }

    /**
     * 删除当前资源目录下的无用的资源文件
     *
     * @param resDir
     */
    private void deleteResFileUnderCurrentResDir(File resDir) {
        File subFiles[] = resDir.listFiles();
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (subFile == null) {
                continue;
            }
            if (isResourceUsed(subFile)) {
                continue;
            }
            deleteFile(subFile);
            Logger.D("==== [deleteFile] {?} path is {?}: {?}", subFile.getName(), subFile.getAbsolutePath(), deleteNumber);
        }
    }

    /**
     * 删除文件本身
     *
     * @param f
     */
    private void deleteFile(File f) {
        deleteNumber++;
        if (isDelete) {
            f.delete();
//            removeFileFromCache(f);
        }
    }

    /**
     * 判断资源文件是否被使用。
     * 资源文件会去掉后缀，比如xml,.9.png,.png等等
     *
     * @param resFile 校验的资源文件
     * @return
     */
    private boolean isResourceUsed(File resFile) {
        String resName = getFileName(resFile);
        int index = resName.indexOf('.');
        if (index > 0) {
            resName = resName.substring(0, index);
        }
        String javaParam = getDeleteJavaParam(resName);
        String xmlParam = getDeleteXmlParam(resName);
        if (isLoadFileCache) {
            for (String f : javaFileCacheDatas.keySet()) {
                if ((isParamInCache(javaParam, javaFileCacheDatas.get(f)))) {
//                    Logger.D("java: {?} in :{?}", javaParam, f);
                    return true;
                }
            }
            for (String f : xmlFileCacheDatas.keySet()) {
                if ((isParamInCache(xmlParam, xmlFileCacheDatas.get(f)))) {
//                    Logger.D("xml: {?} in :{?}", xmlParam, f.getAbsolutePath());
                    return true;
                }
            }
            Logger.D("Cannot find xmlParam = {?} javaParam = {?} path ={?}", xmlParam, javaParam, resFile.getAbsolutePath());

        } else {
            for (File f : javaFiles) {
                if ((isParamInFile(javaParam, f))) {
//                    Logger.D("java: {?} in :{?}", javaParam, f.getAbsolutePath());
                    return true;
                }
            }
            for (File f : xmlFiles) {
                if ((isParamInFile(xmlParam, f))) {
//                    Logger.D("xml: {?} in :{?}", xmlParam, f.getAbsolutePath());
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 参数是否在文件中
     *
     * @param param
     * @param f
     * @return
     */
    private boolean isParamInFile(String param, File f) {
        BufferedReader br = null;
        try {

            if (!f.exists()) {
                return false;
            }
            String thisLine = null;
            // open input stream test.txt for reading purpose.
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            while ((thisLine = br.readLine()) != null) {
                if (thisLine.contains(param)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 将xml缓存到内存中
     *
     * @param xmlFile
     */
    private void cacheXmlFile(File xmlFile) {
        cacheFile(xmlFileCacheDatas, xmlFile);
    }

    /**
     * 将java文件缓存到内存中
     *
     * @param javaFile
     */
    private void cacheJavaFile(File javaFile) {
        cacheFile(javaFileCacheDatas, javaFile);
    }

    /**
     * 将文件内容缓存到文件中中
     *
     * @param cache
     * @param f
     */
    private void cacheFile(Map<String, Set<String>> cache, File f) {
        if (f == null || !f.isFile() || !f.canRead()) {
            return;
        }
        Set<String> values = new HashSet<String>();
        BufferedReader br = null;
        try {
            String thisLine = null;
            // open input stream test.txt for reading purpose.
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            while ((thisLine = br.readLine()) != null) {
                values.add(thisLine.trim());
            }
            String key = f.getAbsolutePath();
            cache.put(key, values);
            Logger.D("cacheFile : key = {?}  value = {?}", key, values.size());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 参数是否在内存中
     *
     * @param param
     * @param cache
     * @return
     */
    private boolean isParamInCache(String param, Set<String> cache) {
        for (String line : cache) {
            if (line.contains(param)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从缓存中删除文件
     *
     * @param f
     */
    private void removeFileFromCache(File f) {
        String path = f.getAbsolutePath();
        if (isLoadFileCache) {
            if (javaFileCacheDatas.keySet().contains(path)) {
                javaFileCacheDatas.remove(path);
                return;
            }
            if (xmlFileCacheDatas.keySet().contains(path)) {
                xmlFileCacheDatas.remove(path);
                return;
            }
        } else {
            for (File xml : xmlFiles) {
                if (xml.getAbsolutePath().equals(path)) {
                    xmlFiles.remove(xml);
                    return;
                }
            }
            for (File xml : javaFiles) {
                if (xml.getAbsolutePath().equals(path)) {
                    javaFiles.remove(xml);
                    return;
                }
            }
        }
    }
}

