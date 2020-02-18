package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static util.TimeHelper.dateToTimeStamp;

/**
 * @ProjectName: MetadataUpdater
 * @Package: util
 * @ClassName: TokenHelper
 * @Description:
 * @Author: bruce
 * @CreateDate: 2019/12/18 9:49
 * @Version: 1.0
 */
public class TokenHelper {
    private static Logger logger = LoggerFactory.getLogger(TokenHelper.class);

    /**
     * @param token
     * @param token_num
     * @return //根据token的值找到在redis数据库中存储的key为多少？
     */
    public static int findTokenKey(String token, int token_num){
        Jedis jedis  = RedisHelper.getJedis();
        jedis.select(1);
        for (int i=0;i<token_num;i++){
            String str_token_tmp = String.valueOf(jedis.hmget(String.valueOf(i),"token"));
            String str_token = str_token_tmp.substring(1,str_token_tmp.length()-1);
            if (token.equals(str_token)){
                RedisHelper.returnResource(jedis);
                return i;
            }
        }
        RedisHelper.returnResource(jedis);
        return -1;
    }

    public static void updateToken(int key_redis,String remaining,String resetTime){
        //如果remmianing小于500，则需要将flag设置为false
        Jedis jedis = RedisHelper.getJedis();
        jedis.select(1);

        String str_token_tmp = String.valueOf(jedis.hmget(String.valueOf(key_redis),"token"));
        String str_token = str_token_tmp.substring(1,str_token_tmp.length()-1);

        Map<String,String> map = new HashMap<String, String>();
        //flag控制改token是否可以被使用（防止被封）
        //此处由于经过多次测试，发现github存在token的remaining num会突然从1000骤降至50多，然后直接403,401
        if (Integer.parseInt(remaining)<2000){
            map.put("flag","false");
        }
        else{
            map.put("flag","true");
        }
        map.put("remaining_num",remaining);
        map.put("reset_timestamp",resetTime);
        map.put("token",str_token);

        jedis.hmset(String.valueOf(key_redis),map);
        RedisHelper.returnResource(jedis);
    }


    /**
     * @param token_num
     * @param id
     * @return 从redis数据库获取token的token
     * @throws InterruptedException
     * @throws ParseException
     */
    public static String getToken(int token_num, int id) throws InterruptedException, ParseException {
        if (token_num>0){
            //筛选哪个token作为此repository_id
            int index = id%token_num;
            logger.info("The token index is:"+index);
            Jedis jedis = RedisHelper.getJedis();
            jedis.select(1);
            if (jedis.exists(String.valueOf(index))){
                //获取flag的值是否为true
                String flag = String.valueOf(jedis.hmget(String.valueOf(index),"flag"));
                //获取token
                String strToken = String.valueOf(jedis.hmget(String.valueOf(index),"token"));
                //如果取得的token已经达到请求访问的上限（5000/h），此时需要sleep至resetTime
                if("[false]".equals(flag)){
                    //得到redis数据库中索引为index的token的resetTime
                    String strDateResetTmp = String.valueOf(jedis.hmget(String.valueOf(index),"reset_timestamp"));
                    String strDateReset = strDateResetTmp.substring(1,strDateResetTmp.length()-1);
                    System.out.println("str_date_reset:"+strDateReset);
                    Long dateReset = dateToTimeStamp("yyyy-MM-dd HH:mm:ss",strDateReset);
                    Date date = new Date();
                    Long dateNow = date.getTime();
                    logger.info("tokenID为： "+strToken+"  的token由于使用频繁，正在等待sleep()..............");
                    logger.info("str_date_reset:"+strDateReset+"======="+"date:"+date);
                    logger.info("date_reset:"+dateReset+"======="+"date_now:"+dateNow);
                    if (dateReset-dateNow > 0){
                        RedisHelper.returnResource(jedis);
                        Thread.sleep(dateReset-dateNow);
                        jedis = RedisHelper.getJedis();
                    }
                    else {
                        logger.info("This token is null because 'date_reset - date_now' below zero!");
                    }
                    updateToken(index,"5000","");
                    logger.info("tokenID为： "+strToken+"  的token等待结束，正在重新恢复使用.............");
                }
                RedisHelper.returnResource(jedis);
                return strToken.substring(1,strToken.length()-1);
            }
        }
        System.exit(0);
        return null;
    }

}
