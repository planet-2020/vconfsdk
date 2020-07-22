package com.kedacom.vconf.sdk.utils.file;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kedacom.vconf.sdk.utils.lang.StringHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class FileHelper {
    public static final String EMPTY_STR = "";

    private static Application ctx;
    
    private FileHelper(){}

    public static void init(Application context){
        if (null != ctx){
            return;
        }
        ctx = context;
    }

    // TODO 使用java nio

    //=================== 文件路径/文件名相关处理

    /**获取文件名。
     * NOTE：不区分目录和文件，也不做路径的合法性校验，只是基于路径的字面值进行解析，获取其中表示文件名的部分。
     * @param path 文件路径。
     * @return 文件名。例如：
     *               crash.txt -> crash.txt
     *              /sdcard/kedacom/crash.txt -> crash.txt
     *              /sdcard/kedacom/ -> kedacom
     *              /sdcard/kedacom/// -> kedacom
     *              /   -> {@link #EMPTY_STR}
     * */
    public static @NonNull String getFileName(@NonNull String path) {
        path = getRidOfAppendingSeparator(path);
        int lastSepIndx = path.lastIndexOf(File.separator);
        if (-1 == lastSepIndx){
            return path;
        }else{
            return path.substring(lastSepIndx + 1);
        }
    }


    /**
     * 获取不包含扩展名的文件名
     * NOTE：不区分目录和文件，也不做路径的合法性校验，只是基于路径的字面值进行解析，获取其中不包含扩展名的文件名的部分。扩展名的分隔符为"."。
     *
     * @param path 文件路径。
     * @return 不含扩展名的文件名。例如：
     *          crash -> crash
     *          crash.txt -> crash
     *          /sdcard/kedacom/crash.txt -> crash
     *          /sdcard/kedacom/ -> kedacom
     *          /sdcard/kedacom.txt/ -> kedacom
     *          /   -> {@link #EMPTY_STR}
     * */
    public static @NonNull String getFileNameWithoutExtension(@NonNull String path) {
        String fileName = getFileName(path);
        int lastDotIndx = fileName.lastIndexOf(".");
        if (-1 == lastDotIndx){
            return fileName;
        }
        return fileName.substring(0, lastDotIndx);
    }

    /**
     * 获取文件扩展名。
     * NOTE：不区分目录和文件，也不做路径的合法性校验，只是基于路径的字面值进行解析，获取其中文件名的扩展名部分。扩展名的分隔符为"."。
     *
     * @param path 文件路径。
     * @return 文件扩展名。例如：
     *          crash.txt -> txt
     *          crash..txt -> txt
     *          /sdcard/kedacom/crash.txt -> txt
     *          /sdcard/kedacom/ -> {@link #EMPTY_STR}
     *          /sdcard/kedacom.txt/ -> txt
     * */
    public static @NonNull String getExtension(@NonNull String path) {
        path = getRidOfAppendingSeparator(path);
        int lastDotIndx = path.lastIndexOf(".");
        if (-1 == lastDotIndx){
            return EMPTY_STR;
        }
        return path.substring(lastDotIndx+1);
    }


    /**获取父目录路径。
     * NOTE：不区分目录和文件，也不做路径的合法性校验，只是基于路径的字面值进行解析，获取其中表示父目录的部分。
     * @param path 文件路径
     * @return 返回该文件所在父目录路径。例如：
     *      /sdcard/kedacom/crash.txt -> /sdcard/kedacom/
     *      /sdcard/kedacom/    -> /sdcard/
     *      /crash.txt   -> /
     *      crash.txt   -> {@link #EMPTY_STR}
     *      /   ->  {@link #EMPTY_STR}
     * */
    public static @NonNull String getParentDir(@NonNull String path){
        path = getRidOfAppendingSeparator(path);
        int lastSepIdx = path.lastIndexOf(File.separator);
        if (-1 == lastSepIdx){
            return EMPTY_STR;
        }else {
            return path.substring(0, lastSepIdx+1);
        }
    }


    /**
     * 剔除末尾的文件路径分隔符。
     * 例如：
     * /sdcard/kedacom/    -> /sdcard/kedacom
     * /sdcard/kedacom///    -> /sdcard/kedacom
     *  /    ->   {@link #EMPTY_STR}
     * */
    public static @NonNull String getRidOfAppendingSeparator(@NonNull String path){
        if(path.equals(File.separator)){
            return EMPTY_STR;
        }
        if(!path.endsWith(File.separator)){
            return path;
        }
        return getRidOfAppendingSeparator(path.substring(0, path.length()-1));
    }


    /**获取文件的MIME类型
     * @param extension 文件扩展名
     * @return 对应的MIME类型。例如：
     *          "txt"对应的MIME类型为"text/plain"
     * */
    public static @NonNull String getMIMEType(@NonNull String extension) {
        for(int i = 0; i< ExtMIMEMap.length; ++i) {
            if(extension.equals(ExtMIMEMap[i][0])) {
                return ExtMIMEMap[i][1];
            }
        }
        return "*/*";
    }


    /**
     * 根据URI获取文件路径
     * */
    public static @Nullable String getPathFromUri(@NonNull Context context, @NonNull Uri uri) {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
            try {
                String[] projection = { "_data" };
                Cursor cursor = context.getContentResolver().query(uri, projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    String path = cursor.getString(column_index);
                    cursor.close();
                    return path;
                }
            } catch (Exception e) {
            }
        }
        else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(scheme)) {
            return uri.getPath();
        }

        return null;
    }


    //=================== 文件属性相关

    /**获取文件大小
     * NOTE：若file指向文件则计算该文件大小，若指向目录则递归计算该目录下所有文件的大小总和
     * @param fullPath 文件完整路径
     * @return 文件大小；0，若文件不存在。单位：字节
     * */
    public static long getFileSize(String fullPath) {
        return getFileSize(new File(fullPath));
    }

    /**获取文件大小
     * NOTE：若file指向文件则计算该文件大小，若指向目录则递归计算该目录下所有文件的大小总和
     * @param file 文件对象
     * @return 文件大小；0，若文件不存在。单位：字节
     * */
    public static long getFileSize(File file) {
        if (!file.exists()){
            return 0;
        }
        long size = 0;

        if (file.isFile()){
            return file.length();
        }else if (file.isDirectory()){
            File[] flist = file.listFiles();
            for (File f:flist) {
                if (f.isFile()){
                    size += f.length();
                }else if (f.isDirectory()){
                    size += getFileSize(f);
                }
            }
            return size;
        }else{
            return 0;
        }
    }

    /**
     * 获取易读格式的文件大小（精确到小数点后两位）
     * 
     * @param size 文件大小
     * @param unit 输出单位。NOTE：若指定为AUTO则自动根据size大小选择输出单位：
     *                          不足1KB以B为单位；不足1MB以KB为单位，不足1GB以MB为单位，其余以GB为单位。
     * @return 易读格式的文件大小，如1010B、1.12KB、1000.12MB、1.12GB
     * */
    public static String formatFileSize(long size, Unit unit) {
        if (size <= 0) {
            return "0B";
        }

        DecimalFormat df = new DecimalFormat("#.00");
        String readableSize = "";
        if (unit == Unit.AUTO){
            if (size < Unit.KB.getValue()) {
                readableSize = size + "B";
            } else if (size < Unit.MB.getValue()) {
                readableSize = df.format((double) size / Unit.KB.getValue()) + Unit.KB;
            } else if (size < Unit.GB.getValue()) {
                readableSize = df.format((double) size / Unit.MB.getValue()) + Unit.MB;
            } else {
                readableSize = df.format((double) size / Unit.GB.getValue()) + Unit.GB;
            }
        }else{
            long unitVal = unit.getValue();
            readableSize = df.format((double) size / unitVal) + unit;
        }
        
        return readableSize;
    }

    /**
     * 文件大小单位
     * */
    public enum Unit{
        AUTO(0), // 根据文件大小自动选择
        KB(1024),
        MB(1024*1024),
        GB(1024*1024*1024);
        
        long value;

        Unit(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }
    }


    //=================== 文件创建、删除、拷贝、移动

    /**路径所指文件是否存在。
     * @param fullPath 文件完整路径
     * */
    public static boolean exists(@NonNull String fullPath){
        return new File(fullPath).exists();
    }


    /**
     * 获取文件完整路径
     * @param location 文件所在位置。内部私有空间（仅本进程可访问）or外部空间（其他进程亦可访问）
     * @param type 文件类型。普通文件 or cache文件（若存储空间紧张，系统可能会自动删除cache文件）
     * @param relativePath location和type共同定位到一个父目录，此参数即相对于该父目录的路径。可以为null，若为null则返回该父目录路径。
     * @return 文件完整路径。例如：
     *          location=INTERNAL, type=COMMON，relativePath=log/uilog.txt  ->  /data/data/package-name/files/log/uilog.txt
     * */
    public static String getPath(@NonNull Location location, @NonNull Type type, @Nullable String relativePath){
        File parentDir;
        if (location == Location.INTERNAL){
            if (type == Type.COMMON){
                parentDir = ctx.getFilesDir();
            }else{
                parentDir = ctx.getCacheDir();
            }
        }else {
            if (type == Type.COMMON) {
                parentDir = ctx.getExternalFilesDir(null);
            } else {
                parentDir = ctx.getExternalCacheDir();
            }
            if (parentDir == null){
                return null;
            }
        }
        if (null != relativePath) {
            return new File(parentDir, relativePath).getAbsolutePath();
        }else{
            return parentDir.getAbsolutePath();
        }
    }


    /**
     * 创建文件。
     * 若文件已存在则直接返回已存在的文件；
     * 若路径中包含不存在的目录则会递归创建所有不存在的目录；
     * @param fullPath 文件完整路径。可以通过{@link #getPath(Location, Type, String)}获取。
     * @return 成功返回创建的file，失败返回null
     * */
    public static File createFile(@NonNull String fullPath){
        File file = new File(fullPath);
        if (file.exists()){
            KLog.p(KLog.WARN, "file %s has existed already", fullPath);
            return file;
        }

        File parent = file.getParentFile();
        if (!parent.exists()){
            if (!parent.mkdirs()){
                KLog.p(KLog.ERROR, "FAILED to create parent dir %s for file %s!", parent.getAbsolutePath(), fullPath);
                return null;
            }
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

    /**
     * 创建目录。
     * 若已存在则直接返回已存在的目录；
     * 若路径中包含不存在的目录则会递归创建所有不存在的目录；
     * @param fullPath 目录完整路径。可以通过{@link #getPath(Location, Type, String)}获取。
     * @return 成功返回创建的file，失败返回null
     * */
    public static File createDir(@NonNull String fullPath){
        File file = new File(fullPath);
        if (file.exists()){
            KLog.p(KLog.WARN, "dir %s has existed already", fullPath);
            return file;
        }
        if (!file.mkdirs()){
            KLog.p(KLog.ERROR, "FAILED to create dir %s!", file.getAbsolutePath());
            return null;
        }
        return file;
    }


    /**
     * 删除文件。
     * <br>若待删对象为目录则会删除该目录及其下所有内容
     * @param fullPath 待删文件路径
     * @return 成功返回true，失败返回false
     * */
    public static boolean deleteFile(@NonNull String fullPath) {
        return deleteFile(new File(fullPath));
    }

    /**
     * 删除文件。
     * <br>若待删对象为目录则会删除该目录及其下所有内容
     * @param file 待删文件对象
     * @return 成功返回true，失败返回false
     * */
    public static boolean deleteFile(@NonNull File file) {
        if (!file.exists()){
            KLog.p(KLog.WARN, "%s does not exist!", file.getAbsolutePath());
            return true;
        }

        if (file.isFile()){
            return file.delete();
        }else if (file.isDirectory()){
            boolean ret = true;
            File[] childFiles = file.listFiles();
            if (null == childFiles){
                // 比如属于root的文件夹您无权访问
                KLog.p(KLog.ERROR, "FAILED to read %s, PERMISSION DENIED!", file.getAbsolutePath());
                return false;
            }
            for (File f : childFiles) {
                if (!deleteFile(f)) {
                    ret = false;
                }
            }
            return file.delete() && ret;
        }else{
            KLog.p(KLog.WARN, "UNKNOWN file type %s", file.getAbsolutePath());
            return false;
        }
    }


    /**
     * 拷贝文件到指定目录
     * 若目标路径包含不存在的目录，则这些目录会被创建；
     * @param srcPath 源文件路径
     * @param dstDirPath 目标目录路径
     * @return 成功返回拷贝生成的目标文件。失败返回null。可能的失败情形例如：
     *         源文件不存在；
     *         目标目录存在跟源文件同名的文件；
     *         创建目标路径失败；
     *         源文件/目标路径无法访问；
     * */
    public static File copyFile(@NonNull String srcPath, @NonNull String dstDirPath){
        File srcFile = new File(srcPath);
        if (!srcFile.exists()){
            KLog.p(KLog.ERROR, "src file %s is NOT EXISTS", srcPath);
            return null;
        }

        File dstDir = new File(dstDirPath);
        if (!dstDir.exists()){
            if (!dstDir.mkdirs()){
                KLog.p(KLog.ERROR, "FAILED to recursively make dir %s", dstDirPath);
                return null;
            }
        }

        File dstFile = new File(dstDir, srcFile.getName());
        if (dstFile.exists()){
            KLog.p(KLog.ERROR, "dst file %s has existed already!", dstFile.getAbsolutePath());
            return null;
        }

        if (srcFile.isFile()){
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                fis = new FileInputStream(srcFile);
                fos = new FileOutputStream(dstFile);
                int bufSiz = fis.available();
                byte[] buf = new byte[bufSiz];
                while (fis.read(buf)>0){
                    fos.write(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }else if (srcFile.isDirectory()){
            File[] childs = srcFile.listFiles();
            if (null == childs){
                // 比如属于root的文件夹您无权访问
                KLog.p(KLog.ERROR, "FAILED to read %s, PERMISSION DENIED!", srcFile.getAbsolutePath());
                return null;
            }
            for (File f : childs){
                copyFile(f.getAbsolutePath(), dstDir.getAbsolutePath()+File.separator+srcFile.getName());
            }

        }else{
            KLog.p(KLog.WARN, "UNKNOWN file type %s", srcFile.getAbsolutePath());
            return null;
        }

        return dstFile;
    }


    /**
     * 移动文件
     * @param srcPath 源文件路径，若源文件路径指向目录则移动整个目录。
     * @param dstDirPath 目标文件所处目录
     * @return 成功返回true，失败返回false。
     * */
    public static boolean moveFile(@NonNull String srcPath, @NonNull String dstDirPath){
        if (isInExternalStorage(srcPath) == isInExternalStorage(dstDirPath)){
            //源文件和目标文件同处内部存储或同处外部存储，则移动文件只需重命名文件即可无需真正的数据拷贝
            File srcFile = new File(srcPath);
            if (!srcFile.exists()){
                KLog.p(KLog.ERROR, "src file %s is NOT EXISTS", srcPath);
                return false;
            }

            File dstDir = new File(dstDirPath);
            if (!dstDir.exists()){
                if (!dstDir.mkdirs()){
                    KLog.p(KLog.ERROR, "FAILED to recursively make dir %s", dstDirPath);
                    return false;
                }
            }

            File dstFile = new File(dstDir, srcFile.getName());
            if (dstFile.exists()){
                KLog.p(KLog.ERROR, "dst file %s has existed already!", dstFile.getAbsolutePath());
                return false;
            }

            if (!srcFile.renameTo(dstFile)){
                KLog.p(KLog.ERROR, "FAILED to rename %s to %s", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
                return false;
            }

        }else {
            // 若源文件和目标文件身处不同存储空间，一个在内部空间一个在外部空间，则移动文件不能通过重命名完成，需先拷贝数据到目标文件再删除源文件
            File dstFile = copyFile(srcPath, dstDirPath);
            if (dstFile == null) {
                return false;
            }
            if (!deleteFile(srcPath)) {
                return false;
            }
        }

        return true;
    }

    //===================== 文件读写
    /**
     * 读取文件内容到字符串
     * @param path 目标文件路径
     * @return 若成功以单一字符串形式返回文件所有内容，若失败返回null
     * */
    public static String read2Str(@NonNull String path){
        List<String> lines = readByLine(path, -1);
        if (null == lines){
            return null;
        }
        StringBuilder tmp = new StringBuilder();
        for (String line : lines){
            tmp.append(line);
        }
        return tmp.toString();
    }

    /**
     * 按行读取文件，指定最大读取行数
     * @param path 目标文件路径
     * @param lineNum 最大读取行数，若小于等于0则表示读取所有行
     * @return 若成功以行列表形式返回文件第一行到指定行数之间所有行，若失败返回空list
     * */
    public static @NonNull List<String> readByLine(@NonNull String path, int lineNum){
        List<String> lines = new ArrayList<>();
        File file = createFile(path);
        if (null==file){
            return lines;
        }
        if (!file.canRead()){
            KLog.p(KLog.ERROR, "%s NOT READABLE", file.getAbsolutePath());
            return lines;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // FIXME should i close FileInputStream and InputStreamReader if only failed to create BufferedReader?
            return lines;
        }

        try {
            String line;
            if (lineNum>0) {
                int count=0;
                while (null != (line = reader.readLine())
                        && count < lineNum) {
                    lines.add(line);
                    ++count;
                }
            }else{
                while (null != (line = reader.readLine())) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            KLog.p(KLog.ERROR, "FAILED to read line from %s", file.getAbsolutePath());
            e.printStackTrace();
            return lines;
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return lines;
    }

    /**
     * 读取res/raw下面的文件
     * */
    public static String readFromResRaw(int resId){
        // TODO
        return null;
    }

    /**
     * 读取assets下面的文件
     * @param fileName 待读取的文件名
     * */
    public static String readFromAssets(String fileName){
        // TODO
        return null;
    }

//    private static Bitmap readFromAssets(String fileName){
//
//    }

    public static String readFromStream(InputStream is){
        // TODO
        return null;
    }

    /**
     * 拷贝assets文件到指定路径
     * @param assetName assets文件名
     * @param filePath 拷贝的目标路径
     * @return 若成功返回拷贝生成的目标文件，若失败返回null
     * */
    public static File asset2File(@NonNull String assetName, @NonNull String filePath){
        AssetManager am = ctx.getAssets();
        InputStream is;
        try {
            is = am.open(assetName);
            File file = stream2file(is, filePath, -1);
            is.close();
            return file;
        } catch (IOException e) {
            KLog.p(KLog.ERROR, "FAILED to open asset %s", assetName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 拷贝res/raw文件到指定路径
     * @param resId raw文件对应的资源id
     * @param filePath 拷贝的目标路径
     * @return 若成功返回拷贝生成的目标文件，若失败返回null
     * */
    public static File resRaw2File(int resId, @NonNull String filePath){
        InputStream is = ctx.getResources().openRawResource(resId);
        File file = stream2file(is, filePath, -1);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    /**
     * 将输入流写入文件
     * @param is 输入流
     * @param filepath 目标文件路径
     * @param byteCount 写入的字节数。若小于等于0则写入输入流中所有内容。
     * @return 若成功返回生成的目标文件，若失败返回null
     * */
    public static File stream2file(@NonNull InputStream is, @NonNull String filepath, long byteCount){
        File file = createFile(filepath);
        if (null==file){
            return null;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            if (!inputStream2outputStream(is, fos, byteCount)){
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return file;
    }


    /**
     * 从输入流写到输出流
     * @param is 输入流
     * @param os 输出流
     * @param byteCount 写入的字节数。若小于等于0则写入输入流中所有内容。
     * @return 若成功返回true，失败返回false。
     * */
    public static boolean inputStream2outputStream(@NonNull InputStream is, @NonNull OutputStream os, long byteCount){
        try {
            if (byteCount > 0){
                long leftBytesToWrite = byteCount;
                int bytesToRead = (int) Math.min(is.available(), leftBytesToWrite);
                byte[] buf = new byte[bytesToRead];
                int readBytes;
                while (leftBytesToWrite>0 && (readBytes=is.read(buf, 0, bytesToRead)) > 0){
                    os.write(buf, 0, readBytes);
                    leftBytesToWrite -= readBytes;
                    bytesToRead = (int) Math.min(is.available(), leftBytesToWrite);
                }
            }else{
                int bytesToRead = is.available();
                byte[] buf = new byte[bytesToRead];
                int readBytes;
                while ((readBytes=is.read(buf)) > 0){
                    os.write(buf, 0, readBytes);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }



    /**
     * 将字节数组写入文件
     * @param buf 字节数组
     * @param filepath 目标文件路径
     * @return 若成功返回生成的目标文件，若失败返回null
     * */
    public static File buf2file(@NonNull byte[] buf, @NonNull String filepath){
        File file = createFile(filepath);
        if (null==file){
            return null;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(buf);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return file;
    }

    /**
     * 将字符串写入文件
     * @param content 待写入内容
     * @param charSet 字符集
     * @param filepath 目标文件路径
     * @return 若成功返回生成的目标文件，若失败返回null
     * */
    public static File str2File(@NonNull String content, String charSet, @NonNull String filepath) {
        File file = createFile(filepath);
        if (null==file){
            return null;
        }
        if (StringHelper.isNullOrBlank(charSet)){
            charSet = "UTF-8";
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charSet));
            bw.write(content);
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }finally {
            if (bw != null){
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }


    /**
     * 按指定大小分割文件
     * @param srcFilePath 源文件路径
     * @param splitSize 分割文件的大小。
     * @param saveDirPath 分割文件的保存目录。若为null则保存在源文件同目录下的"${源文件名称}_split"目录中。
     * @return 分割文件列表。若分割失败返回空列表。
     * */
    public static @NonNull List<File> split(@NonNull String srcFilePath, long splitSize, String saveDirPath){
        List<File> fileList = new ArrayList<>();
        if (splitSize <= 0) {
            return fileList;
        }

        File srcFile = new File(srcFilePath);
        if (!srcFile.exists() || (!srcFile.isFile())) {
            KLog.p(KLog.ERROR, "%s does not exist or is not file", srcFilePath);
            return fileList;
        }

        if (null == saveDirPath){
            saveDirPath = srcFile.getParent()+File.separator+getFileNameWithoutExtension(srcFilePath)+"_split";
        }
        File saveDir = new File(saveDirPath);
        if (!saveDir.exists()){
            if(!saveDir.mkdirs()){
                KLog.p(KLog.ERROR, "create save dir %s failed", saveDirPath);
                return fileList;
            }
        }

        long leftSize = srcFile.length();
        FileInputStream in = null;
        try {
            in = new FileInputStream(srcFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return fileList;
        }

        int count = 0;
        while (leftSize > 0) {
            File file = stream2file(in, saveDir.getAbsolutePath()+File.separator+srcFile.getName()+".part"+count, splitSize);
            if (null != file){
                fileList.add(file);
            }
            leftSize -= splitSize;
            ++count;
        }

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileList;
    }


    /**
     * 合并文件
     * */
//    public static File merge(@NonNull List<File> srcFileList, String saveDirPath){
//        //TODO
//    }




    public static void bmp2file(Bitmap bmp, String filepath, Bitmap.CompressFormat format){
        if (null==bmp || null==filepath){
            return;
        }

        File parentDir = new File(getParentDir(filepath));
        if (!parentDir.exists()){
            parentDir.mkdirs();
        }

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        bmp.compress(format, 100, fout);
        try {
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //======================= 目录相关
    /**
     * 获取目录内文件名称列表
     * @param dirPath 目录路径
     * @param recursively 是否递归
     * @return 成功返回目录内文件名列表，失败返回null
     * */
    public static List<String> getFileNameList(String dirPath, boolean recursively){
        return getFileNameList(new File(dirPath), recursively);
    }// 没必要提供获取文件完整路径的版本，若用户需要完整路径可自行用目录路径和文件名拼接

    /**
     * 获取目录内文件名称列表
     * @param dir 目录文件
     * @param recursively 是否递归
     * @return 成功返回目录内文件名列表，失败返回null
     * */
    public static List<String> getFileNameList(File dir, boolean recursively){
        if (!dir.exists() || !dir.isDirectory()){
            return null;
        }

        if (!recursively) {
            return Arrays.asList(dir.list());
        }else {
            List<String> fileNameList = new ArrayList<>();
            File[] files = dir.listFiles();
            for (File f : files){
                if (f.isDirectory()){
                    fileNameList.addAll(getFileNameList(f, recursively));
                }else{
                    fileNameList.add(f.getName());
                }
            }

            return fileNameList;
        }
    }

    /**
     * 获取目录内指定扩展名的文件名称列表
     * @param dirPath 目录路径
     * @param extName 扩展名
     * @param recursively 是否递归
     * @return 成功返回目录内指定扩展名的文件名列表，失败返回null
     * */
    public static List<String> getFileNameListByExtName(String dirPath, final String extName, boolean recursively){
        return getFileNameListByExtName(new File(dirPath), extName, recursively);
    }

    /**
     * 获取目录内指定扩展名的文件名称列表
     * @param dir 目录文件
     * @param extName 扩展名
     * @param recursively 是否递归
     * @return 成功返回目录内指定扩展名的文件名列表，失败返回null
     * */
    public static List<String> getFileNameListByExtName(File dir, final String extName, boolean recursively){
        if (!dir.exists() || !dir.isDirectory()){
            return null;
        }
        final String dotExtName = extName.startsWith(".") ? extName : "."+extName;
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(dotExtName);
            }
        };

        List<String> fileNameList = new ArrayList<>();
        File[] files = dir.listFiles(filter);
        if (!recursively) {
            for (File f : files){
                if (f.isFile()) {
                    fileNameList.add(f.getName());
                }
            }
        }else {
            for (File f : files){
                if (f.isDirectory()){
                    fileNameList.addAll(getFileNameListByExtName(f, extName, recursively));
                }else{
                    fileNameList.add(f.getName());
                }
            }
        }

        return fileNameList;
    }

    /**清空目录
     * @param dirPath 目录路径
     * */
    public static void clearDir(String dirPath){
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()){
            return;
        }

        File[] childFiles = dir.listFiles();
        for (File f:childFiles){
            deleteFile(f);
        }
    }

    /**
     * 删除目录下指定扩展名的文件
     * @param dirPath 目录路径
     * @param extName 扩展名
     * */
    public static void deleteFromDirByExtName(String dirPath, final String extName){
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()){
            return;
        }
        final String dotExtName = extName.startsWith(".") ? extName : "."+extName;
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(dotExtName);
            }
        });
        for (File f:files){
            deleteFile(f);
        }
    }


    //======================= 存储相关
    /**路径是否在外部存储中*/
    public static boolean isInExternalStorage(String path){
        if (null==path){
            return false;
        }
        File f = Environment.getExternalStorageDirectory();
        if (null==f){
            return false;
        }
        return path.startsWith(f.getAbsolutePath());
    }

    /**获取外部存储路径*/
    public static String getExternalStoragePath(){
        File f = Environment.getExternalStorageDirectory();
        if (null==f){
            return null;
        }
        return f.getAbsolutePath();
    }

    /**外部存储是否可写*/
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**外部存储是否可读*/
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

//    /**
//     * 获取真实sdcard路径
//     * */
//    public static String getRealSdcardPath() throws IOException {
//        File file = new File("/proc/mounts");
//        if (file.canRead()) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//            String line;
//            while (null != (line = reader.readLine())){
//                String[] parts = line.split(StringUtils.MULTI_BLANK_CHARS);
//                if (parts.length >= 2) {
//                    if (parts[0].contains("/vold/")) {
//                        return parts[1];
//                    }
//                }
//            }
//        }
//
//        return null;
//    }


    private static final String[][] ExtMIMEMap ={
        /*{扩展名， 	 MIME类型} */
            {"",        "*/*"},
            {"txt",     "text/plain"},
            {"log",     "text/plain"},
            {"xml",     "text/plain"},
            {"conf",    "text/plain"},
            {"h",  	    "text/plain"},
            {"c",  	    "text/plain"},
            {"cpp",     "text/plain"},
            {"java",    "text/plain"},
            {"sh", 	    "text/plain"},
            {"htm",     "text/html"},
            {"html",    "text/html"},
            {"pdf",     "application/pdf"},
            {"doc",     "application/msword"},
            {"ppt",     "application/vnd.ms-powerpoint"},
            {"xls", 	"application/vnd.ms-excel"},
            {"bmp",     "image/bmp"},
            {"jpeg",    "image/jpeg"},
            {"png",     "image/png"},
            {"gif",   	"image/gif"}
    };

    public enum Location{
        INTERNAL,   // /data/data/package/
        EXTERNAL,   // /sdcard/
    }

    public enum Type{
        COMMON,     // $Location/files
        CACHE,      // $Location/cache
    }

}
