package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.tclicensereport.domain.*;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 9:06
 * @Version 1.0
 */
@Mapper
public interface TcLicenseReportMapper {

    List<ReportVO> summary(QueryRp rp);

    List<ReportVO> history(QueryRp rp);

    List<LovEntity> getLov();

    List<UserInfoVO> getUserInfo(QueryRp rp);

    List<UserInfoVO> getUsageInfo(QueryRp rp);

    List<TCLicenseByBean> exportByPhase();

    /**
     * 統計指定時間段BU每天的使用率
     *
     * @param startDay
     * @param endDay
     * @return
     */
    @MapKey("BU")
    List<Map<String, Object>> utilizationRate(String startDay, String endDay);

    /**
     * 統計指定時間段BU每天的稼動率
     *
     * @param startDay
     * @param endDay
     * @return
     */
    @MapKey("BU")
    List<Map<String, Object>> cropRate(String startDay, String endDay);

    /**
     * 統計指定時間段BU總計的稼動率
     *
     * @param startDay
     * @param endDay
     * @return
     */
    @MapKey("BU")
    List<Map<String, Object>> totalCropRate(String startDay, String endDay);


    /**
     * 統計指定時間段BU總計的使用率
     *
     * @param startDay
     * @param endDay
     * @return
     */
    @MapKey("FUNCTION")
    List<Map<String, Object>> utilizationRateByFunction(String startDay, String endDay);

    /**
     * 統計指定時間段BU總計的稼動率
     *
     * @param startDay
     * @param endDay
     * @return
     */
    @MapKey("FUNCTION")
    List<Map<String, Object>> cropRateByFunction(String startDay, String endDay);

    /**
     * 計算範圍了最後日期的license
     *
     * @param startDay
     * @param endDay
     * @return
     */
    int countNum(String startDay, String endDay);

    /**
     * 統計歷史數據BU的使用率
     *
     * @return
     */
    @MapKey("BU")
    List<Map<String, Object>> historyUtilizationRate();


    /**
     * 統計歷史數據BU的稼動率
     *
     * @return
     */
    @MapKey("BU")
    List<Map<String, Object>> historyCropRate();

    /**
     * 統計歷史數據各部門的使用率和稼動率
     *
     * @param month
     * @return
     */
    @MapKey("FUNCTION")
    List<Map<String, Object>> historyRadarChart(@Param("month") Integer month);
}
