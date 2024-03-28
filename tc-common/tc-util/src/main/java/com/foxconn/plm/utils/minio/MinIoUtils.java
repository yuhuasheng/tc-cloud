package com.foxconn.plm.utils.minio;

import cn.hutool.log.LogFactory;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName: MinIoUtil
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
public class MinIoUtils {

    /**
     * 查看存储bucket是否存在
     *
     * @return boolean
     */
    public static Boolean bucketExists(MinioClient minioClient, String bucketName) {
        Boolean found;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            LogFactory.get().error("minio檢查bucket是否存在出錯，錯誤信息:", e);
            return false;
        }
        return found;
    }

    /**
     * 创建存储bucket
     *
     * @return Boolean
     */
    public static Boolean makeBucket(MinioClient minioClient, String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            LogFactory.get().error("minio創建bucket出錯，錯誤信息:", e);
            return false;
        }
        return true;
    }

    /**
     * 删除存储bucket
     *
     * @return Boolean
     */
    public static Boolean removeBucket(MinioClient minioClient, String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            LogFactory.get().error("minio刪除bucket出錯，錯誤信息:", e);
            return false;
        }
        return true;
    }

    /**
     * 获取全部bucket
     */
    public static List<Bucket> getAllBuckets(MinioClient minioClient) {
        try {
            List<Bucket> buckets = minioClient.listBuckets();
            return buckets;
        } catch (Exception e) {
            LogFactory.get().error("minio獲取所有的bucket出錯，錯誤信息:", e);
        }
        return null;
    }

    /**
     * 根據名稱獲取backet
     *
     * @param bucketName
     * @return
     */
    public static Optional<Bucket> getBucket(MinioClient minioClient, String bucketName) {
        try {
            return minioClient.listBuckets().parallelStream().filter(item -> item.name().equals(bucketName)).findFirst();
        } catch (Exception e) {
            LogFactory.get().error("minio查找指定的bucket出錯，錯誤信息:", e);
        }
        return null;
    }

    /**
     * 根據文件名稱獲取指定文件流
     *
     * @param bucketName
     * @param objectName
     * @return
     */
    public static InputStream getObject(MinioClient minioClient, String bucketName, String objectName) {
        if (!bucketExists(minioClient, bucketName)) {
            return null;
        }
        try {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucketName).object(objectName).build();
            return minioClient.getObject(getObjectArgs);
        } catch (Exception e) {
            LogFactory.get().error("minio獲取指定文件流出錯，錯誤信息:", e);
        }
        return null;
    }

    /**
     * 在指定桶中根據名稱創建文件夾
     *
     * @param bucketName
     * @param dirName
     * @return
     */
    public static boolean createDir(MinioClient minioClient, String bucketName, String dirName) {
        if (!bucketExists(minioClient, bucketName)) {
            return false;
        }
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(dirName).stream(
                    new ByteArrayInputStream(new byte[]{}), 0, -1
            ).build());
            return true;
        } catch (Exception e) {
            LogFactory.get().error("minio創建文件夾出錯，錯誤信息:", e);
        }
        return false;
    }

    /**
     * 刪除指定桶下的指定文件
     *
     * @param bucketName
     * @param objectName
     * @return
     */
    public static boolean removeObject(MinioClient minioClient, String bucketName, String objectName) {
        if (!bucketExists(minioClient, bucketName)) {
            return false;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (Exception e) {
            LogFactory.get().error("minio刪除指定文件出錯，錯誤信息:", e);
        }
        return false;
    }

    /**
     * 刪除指定桶下的多個文件，返回刪除錯誤的文件名稱，如果全部刪除則返回空數組
     *
     * @param bucketName
     * @param objectNames
     * @return
     */
    public static List<String> removeObjects(MinioClient minioClient, String bucketName, List<String> objectNames) {
        if (!bucketExists(minioClient, bucketName)) {
            return Collections.emptyList();
        }
        List<DeleteObject> deleteObjects = new ArrayList<>(objectNames.size());
        for (String objectName : objectNames) {
            deleteObjects.add(new DeleteObject(objectName));
        }
        try {
            List<String> res = new ArrayList<>();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteObjects)
                    .build()
            );
            for (Result<DeleteError> result : results) {
                DeleteError deleteError = result.get();
                res.add(deleteError.objectName());
            }
            return res;
        } catch (Exception e) {
            LogFactory.get().error("minio刪除文件出錯，錯誤信息:", e);
        }
        return Collections.emptyList();
    }

    /**
     * 獲取訪問文件的外鏈
     *
     * @param bucketName
     * @param objectName
     * @param expiry     過期時間，最大為7天，單位為分鐘
     * @return
     */
    public static String getObjectUrl(MinioClient minioClient, String bucketName, String objectName, Integer expiry) {
        if (!bucketExists(minioClient, bucketName)) {
            return null;
        }
        expiry = expiryHandle(expiry);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiry)
                            .build()
            );
        } catch (Exception e) {
            LogFactory.get().error("minio獲取文件鏈接出錯，錯誤信息:", e);
        }
        return null;
    }

    /**
     * 下載文件
     *
     * @param bucketName
     * @param fileName
     * @param res
     */
    public static void download(MinioClient minioClient, String bucketName, String fileName, HttpServletResponse res) {
        if (!bucketExists(minioClient, bucketName)) {
            return;
        }
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucketName)
                .object(fileName).build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                res.setCharacterEncoding("utf-8");
                // 设置强制下载不打开
                // res.setContentType("application/force-download");
                res.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
                try (ServletOutputStream stream = res.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            LogFactory.get().error("minio下載文件出錯，錯誤信息:", e);
        }
    }

    /**
     * 通過文件流上傳文件
     *
     * @param bucketName 桶
     * @param objectName 文件名稱
     * @param in         文件流
     * @return
     */
    public static String putObject(MinioClient minioClient, String bucketName, String objectName, InputStream in) {
        if (!bucketExists(minioClient, bucketName)) {
            return null;
        }
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName)
                    .object(objectName).stream(in, in.available(), -1).build()
            );
            return getObjectUrl(minioClient, bucketName, objectName, 7 * 24 * 60);
        } catch (Exception e) {
            LogFactory.get().error("minio通過流上傳文件出錯，錯誤信息:", e);
        }
        return null;
    }

    /**
     * 通過表單文件上傳文件
     *
     * @param bucketName
     * @param file
     * @return
     */
    public static String uploadFileSingle(MinioClient minioClient, String bucketName, MultipartFile file) {
        if (!bucketExists(minioClient, bucketName)) {
            return null;
        }
        String fileName = file.getOriginalFilename();
        String[] split = fileName.split("\\.");
        if (split.length > 1) {
            fileName = split[0] + "_" + System.currentTimeMillis() + "." + split[1];
        } else {
            fileName = fileName + "_" + System.currentTimeMillis();
        }
        InputStream in = null;
        try {
            in = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName)
                    .object(fileName).stream(in, in.available(), -1).build());
        } catch (Exception e) {
            LogFactory.get().error("minio通過表單違建上傳出錯，錯誤信息:", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    LogFactory.get().error("關閉文件流出錯，錯誤信息:", e);
                }
            }
        }
        return getObjectUrl(minioClient, bucketName, fileName, 7 * 24 * 60);
    }


    /**
     * 查看文件对象
     *
     * @return 存储bucket内文件对象信息
     */
    public List<Item> listObjects(MinioClient minioClient, String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).build());
        List<Item> items = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                items.add(result.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return items;
    }


    private static int expiryHandle(Integer expiry) {
        expiry = expiry * 60;
        return expiry > 604800 ? 604800 : expiry;
    }


}
