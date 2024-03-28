package com.foxconn.plm.feign.fallback;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.param.ActionLogRp;
import com.foxconn.plm.entity.param.MakerPNRp;
import com.foxconn.plm.entity.param.PartPNRp;
import com.foxconn.plm.entity.pojo.ReportPojo;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.SPASProject;
import com.foxconn.plm.entity.response.SPASUser;
import com.foxconn.plm.feign.service.TcIntegrateClient;

import java.util.List;


/**
 * @ClassName: TcIntegrateClientFallback
 * @Description:
 * @Author DY
 * @Create 2023/2/1
 */
public class TcIntegrateClientFallback implements TcIntegrateClient {
    @Override
    public String postMakerPN(List<MakerPNRp> makerPNRps)  throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的postMakerPN接口失敗");
    }

    @Override
    public List<PartPNRp> isExistInSAP(List<PartPNRp> parts) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的isExistInSAP接口失敗");
    }

    @Override
    public List<SPASProject> getClosedProjectsByDate(String startDate, String endDate, String buName)  throws BizException{
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的getClosedProjectsByDate接口失敗");
    }

    @Override
    public SPASProject getProjectPhase(String projectId)  throws BizException{
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的getProjectPhase接口失敗");
    }

    @Override
    public String getTCProjectByBu(String projectList, String BUName)  throws BizException{
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的getTCProjectByBu接口失敗");
    }

    @Override
    public List<SPASUser> getTeamRosterByEmpId(String data)  throws BizException{
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的getTeamRosterByEmpId接口失敗");
    }

    @Override
    public String getSTIProjectInfo(String projId)  throws BizException{
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的getSTIProjectInfo接口失敗");
    }

    @Override
    public String getProjectBu(String projId)  throws BizException{
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的 getProjectBu 接口失敗");
    }

    @Override
    public R<Long> addlog(List<ActionLogRp> actionLogRps)  throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的 addlog 接口失敗");
    }

    @Override
    public R<Long> getCISActionLogRecord(ActionLogRp pctionLogRp) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的 getCisRecord 接口失敗");
    }

    @Override
    public R<Boolean> insertCISPart(ActionLogRp pctionLogRp) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的 insertCISPart 接口失敗");
    }

    @Override
    public R<Long> freshReport() throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的 freshReport 接口失敗");
    }

    @Override
    public List<ReportPojo> queryProjectById(String projId) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcIntegrate服務的 queryProjectById 接口失敗");
    }
}
