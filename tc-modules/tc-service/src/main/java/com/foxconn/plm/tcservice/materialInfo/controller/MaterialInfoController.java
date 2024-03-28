package com.foxconn.plm.tcservice.materialInfo.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.materialInfo.domain.rp.MaterialGroupAndBaseUnitRp;
import com.foxconn.plm.tcservice.materialInfo.domain.rp.MaterialInfoPojo;
import com.foxconn.plm.utils.property.BaseUnitPropertitesUtil;
import com.foxconn.plm.utils.property.MaterialGroupPropertitesUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "物料信息查询")
@RestController
@Scope("request")
@RequestMapping("/materialInfo")
public class MaterialInfoController {

    private static Log log = LogFactory.get();

    @ApiOperation("查询物料materialGroup和baseUnit")
    @PostMapping("/getMaterialGroupAndBaseUnit")
    public R<List<MaterialInfoPojo>> getMaterialGroupAndBaseUnit(@RequestBody List<MaterialGroupAndBaseUnitRp> materialGroupAndBaseUnitRps) {

        List<MaterialInfoPojo> materialInfoPojos = new ArrayList<>();
        for (MaterialGroupAndBaseUnitRp rp : materialGroupAndBaseUnitRps) {
            MaterialInfoPojo mat = new MaterialInfoPojo();
            String materialNum = rp.getMaterialNum();
            mat.setMaterialNum(materialNum);
            try {
                mat.setMaterialGroup(MaterialGroupPropertitesUtil.props.getProperty(materialNum.substring(0, 4)));
                mat.setBaseUnit(BaseUnitPropertitesUtil.props.getProperty(mat.getMaterialGroup()));
            } catch (Exception e) {
            }
            materialInfoPojos.add(mat);
        }

        return R.success(materialInfoPojos);
    }
}
