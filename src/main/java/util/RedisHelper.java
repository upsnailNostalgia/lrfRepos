package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @ProjectName: MetadataUpdater
 * @Package: util
 * @ClassName: RedisHelper
 * @Description:
 * @Author: bruce
 * @CreateDate: 2019/12/18 9:50
 * @Version: 1.0
 */
public class RedisHelper {
    private static Logger logger = LoggerFactory.getLogger(RedisHelper.class);
    //服务器IP地址
    private static String ADDR = "10.141.221.85";

    //端口
    private static int PORT = 6379;
    //密码
    private static String AUTH = "85redis";
    //连接实例的最大连接数
    private static int MAX_ACTIVE = 200;

    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 30;


    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，
    // 则直接抛出JedisConnectionException
    private static int MAX_WAIT = 10000;

    //连接超时的时间
    private static int TIMEOUT = 10000;

    //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;

    private static JedisPool jedisPool = null;


    /**
     * 初始化Redis连接池
     */
    static {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxIdle(MAX_IDLE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT, AUTH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Jedis实例
     */
    public synchronized static Jedis getJedis() {
        try {
            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 释放资源
     */
    public static void returnResource(final Jedis jedis) {
        if (jedis != null) {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 该方法用来从redis中获取上一次更新的metadata的repository的id
     * @return
     */
    public static int getLastMetadataUpdateId(){
        Jedis jedis = RedisHelper.getJedis();
        jedis.select(2);
        String lastMetadataUpdateId = jedis.get("lastMetadataUpdateId");
        logger.info("lastMetadataUpdateId值为: {}",lastMetadataUpdateId);
        return Integer.valueOf(lastMetadataUpdateId);
    }


    /**
     * 此方法用来设置redis里面的lastMetadataUpdateId
     * @param lastMetadataUpdateId
     */
    public static void setLastMetadataUpdateId(int lastMetadataUpdateId) {
        Jedis jedis = RedisHelper.getJedis();
        jedis.select(2);
        jedis.set("lastMetadataUpdateId",String.valueOf(lastMetadataUpdateId));
        logger.info("已经将lastMetadataUpdateId为 {} 的id更新到redis",lastMetadataUpdateId);
    }
}
