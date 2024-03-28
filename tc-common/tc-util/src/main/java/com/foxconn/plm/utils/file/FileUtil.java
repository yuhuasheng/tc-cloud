package com.foxconn.plm.utils.file;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.privately.Access;
import org.springframework.core.io.ClassPathResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class FileUtil {
    private static Log log = LogFactory.get();

    public  static void checkFileName (String fileName)throws Exception {
        if (fileName==null||"".equalsIgnoreCase(fileName.trim())) {
            throw new Exception("文件名存在問題");
        }
    }


    public static  void checkSecurePath(String filePath) throws Exception  {
        if(filePath==null||"".equalsIgnoreCase(filePath.trim())){
            throw new Exception("文件路径存在问题： ");
        }
        if (filePath.indexOf("../") > 0 || filePath.indexOf("..") > 0 || filePath.indexOf("..\\") > 0) {
            throw new Exception("文件路径存在问题");
        }
    }

    public static File releaseFile(String fileName) throws Exception {
        ClassPathResource classPathResource = new ClassPathResource(fileName);

        InputStream inputStream = classPathResource.getInputStream();
        try {
            String outDir = System.getProperty("user.dir") + "/release/";
            File dirFile = new File(Access.check(outDir));
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File destination = new File(outDir + fileName);
            FileUtils.copyInputStreamToFile(inputStream, destination);
            return destination;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }

    /**
     * 通过输入流下载文件
     * @param in
     * @param dir
     * @param fileName
     * @return
     */
    public static File downloadFileByStream(InputStream in, String dir, String fileName) {
        File destination = null;
        try {
            if (dir.endsWith(File.separator)) {
                destination = new File(dir + fileName);
            } else {
                destination = new File(dir + File.separator + fileName);
            }
            FileUtils.copyInputStreamToFile(in, destination);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
        return destination;
    }


    /**
     * 删除某个文件夹下的所有文件
     *
     * @param delpath String
     * @return boolean
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean deletefile(String delpath) throws Exception {
        try {
            File file = new File(delpath);
            // 当且仅当此抽象路径名表示的文件存在且 是一个目录时，返回 true
            if (!file.isDirectory()) {
                file.delete();
            } else if (file.isDirectory()) {
                String[] filelist = file.list();
                for (int i = 0; i < filelist.length; i++) {
                    File delfile = new File(delpath + File.separator + filelist[i]);
                    if (!delfile.isDirectory()) {
                        delfile.delete();
                        log.info("【INFO】 " + delfile.getAbsolutePath() + "删除文件成功");
                    } else if (delfile.isDirectory()) {
                        deletefile(delpath + File.separator + filelist[i]);
                    }
                }
//				log.info("【INFO】 " + file.getAbsolutePath() + "删除成功");
//				file.delete();
            }

        } catch (FileNotFoundException e) {
            log.info("【ERROR】 " + "deletefile() Exception:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static String getFilePath(String foldName) {
        String tempPath = FileUtil.validatePath(System.getProperty("java.io.tmpdir") + File.separator);
        log.info("【INFO】 tempPath: " + tempPath);
        File file = new File(tempPath + foldName);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }


    public static String validatePath(String aString) {
        if (aString == null) return null;
        String cleanString = "";
        for (int i = 0; i < aString.length(); ++i) {
            cleanString += cleanChar(aString.charAt(i));
        }
        return cleanString;
    }

    private static char cleanChar(char aChar) {

        // 0 - 9
        for (int i = 48; i < 58; ++i) {
            if (aChar == i) return (char) i;
        }

        // 'A' - 'Z'
        for (int i = 65; i < 91; ++i) {
            if (aChar == i) return (char) i;
        }

        // 'a' - 'z'
        for (int i = 97; i < 123; ++i) {
            if (aChar == i) return (char) i;
        }

        // other valid characters
        switch (aChar) {
            case '/':
                return '/';
            case '.':
                return '.';
            case '-':
                return '-';
            case '_':
                return '_';
            case ' ':
                return ' ';
            case ':':
                return ':';
            case '\\':
                return '\\';
        }
        return '%';
    }

}
