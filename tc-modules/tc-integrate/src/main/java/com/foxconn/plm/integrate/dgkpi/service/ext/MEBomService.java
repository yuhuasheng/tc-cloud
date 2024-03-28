package com.foxconn.plm.integrate.dgkpi.service.ext;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.dgkpi.domain.DesignStandardPojo;
import com.foxconn.plm.integrate.dgkpi.domain.rp.DesignStandardRp;
import com.foxconn.plm.integrate.dgkpi.mapper.DesignStandardMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.ItemUtil;
import com.foxconn.plm.utils.tc.SessionUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2013_05.StructureManagement;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service("mEBomService")
public   class MEBomService   {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    DesignStandardMapper designStandardMapper;
    private Cache<String,String> requestCache = CacheUtil.newFIFOCache(20);
    private ReentrantLock lock = new ReentrantLock(true);

    public R getMEDBOM(JSONObject paramJSONObject){
        TCSOAServiceFactory tcSOAServiceFactory=null;
        List<DesignStandardPojo>  kpiPojos=new ArrayList<>();
        String key=null;
        try {
            DesignStandardRp kPIPojoRp= JSONObject.toJavaObject(paramJSONObject, DesignStandardRp.class);
            try {
                lock.lock();
                String itemId = kPIPojoRp.getItemId();
                String projId = kPIPojoRp.getSpasProjId();
                String function = kPIPojoRp.getFunction();
                key = SecureUtil.md5(itemId + projId + function);
                if (requestCache.get(key,false) != null) {
                    log.info("key  ======>"+requestCache.get(key) );
                    return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "頻繁訪問");
                }
                requestCache.put(key,key, DateUnit.MINUTE.getMillis()*5);
            }catch(Exception ee){
                log.error(ee);
            }finally {
                lock.unlock();
            }
            tcSOAServiceFactory=new TCSOAServiceFactory(TCUserEnum.DEV);
            String modelId=kPIPojoRp.getItemId();
            log.info("Begin get kpi data ==========  modeId:"+modelId);
            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            SessionUtil.byPass(sessionService, true);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService=tcSOAServiceFactory.getSavedQueryService();
            StructureManagementService structureManagementService=tcSOAServiceFactory.getStructureManagementService();
            Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, TCSearchEnum.ITEM_NAME_OR_ID.queryName(),
                    TCSearchEnum.ITEM_NAME_OR_ID.queryParams(), new String[]{(modelId)});
            if (queryResults.get("succeeded") == null) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询图档"+modelId+"的KPI数据");
            }
            ModelObject[] mds = (ModelObject[]) queryResults.get("succeeded");
            if (mds == null || mds.length <= 0) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询图档"+modelId+"的KPI数据");
            }
            ModelObject modelIv = mds[0];
            Item modelItem = (Item) DataManagementUtil.findObjectByUid(dataManagementService, modelIv.getUid());
            ItemRevision modelRev = ItemUtil.getItemLatestRevision(dataManagementService, modelItem);

            DataManagementUtil.getProperty(dataManagementService, modelItem, TCItemConstant.REL_BOM_VIEW_TAGS);
            ModelObject[] bom_view_tags = modelItem.get_bom_view_tags();
            BOMView bomView = (BOMView) bom_view_tags[0];
            StructureManagement.CreateWindowsInfo2[] createWindowsInfo2s = new StructureManagement.CreateWindowsInfo2[1];
            createWindowsInfo2s[0] = new StructureManagement.CreateWindowsInfo2();
            createWindowsInfo2s[0].item = modelItem;
            createWindowsInfo2s[0].itemRev = modelRev;
            createWindowsInfo2s[0].bomView = bomView;
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse response = structureManagementService.createBOMWindows2(createWindowsInfo2s);
            BOMLine topBOMLine = response.output[0].bomLine;
            BOMWindow bomWindow = response.output[0].bomWindow;

            ThreadPoolExecutor pool =  new ThreadPoolExecutor(2*16+1, 2*16+1,
                    15, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000), new TaskThreadFactory("xx",false,Thread.NORM_PRIORITY),new RejectedExecutionHandler(){
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.error("======== the task is more than tha maxnium! ========");
                }});
            pool.execute(new MEBomRunnable(dataManagementService, topBOMLine, kpiPojos,
                    kPIPojoRp, designStandardMapper,pool));

            Thread.sleep(300);
            while (pool.getActiveCount() > 0 || pool.getQueue().size() > 0){}
            pool.shutdown();
            pool.shutdownNow();
            pool.purge();

            structureManagementService.saveBOMWindows(new BOMWindow[]{bomWindow});
            structureManagementService.closeBOMWindows(new BOMWindow[]{bomWindow});
            SessionUtil.byPass(sessionService, false);
            log.info("End get kpi data ==========  modeId:"+paramJSONObject.getString("itemId"));
            if(  kpiPojos==null||kpiPojos.size()<=0){
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询图档"+modelId+"的KPI数据");
            }
            return R.success(kpiPojos);
        }catch(Exception e){
            requestCache.remove(key);
            log.error(e.getLocalizedMessage(),e);
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }finally {
            try {
                if(tcSOAServiceFactory!=null) {
                    tcSOAServiceFactory.logout();
                }
            }catch (Exception e){}
        }

    }







}
