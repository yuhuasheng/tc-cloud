package com.foxconn.plm.utils.date;

import com.foxconn.plm.utils.string.StringUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DateUtil {

    /**
     * 普通的时间格式
     */
    private static final String sdf = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间相加
     *
     * @param date   初始时间
     * @param format 时间样式
     * @param add    时间新增量
     * @return
     */
    public static String addTime(Date date, String format, long add) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        System.out.println("初始时间: " + sdf.format(date));
        long time = date.getTime();
        time = time + add;
        Date expireTime = new Date();
        expireTime.setTime(time);
        System.out.println("修改之后时间: " + sdf.format(expireTime));
        return sdf.format(expireTime);
    }

    public static Date getNextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        date = calendar.getTime();
        return date;
    }


    /**
     * 获取当前String类型的的时间(自定义格式)
     *
     * @param format 时间格式
     * @return String
     */
    public static String getNowTime(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }

    /**
     * String类型的时间转换成 long
     *
     * @param
     * @return String
     * @throws ParseException
     */
    public static long stringTime2LongTime(String time, String format) throws ParseException {
        if (StringUtil.isEmpty(format)) {
            format = sdf;
        }
        if (StringUtil.isEmpty(time)) {
            time = getNowTime(format);
        }
        SimpleDateFormat sd = new SimpleDateFormat(format);
        Date date = sd.parse(time);
        return date.getTime();
    }


    /**
     * 获取当前月第一天
     *
     * @param month
     * @return
     */
    public static String getFirstDayOfMonth(int month) {
        Calendar calendar = Calendar.getInstance();
        // 设置月份
        calendar.set(Calendar.MONTH, month - 1);
        // 获取某月最小天数
        int firstDay = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
        // 设置日历中月份的最小天数
        calendar.set(Calendar.DAY_OF_MONTH, firstDay);
        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String firstDayDate = sdf.format(calendar.getTime()) + " 00:00:00";
        return firstDayDate;
    }



    public static String getMonth(String dateStr) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateStr, dtf);
        return date.getMonth().getValue() + "";
    }


    public static Date getLastYear(Date date) {
        Calendar ca = Calendar.getInstance();//得到一个Calendar的实例
        ca.setTime(date);   //设置时间为当前时间
        ca.add(Calendar.YEAR, -1); //年份-1
        return ca.getTime();
    }



    /**
     * 获取当前月最后一天
     *
     * @param month
     * @return
     */
    public static String getLastDayOfMonth(int month) {
        Calendar calendar = Calendar.getInstance();
        // 设置月份
        calendar.set(Calendar.MONTH, month - 1);
        // 获取某月最大天数
        int lastDay = 0;
        //2月的平年瑞年天数
        if (month == 2) {
            // 这个api在计算2020年2月的过程中有问题
            lastDay = calendar.getLeastMaximum(Calendar.DAY_OF_MONTH);
        } else {
            lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        // 设置日历中月份的最大天数
        calendar.set(Calendar.DAY_OF_MONTH, lastDay);
        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String lastDayDate = sdf.format(calendar.getTime()) + " 23:59:59";
        return lastDayDate;
    }


    /**
     * 获取集合中最大日期和最小日期
     * @param dates
     * @param format
     * @return
     */
    public static Map<String, String> getMinAndMaxDate(List<String> dates, String format) {
        Map<String, String> map = new HashMap<>();
        if (null == dates || dates.size() <= 0) {
            return null;
        }
        if (dates.size() < 2) {
            map.put("minDate", dates.get(0));
            map.put("maxDate", dates.get(0));
            return map;
        }

        // 自定义list排序，集合数据(月份)按升序排序
        final SimpleDateFormat sdf = new SimpleDateFormat(format);
        Collections.sort(dates, new Comparator<String> () {
            public int compare(String o1, String o2) {
                int mark = 1;
                try {
                    Date date1 = sdf.parse(o1);
                    Date date2 = sdf.parse(o2);
                    if (date1.getTime() < date2.getTime()) {
                        mark = -1; // 调整顺序，-1为不需要调整顺序
                    }

                    if (o1.equals(o2)) {
                        mark = 0;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    System.out.println("日期转换异常" + e);
                }
                return mark;
            }
        });
        map.put("minDate", dates.get(0));
        map.put("maxDate", dates.get(dates.size() - 1));
        return map;
    }

    /**
     * 处理时间格式化
      * @param str
     * @param format
     * @return
     */
    public static Calendar dealDateFormat(String str, String format) {
        DateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(str);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取时间相减的天数
     * @param beginDateStr
     * @param endDateStr
     * @param format
     * @return
     * @throws ParseException
     */
    public static long getDaySub(String beginDateStr, String endDateStr, String format) throws ParseException {
        long day = 0;
        try {
            if (StringUtil.isEmpty(beginDateStr)) {
                return day;
            }

            if (StringUtil.isEmpty(endDateStr)) {
                return day;
            }

            if (beginDateStr.compareTo(endDateStr) == 0) {
                return day;
            }
            if (beginDateStr.compareTo(endDateStr) > 0) {
                return day;
            }
            DateFormat sdf = new SimpleDateFormat(format);
            Date beginDate = sdf.parse(beginDateStr);
            Date endDate = sdf.parse(endDateStr);
            day =(endDate.getTime()-beginDate.getTime())/(24*60*60*1000);
            System.out.println("相隔的天数=" + day);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return day;
    }


    /**
     * 比较两个时间的大小
     * @param beginTime
     * @param endTime
     * @param format
     * @return
     * @throws ParseException
     */
    public static long compareTime(String beginTime, String endTime, String format) throws ParseException {
        if (StringUtil.isEmpty(beginTime)) {
            return  -1;
        }

        if (StringUtil.isEmpty(endTime)) {
            return -1;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        long time1 = sdf.parse(beginTime).getTime();
        long time2 = sdf.parse(endTime).getTime();
        return time2 - time1;
    }
}
