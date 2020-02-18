import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.TimeHelper.timeParse;
import static util.TokenHelper.getToken;
import static util.UrlHelper.TOKEN_NUM;
import static util.UrlHelper.getJson;

/**
 * @ProjectName: lrfRepos
 * @Package: PACKAGE_NAME
 * @ClassName: pullRepos
 * @Description:
 * @Author: bruce
 * @CreateDate: 2019/12/9 10:29
 * @Version: 1.0
 */
public class RepoCloneHelper {
    private static Logger logger = LoggerFactory.getLogger(RepoCloneHelper.class);

    /*
     *从数据库中获取repo的url地址
     */
    public static HashMap<Integer,String> getURLFromMysql(String str_sql){
        HashMap<Integer,String> hashMap = new HashMap<Integer,String>();
        try {
            Connection connection = connectDatabase();
            PreparedStatement preparedStatement;
            String sql_select = str_sql;
            preparedStatement = connection.prepareStatement(sql_select);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                int id = resultSet.getInt(1);
                String git_address = resultSet.getString(2);
                hashMap.put(id,git_address);
            }
            preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hashMap;
    }

    /*连接数据库，配置connection的相关mysql信息
     */
    public static java.sql.Connection connectDatabase() throws SQLException {
        String url = "jdbc:mysql://10.141.221.85:3306/github?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        //String url = "jdbc:mysql://localhost:3306/github?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String user = "root";
        //用户名
        String password = "root";
        //密码

        Driver driver = new com.mysql.cj.jdbc.Driver();
        //新版本
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        Connection conn = driver.connect(url, props);
        return conn;
    }

    /**
     *该函数是用传进来的形参url进行克隆repo（默认测试阶段clone到本地，之后在将其clone到库里）
     *
     * */
    public static void cloneRepo(int id,String url,String localPathRoot){
        String[] split_url = url.split("//|/");
        int url_length = split_url.length;

        File localPath = new File(localPathRoot+"/"+split_url[url_length-2]+"/"+split_url[url_length-1]);

        //如果不存在目录（用户），则新建
        //如果存在该目录，则需要先将该目录内的所有文件删除，之后重现创建
        if (localPath.exists()){
//            System.out.println("删除之前存在的文件夹:"+localPath.getPath());
//            deleteDir(localPath);
            return;
        }

        localPath.mkdir();

        logger.info("Cloning from "+ url + " to " + localPath);
        try {
            Git cloneCommand = Git.cloneRepository().setURI(url).setDirectory(localPath).setTimeout(60).call();
            cloneCommand.close();
            logger.info("The clone has done successfully!!! The id is:"+id);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**根据从数据库中得到的repo的url,批量进行clone（测试环节clone到本地）
     *
     */
    public static void cloneRepoByMap(HashMap<Integer,String> hashMap, String localPathRoot){
        //由于线程池的原因，需要将localPathRoot设置为final
        final String localPathRoot_final = localPathRoot;
        //通过HashMap进行批量下载
        Set<Map.Entry<Integer, String>> entries = hashMap.entrySet();

        //线程池
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);

        for(Map.Entry<Integer, String> entry: entries){
            final Map.Entry<Integer,String> entry_final = entry;
            fixedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    int id = entry_final.getKey();
                    String url = entry_final.getValue();
                    logger.info(id+"="+url);
                    cloneRepo(id,url,localPathRoot_final);
                }
            });

        }
        fixedThreadPool.shutdown();
    }

    public static void getStars(String inputPath) throws IOException, SQLException, ParseException, InterruptedException {
        File file = new File(inputPath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String str = "";
        List<String> list = new ArrayList<String>();
        while (null!=(str=bufferedReader.readLine())) {
            list.add(str);
        }
        bufferedReader.close();

        BufferedReader bufferedReader1 = new BufferedReader(new FileReader(new File("G:\\lrfRepos\\output\\result.txt")));
        String str_tmp2 = "";
        while (null!=(str_tmp2=bufferedReader1.readLine())) {
            list.remove(str_tmp2.split(" ")[0]);
            System.out.println(str_tmp2.split(" ")[0]);
        }
        bufferedReader1.close();

        File file_tmp = new File("G:\\lrfRepos\\output\\result1.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(file_tmp);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

        HashMap<String,Integer> hashMap = new HashMap<String, Integer>();
        for (String str_addr : list) {
            String strToken = getToken(TOKEN_NUM, new Random().nextInt(20));
            String metadataJson = getJson("https://api.github.com/repos/"+str_addr, strToken);
            int starsCount = getUpdatingMetadata(metadataJson);
            if (starsCount!=-1) {
                outputResult(bufferedWriter,starsCount,str_addr);
            }
        }

        bufferedWriter.close();
        fileOutputStream.close();
    }

    private static void outputResult(BufferedWriter bufferedWriter, int starsCount, String str_addr) throws IOException {
        bufferedWriter.append(str_addr+" "+starsCount);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }


    public static int getUpdatingMetadata(String metadataJson) throws ParseException {
        //如果传来的json为空，则需要将isValid置为false，待下一步过滤此map
        if ("".equals(metadataJson)) {
            return -1;
        }
        JSONObject jsonObject = new JSONObject(metadataJson);
        int starsCount = (!jsonObject.isNull("stargazers_count"))?(Integer)jsonObject.get("stargazers_count"):0;
        return starsCount;
    }

    public static void deleteDuplicated() throws IOException {
        File file = new File("G:\\lrfRepos\\output\\result.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String str = "";
        Map<String,String> list = new HashMap<String, String>();
        while (null!=(str=bufferedReader.readLine())) {
            list.put(str.split(" ")[0],str.split(" ")[1]);
        }
        bufferedReader.close();

        File file_tmp = new File("G:\\lrfRepos\\output\\result2.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(file_tmp);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        for (Map.Entry<String,String> entry:list.entrySet()) {
            bufferedWriter.append(entry.getKey()+" "+entry.getValue());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        bufferedWriter.close();
    }

    public static void findDuplicated() throws IOException {
        File file = new File("G:\\lrfRepos\\output\\result.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String str = "";
        Set<String> set = new HashSet<String>();
        while (null!=(str=bufferedReader.readLine())) {
            set.add(str.split(" ")[0]);
        }
        bufferedReader.close();
        System.out.println(set.size());
    }


    public static void main(String[] args) throws InterruptedException, SQLException, ParseException, IOException {
//        String str_sql = "select id,git_address from repository_java where stars_count>=25";
//        HashMap<Integer,String> hashMap = getURLFromMysql(str_sql);
        //cloneRepoByMap(hashMap,"G:\\testGit4");
//        cloneRepoByMap(hashMap,"H:\\Repos");

//        getStars("G:\\lrfRepos\\input\\prjects_3000.txt");
        deleteDuplicated();
//        findDuplicated();

    }
}
