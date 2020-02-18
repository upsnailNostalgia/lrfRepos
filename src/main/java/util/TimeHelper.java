package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @ProjectName: MetadataUpdater
 * @Package: util
 * @ClassName: TimeHelper
 * @Description:
 * @Author: bruce
 * @CreateDate: 2019/12/18 18:05
 * @Version: 1.0
 */
public class TimeHelper {

    /**
     * @param str_format
     * @param str_date
     * @return 将将指定格式（yyyy-MM-dd HH:mm:ss）的时间字符串转成时间戳
     * @throws ParseException
     */
    public static Long dateToTimeStamp(String str_format, String str_date) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(str_format);
        Date date = df.parse(str_date);
        Long date_long = date.getTime();
        return date_long;
    }


    /**
     * @param timestampString
     * @return 将时间戳转换为对应的时间Date
     */
    public static String timeStampToDate(String str_format, String timestampString) {
        Long timestamp = Long.parseLong(timestampString) * 1000;
        String date = new SimpleDateFormat(str_format, Locale.CHINA).format(new Date(timestamp));
        return date;
    }


    /**
     * @param utcTime
     * @return 将时区问题造成的时差进行parse
     * @throws ParseException
     */
    public static String timeParse(String utcTime) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date after = df.parse(utcTime);
        df.applyPattern("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getDefault());
        String time_parsed = "";
        time_parsed = df.format(after);
        return time_parsed;
    }
}
