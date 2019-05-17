package com.qiniu.demo;

import com.qiniu.util.Auth;

import java.util.ArrayList;
import java.util.List;


public class UserDefineModify {

    public static void main(String[] args) {
        String accessKey = "access key";
        String secretKey = "secret key";

        //待处理文件所在空间
        String bucket = "bucket name";

        List<String> keys = new ArrayList<>();
        keys.add("fae5.mp4");
        keys.add("fae6.mp4");
        keys.add("fbe2.mp4");
        keys.add("eab1.mp4");
//        keys.add("eab2.mp4");
//        keys.add("eac1.mp4");
//        keys.add("eac2.mp4");
//        keys.add("eac3.mp4");
//        keys.add("ead1.mp4");
//        keys.add("ead2.mp4");
//
//        keys.add("eba1.mp4");
//        keys.add("eba2.mp4");
//        keys.add("eba3.mp4");
//        keys.add("ebb1.mp4");
//        keys.add("ebc1.mp4");
//        keys.add("ebc2.mp4");
//        keys.add("ebd1.mp4");
//
//        keys.add("eae2.mp4");

        for (String key : keys) {
            //待处理文件名

            Auth auth = Auth.create(accessKey, secretKey);

            BatchModify.changVcodec(auth, key, bucket);//改变视频编码格式
        }
    }
}
