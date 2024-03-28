package com.foxconn.plm.utils.string;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {





    public static boolean isEmpty(String s) {
        if (s == null) {
            return true;
        }
        if (s.trim().length() == 0) {
            return true;
        }
        return false;
    }

    public static boolean isNotEmpty(String s){
        return !isEmpty(s);
    }

    public static String replaceNull(String s){
          if(s==null){
              return "";
          }else{
              return s;
          }
    }




    /**
     * 字符串是否包含中文
     *
     * @param str 待校验字符串
     * @return true 包含中文字符  false 不包含中文字符
     */
    public static boolean isContainChinese(String str) {
        Pattern p = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }



    /**
     * 去除字符串中的制表符\t,回车\n,换行\r
     *
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (dest != null) {
            Pattern p = Pattern.compile("\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll(" ");
        }
        return dest.trim(); // 去除字符串前后空格
    }



    public static int findStrNum(String str, String s) {
        int fromIndex = 0;
        int count = 0;
        while (true) {
            int index = str.indexOf(s, fromIndex);
            if (-1 != index) {
                fromIndex = index + 1;
                count++;
            } else {
                break;
            }
        }
        return count;
    }


    /**
     * 获取异常信息
     *
     * @param e
     * @return String
     */
    public static String getExceptionMsg(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}
