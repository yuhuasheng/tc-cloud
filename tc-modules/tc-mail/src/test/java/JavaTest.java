/**
 * @Classname JavaTest
 * @Description
 * @Date 2022/3/5 14:18
 * @Created by HuashengYu
 */
public class JavaTest {

    public static void main(String[] args) {
        String str = "cmm-it-plm@mail.foxconn.com,hua-sheng.yu@foxconn.com,";
        String[] sendToArr = str.split(",");
        for (String value : sendToArr) {
            System.out.println(value);
        }
    }
}
