package com.foxconn.plm.cis.domain;

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
    @TCPropName(cell = 0, isMerge = true)
    private String bu;

    @TCPropName(cell = 1, isMerge = true)
    private String customer;

    @TCPropName(cell = 2, isMerge = true)
    private String projectSeries;

    @TCPropName(cell = 3, isMerge = true)
    private String projectName;

    @TCPropName(cell = 4, isMerge = true)
    private String phase;

    @TCPropName(cell = 5, isMerge = true)
    private String version;

    @TCPropName(cell = 6, isMerge = true)
    private String category;

    @TCPropName(cell = 7, isMerge = true)
    private String partType;

    @TCPropName(cell = 8)
    private Integer partCount;

    @TCPropName(cell = 9)
    private Integer part3DCount;

    private Set<String> noCisModel;

    @JsonIgnore
    private ItemRevision bom;

    @JsonIgnore
    private Set<ItemRevision> items;//= Collections.synchronizedSet(new HashSet<>());

    @JsonIgnore
    private Folder tempFolder;

    @JsonIgnore
    private String tempFolderName;

    @TCPropName(cell = 10)
    //@Setter(AccessLevel.NONE)
    private String part3DPercent;


    public String getPart3DPercent() {
        if (partCount != null && part3DCount != null && partCount > 0 && part3DCount > 0) {
            BigDecimal partCountBd = new BigDecimal(partCount);
            BigDecimal part3DCountBd = new BigDecimal(part3DCount * 100);
            BigDecimal result = part3DCountBd.divide(partCountBd, 3, RoundingMode.DOWN);
            DecimalFormat dt = new DecimalFormat("0.00");
            return dt.format(result) + "%";
        }
        return "0.00%";
    }

    public void setPercent() {
        this.part3DPercent = getPart3DPercent();
    }

}
