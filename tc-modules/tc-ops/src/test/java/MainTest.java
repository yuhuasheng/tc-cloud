import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * TODO
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 14:59
 **/
public class MainTest {
    public static void main(String[] args) throws JSchException {
        Session session = JschUtil.getSession("10.203.163.137", 22, "infodba", "Foxc0nn666*");
        String exec = JschUtil.exec(session, "/home/infodba/app/tc-hdfs/service.sh restart;", CharsetUtil.CHARSET_UTF_8);
        System.out.println(exec);
        session.disconnect();
    }
}
