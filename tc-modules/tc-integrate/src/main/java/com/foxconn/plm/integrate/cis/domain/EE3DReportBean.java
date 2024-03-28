package com.foxconn.plm.integrate.cis.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.foxconn.plm.entity.constants.TCPropName;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Set;

@Data
public class EE3DReportBean {
    @TCPropName(cell = 0)
    private String bu;

    @TCPropName(cell = 1)
    private String customer;

    @TCPropName(cell = 2)
    private String projectSeries;

    @TCPropName(cell = 3)
    private String projectName;

    @TCPropName(cell = 4)
    private String phase;

    @TCPropName(cell = 5)
    private String version;

    @TCPropName(cell = 6)
    private String category;

    @TCPropName(cell = 7)
    private String partType;

    @TCPropName(cell = 8)
    private Integer partCount;

    @TCPropName(cell = 9)
    private Integer part3DCount;

    @JsonIgnore
    private ItemRevision bom;

    @JsonIgnore
    private Set<ItemRevision> items;//= Collections.synchronizedSet(new HashSet<>());

    @JsonIgnore
    private Folder tempFolder;

    @JsonIgnore
    private String tempFolderName;

    @TCPropName(cell = 10)
    @Setter(AccessLevel.NONE)
    private String part3DPercent;


    public String getPart3DPercent() {
        if (partCount > 0 && part3DCount > 0) {
            BigDecimal partCountBd = new BigDecimal(partCount);
            BigDecimal part3DCountBd = new BigDecimal(part3DCount * 100);
            BigDecimal result = part3DCountBd.divide(partCountBd, 3, RoundingMode.DOWN);
            DecimalFormat dt = new DecimalFormat("0.##");
            return dt.format(result) + "%";
        }
        return "0%";
    }

    public void setPercent() {
        this.part3DPercent = getPart3DPercent();
    }

}
