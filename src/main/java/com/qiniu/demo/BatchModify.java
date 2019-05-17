package com.qiniu.demo;

import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.processing.OperationStatus;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.UrlSafeBase64;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BatchModify {
    public static void main(String[] args) {
        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Zone.zone0());
        //...其他参数参考类注释
        String accessKey = "access key";
        String secretKey = "secret key";
        //待处理文件所在空间
        String bucket = "bucket name";
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 1000;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";
        //列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);

        List<String> deleteKeyList = new ArrayList<>();
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                String key = item.key;

                changVcodec(auth, key, bucket);//第一步：改变视频编码格式
//                downLoadFromUrl(key);//第三步，根据url下载视频
                if (!key.contains("_")) {
                    deleteKeyList.add(key);
                }
            }
        }

//        deleteFiles(bucketManager, deleteKeyList, bucket);//第二步，批量删除文件

    }

    /**
     * 改变视频编码格式
     *
     * @param auth
     * @param key
     * @param bucket
     */
    public static void changVcodec(Auth auth, String key, String bucket) {
        System.out.println(key);
        String name = key.substring(0, 4);

        String targetName = "%s:" + name + "_target.mp4";

        //数据处理指令，支持多个指令
        String saveMp4Entry = String.format(targetName, bucket);//avthumb_test_target.mp4是转换后的文件名
        String avthumbMp4Fop = String.format("avthumb/mp4/vcodec/libx264|saveas/%s", UrlSafeBase64.encodeToString(saveMp4Entry));//libx264为编码格式

        //数据处理队列名称，为空时代表使用公有队列
        String persistentPipeline = "";
        //数据处理完成结果通知地址
        String persistentNotifyUrl = "http://pov1yx2ze.bkt.clouddn.com/qiniu/pfop/notify";
        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Zone.zone0());
        //...其他参数参考类注释
        //构建持久化数据处理对象
        OperationManager operationManager = new OperationManager(auth, cfg);
        try {
            String persistentId = operationManager.pfop(bucket, key, avthumbMp4Fop, persistentPipeline, persistentNotifyUrl, true);
            //可以根据该 persistentId 查询任务处理进度
            System.out.println(persistentId + "  " + targetName);
            OperationStatus operationStatus = operationManager.prefop(persistentId);
            System.out.println(operationStatus + "  " + targetName);
            //解析 operationStatus 的结果
        } catch (QiniuException e) {
            System.err.println(e.response.toString());
        }
    }


    /**
     * 批量删除文件
     *
     * @param bucketManager
     * @param deleteKeyList
     * @param bucket
     */
    private static void deleteFiles(BucketManager bucketManager, List<String> deleteKeyList, String bucket) {
        try {

            String[] deleteKeys = new String[deleteKeyList.size()];
            deleteKeys = deleteKeyList.toArray(deleteKeys);
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addDeleteOp(bucket, deleteKeys);
            Response response = bucketManager.batch(batchOperations);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < deleteKeys.length; i++) {
                BatchStatus status = batchStatusList[i];
                String key = deleteKeys[i];
                System.out.print(key + "\t");
                if (status.code == 200) {
                    System.out.println("delete success");
                } else {
                    System.out.println(status.data.error);
                }
            }
        } catch (QiniuException ex) {
            System.err.println(ex.response.toString());
        }
    }

    /**
     * 根据url下载视频
     *
     * @param fileName
     * @return
     */
    private static void downLoadFromUrl(String fileName) {
        try {

            String domainOfBucket = "http://pqdazcbn4.bkt.clouddn.com/";
            String urlStr = String.format("%s/%s", domainOfBucket, fileName);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置超时间为3秒
            conn.setConnectTimeout(3 * 1000);
            // 防止屏蔽程序抓取而返回403错误
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

            // 得到输入流
            InputStream inputStream = conn.getInputStream();
            // 获取自己数组
            byte[] getData = readInputStream(inputStream);

            // 文件保存位置
            File saveDir = new File("/Users/yanjie/Movies/miniprogram3");
            if (!saveDir.exists()) {
                saveDir.mkdir();
            }
            File file = new File(saveDir + File.separator + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(getData);
            if (fos != null) {
                fos.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            System.out.println("info:" + url + " download success");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 从输入流中获取字节数组
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }


}
