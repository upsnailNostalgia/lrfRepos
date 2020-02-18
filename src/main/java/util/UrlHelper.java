package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import static util.TimeHelper.timeStampToDate;

/**
 * @ProjectName: MetadataUpdater
 * @Package: util
 * @ClassName: UrlHelper
 * @Description:
 * @Author: bruce
 * @CreateDate: 2019/12/18 16:19
 * @Version: 1.0
 */
public class UrlHelper {
    private static Logger logger = LoggerFactory.getLogger(UrlHelper.class);

    /**redis中存储的token数量*/
    public final static int TOKEN_NUM = 20;


    /**
     * @param url 例如：https://api.github.com/repos/wycats/merb-core
     * @param str_token
     * @return 该方法得到相应url返回的jsonArray，也就是repo元数据的信息
     */
    public static String getJson(String url, String str_token){
        StringBuilder result = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            //创建url
            URL realUrl = new URL(url);
            //打开连接
            URLConnection connection = realUrl.openConnection();
            //设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setRequestProperty("Authorization","token " + str_token);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            //建立连接list为空
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            //遍历所有的响应头字段，获取到cookies等
            List<String> list_status = map.get("Status");
            List<String> list_RateLimit_Remaining = map.get("X-RateLimit-Remaining");
            List<String> list_RateLimit_Reset = map.get("X-RateLimit-Reset");

            if (list_status==null){ logger.info("list为空");return "";}
            logger.info("url为："+url);
            logger.info("Status为："+list_status.toString());
            logger.info("X-RateLimit-Remaining为："+list_RateLimit_Remaining.toString());
            logger.info("所使用的token为："+str_token);
            int reset_timestamp = Integer.parseInt(list_RateLimit_Reset.toString().substring(1,list_RateLimit_Reset.toString().length()-1));
            String date_str = timeStampToDate("yyyy-MM-dd HH:mm:ss", String.valueOf(reset_timestamp));
            logger.info("X-RateLimit-Reset为："+date_str);
            logger.info("X-RateLimit-Reset为：" + reset_timestamp);

            /*===================此处代码将更新redis中的token情况（remaining，resetTime，flag）=====================*/

            //获取token在redis的key
            int key_redis = TokenHelper.findTokenKey(str_token,TOKEN_NUM);
            logger.info("key_redis为："+key_redis);

            //得到remaining和resetTime（首先得到的是带有list特色的[],需要将[]去掉）
            String remaining_tmp = list_RateLimit_Remaining.toString();

            String remaining = remaining_tmp.substring(1,remaining_tmp.length()-1);


            //更新token在redis数据库中的信息(date_str就是上面已经转换成yy-MM-dd HH:mm:ss格式的日期时间)
            TokenHelper.updateToken(key_redis,remaining,date_str);

            /*====================================================================================================*/
            if (map.get("Status").toString().startsWith("[4")){
                logger.info("该url出现{}错误!",map.get("Status"));
                return result.toString();
            }
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));

            String line;//循环读取
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            if(bufferedReader!=null){
                try {
                    //关闭流
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }
}
