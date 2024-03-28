package com.foxconn.plm.integrate.cis.service.Impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrate.cis.config.BUProductLine;
import com.foxconn.plm.integrate.cis.config.CISConstants;
import com.foxconn.plm.integrate.cis.domain.PartEntity;
import com.foxconn.plm.integrate.cis.domain.TCSyncBean;
import com.foxconn.plm.integrate.cis.domain.ThreeDDrawingBean;
import com.foxconn.plm.integrate.cis.mapper.cis.CISMapper;
import com.foxconn.plm.integrate.cis.service.ICISService;
import com.foxconn.plm.integrate.cis.utils.CISUtils;
import com.foxconn.plm.integrate.config.dataSource.DataSource;
import com.foxconn.plm.integrate.config.dataSource.DataSourceType;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@DataSource(value = DataSourceType.CIS)
@Service
@Primary
public class CISServiceImpl implements ICISService {
    private static Log log = LogFactory.get();
    private static final String sdfm = "yyyy-MM-dd HH:mm:ss.SSS";

    @Resource
    CISMapper cisMapper;

    @Override
    public List<PartEntity> getNotSyncPart() {
        return cisMapper.getNotSyncPart();
    }

    @Override
    public List<PartEntity> getPartById(int id) {
        return cisMapper.getPartById(id);
    }

    @Override
    public void updateSync(int id) {
        cisMapper.updateSync(id);
    }

    @Override
    public List<ThreeDDrawingBean> getThreeDDrawingRecord() {
        return cisMapper.getThreeDDrawingRecord();
    }

    @Override
    public List<ActionLogRp> recordCisSyncTC(DataManagementService dataManagementService, SavedQueryService savedQueryService) {
        List<TCSyncBean> tcSyncRecord = cisMapper.getTCSyncRecord();
        if (CollectUtil.isEmpty(tcSyncRecord)) {
            return null;
        }

        List<ActionLogRp> actionLogRpList = new ArrayList<>();
        for (TCSyncBean bean : tcSyncRecord) {
            String mfg = bean.getMfg();
            log.info("==>> mfg: " + mfg);
            String mfgPN = bean.getMfgPN();
            log.info("==>> mfgPN: " + mfgPN);
            String hhpn = bean.getHHPN();
            if (StringUtil.isEmpty(hhpn)) {
                hhpn = "*";
            }
            log.info("==>> hhpn: " + hhpn);
            List<Item> hhpnList = CISUtils.getHHPN(savedQueryService, dataManagementService, hhpn, mfg, mfgPN);
            hhpnList = hhpnList.stream().filter(CollectUtil.distinctByKey(item -> item.getUid())).collect(Collectors.toList()); // 移除相同的对象
            if (CollectUtil.isEmpty(hhpnList)) { // 代表没有查找到替代料
                continue;
            }

            Integer count = null;
            if (StringUtil.isNotEmpty(bean.getSchematicPart())) {
                count = cisMapper.getSCHEMATICPARTRecord(BUProductLine.MNTPRODUCTLINE.productLine(), bean.getSchematicPart());
                if (count > 0) {
                    setActionLogParams(dataManagementService, actionLogRpList, bean, hhpnList, CISConstants.SYMBOL_BENEFIT_NAME, BUProductLine.MNTPRODUCTLINE.bu()); // 设置参数
                }
                count = cisMapper.getSCHEMATICPARTRecord(BUProductLine.PRTPRODUCT.productLine(), bean.getSchematicPart());
                if (count > 0) {
                    setActionLogParams(dataManagementService, actionLogRpList, bean, hhpnList, CISConstants.SYMBOL_BENEFIT_NAME, BUProductLine.PRTPRODUCT.bu()); // 设置参数
                }
            }

            if (StringUtil.isNotEmpty(bean.getPcbFootprint())) {
                count = cisMapper.getPCBFOOTPRINTRecord(BUProductLine.MNTPRODUCTLINE.productLine(), bean.getPcbFootprint());
                if (count > 0) {
                    setActionLogParams(dataManagementService, actionLogRpList, bean, hhpnList, CISConstants.FOOTPRINT_PAD_BENEFIT_NAME, BUProductLine.MNTPRODUCTLINE.bu()); // 设置参数
                }
                count = cisMapper.getPCBFOOTPRINTRecord(BUProductLine.PRTPRODUCT.productLine(), bean.getPcbFootprint());
                if (count > 0) {
                    setActionLogParams(dataManagementService, actionLogRpList, bean, hhpnList, CISConstants.FOOTPRINT_PAD_BENEFIT_NAME, BUProductLine.PRTPRODUCT.bu()); // 设置参数
                }
            }
        }
        return actionLogRpList;
    }

    /**
     * 设置参数
     *
     * @param list
     * @param bean
     * @param hhpnList
     */
    private void setActionLogParams(DataManagementService dataManagementService, List<ActionLogRp> list, TCSyncBean bean, List<Item> hhpnList, String functionName, String bu) {
        hhpnList.forEach(item -> {
            ActionLogRp actionLogRp = new ActionLogRp();
            if (BUProductLine.MNTPRODUCTLINE.bu().equals(bu)) {
                actionLogRp.setBu(BUProductLine.MNTPRODUCTLINE.bu());
            } else if (BUProductLine.PRTPRODUCT.bu().equals(bu)) {
                actionLogRp.setBu(BUProductLine.PRTPRODUCT.bu());
            }

            if (CISConstants.SYMBOL_BENEFIT_NAME.equals(functionName)) {
                actionLogRp.setFunctionName(CISConstants.SYMBOL_BENEFIT_NAME);
            } else if (CISConstants.FOOTPRINT_PAD_BENEFIT_NAME.equals(functionName)) {
                actionLogRp.setFunctionName(CISConstants.FOOTPRINT_PAD_BENEFIT_NAME);
            }
            try {
                ItemRevision itemRev = TCUtils.getItemLatestRevision(dataManagementService, item);
                TCUtils.getProperties(dataManagementService, itemRev, new String[]{"item_id", "item_revision_id"});
                actionLogRp.setItemId(itemRev.get_item_id());
                actionLogRp.setRev(itemRev.get_item_revision_id());
                actionLogRp.setRevUid(itemRev.getUid());
                actionLogRp.setStartTime(new SimpleDateFormat(sdfm).format(bean.getStartTime()));
                String endTime = null;
                if (bean.getEndTime()==null) {
                    endTime = DateUtil.addTime(bean.getStartTime(), sdfm, RandomUtil.randomLong(100, 300));
                    actionLogRp.setEndTime(endTime);
                }
                list.add(actionLogRp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
