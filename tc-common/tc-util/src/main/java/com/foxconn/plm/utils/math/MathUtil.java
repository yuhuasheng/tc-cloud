package com.foxconn.plm.utils.math;

import com.foxconn.plm.utils.string.StringUtil;
import org.apache.commons.codec.binary.Base64;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {



    /**
     * base64解密
     *
     * @param encodeStr
     * @return
     */
    @SuppressWarnings("static-access")
    public static String base64De(String encodeStr) {
        Base64 base64 = new Base64();
        byte[] decodeStr = base64.decode(encodeStr.getBytes());
//        byte[] decodeStr = Base64.decodeBase64(encodeStr.getBytes());
        return new String(decodeStr);
    }



    /**
     * 获取到后两位小数
     */
    public static String formatDecimal(String val, int size) {
        if (StringUtil.isEmpty(val))
            return "";

        BigDecimal db = new BigDecimal(val);
        db = db.setScale(size, RoundingMode.HALF_UP);
        return db.toString();
    }

    public static void main(String[] args) {
        System.out.println(MathUtil.base64De("L2JlbmVmaXRyZXBvcnQvQmVuZWZpdFRlbXBsYXRlLnhsc3g="));
    }


}
