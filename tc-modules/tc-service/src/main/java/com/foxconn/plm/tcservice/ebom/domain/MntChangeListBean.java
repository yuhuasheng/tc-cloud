package com.foxconn.plm.tcservice.ebom.domain;

import java.util.List;

import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.tcservice.ebom.constant.ChangeAction;
import org.apache.commons.lang.StringUtils;


public class MntChangeListBean
{
    @TCPropName(cell = 0)
    private String                 no;
    @TCPropName(cell = 1)
    private String                 before_bomItem;
    @TCPropName(cell = 2)
    private String                 before_code;
    @TCPropName(cell = 3)
    private String                 parentPn;
    @TCPropName(cell = 4)
    private String                 before_pn;
    @TCPropName(cell = 5)
    private String                 before_description;
    @TCPropName(cell = 6)
    private String                 before_Unit;
    @TCPropName(cell = 7)
    private String                 before_qty;
    @TCPropName(cell = 8)
    private String                 before_remark;
    @TCPropName(cell = 9)
    private String                 after_bomItem;
    @TCPropName(cell = 10)
    private String                 after_code;
    @TCPropName(cell = 11)
    private String                 after_pn;
    @TCPropName(cell = 12)
    private String                 after_description;
    @TCPropName(cell = 13)
    private String                 after_Unit;
    @TCPropName(cell = 14)
    private String                 after_qty;
    @TCPropName(cell = 15)
    private String                 after_remark;
    private List<MergedRegionInfo> mergedRegionInfos;

    public MntChangeListBean(EBOMLineBean ebomBean, boolean isSubs, ChangeAction action)
    {
        this.parentPn = ebomBean.getParentItem();
        this.after_bomItem = "";
        this.after_code = action.value();
        this.after_pn = ebomBean.getItem();
        this.after_qty = StringUtils.isNotEmpty(ebomBean.getQty()) ? ebomBean.getQty() : "1";
        this.after_remark = "";
        this.after_Unit = ebomBean.getUnit();
        this.after_description = ebomBean.getDescription();
        this.before_bomItem = ebomBean.getFindNum() + "";
        this.before_code = action.value();
        this.before_description = ebomBean.getDescription();
        this.before_pn = ebomBean.getItem();
        this.before_qty = StringUtils.isNotEmpty(ebomBean.getQty()) ? ebomBean.getQty() : "1";
        this.before_remark = "";
        this.before_Unit = ebomBean.getUnit();
        if (action.equals(ChangeAction.Add))
        {
            this.after_bomItem = ebomBean.getFindNum() + "";
            this.before_bomItem = "";
            this.before_description = "";
            this.before_pn = "";
            this.before_qty = "";
            this.before_Unit = "";
            if (isSubs)
            {
                this.after_remark = "增加替代料";
                this.after_qty = "0";
                this.after_bomItem = "";
            }
            else
            {
                this.after_remark = "增加物料";
            }
        }
        else if (action.equals(ChangeAction.Delete))
        {
            this.after_qty = "0";
            if (isSubs)
            {
                this.before_qty = "0";
                this.after_remark = "删除替代料";
                this.before_bomItem = "";
            }
            else
            {
                this.after_remark = "删除物料";
            }
        }
        else if (action.equals(ChangeAction.Change))
        {
            this.after_remark = "變更用量";
        }
    }

    public String getNo()
    {
        return no;
    }

    public void setNo(String no)
    {
        this.no = no;
    }

    public String getBefore_bomItem()
    {
        return before_bomItem;
    }

    public void setBefore_bomItem(String before_bomItem)
    {
        this.before_bomItem = before_bomItem;
    }

    public String getBefore_code()
    {
        return before_code;
    }

    public void setBefore_code(String before_code)
    {
        this.before_code = before_code;
    }

    public String getBefore_pn()
    {
        return before_pn;
    }

    public void setBefore_pn(String before_pn)
    {
        this.before_pn = before_pn;
    }

    public String getBefore_description()
    {
        return before_description;
    }

    public void setBefore_description(String before_description)
    {
        this.before_description = before_description;
    }

    public String getBefore_Unit()
    {
        return before_Unit;
    }

    public void setBefore_Unit(String before_Unit)
    {
        this.before_Unit = before_Unit;
    }

    public String getBefore_qty()
    {
        return before_qty;
    }

    public void setBefore_qty(String before_qty)
    {
        this.before_qty = before_qty;
    }

    public String getBefore_remark()
    {
        return before_remark;
    }

    public void setBefore_remark(String before_remark)
    {
        this.before_remark = before_remark;
    }

    public String getAfter_bomItem()
    {
        return after_bomItem;
    }

    public void setAfter_bomItem(String after_bomItem)
    {
        this.after_bomItem = after_bomItem;
    }

    public String getAfter_code()
    {
        return after_code;
    }

    public void setAfter_code(String after_code)
    {
        this.after_code = after_code;
    }

    public String getAfter_pn()
    {
        return after_pn;
    }

    public void setAfter_pn(String after_pn)
    {
        this.after_pn = after_pn;
    }

    public String getAfter_description()
    {
        return after_description;
    }

    public void setAfter_description(String after_description)
    {
        this.after_description = after_description;
    }

    public String getAfter_Unit()
    {
        return after_Unit;
    }

    public void setAfter_Unit(String after_Unit)
    {
        this.after_Unit = after_Unit;
    }

    public String getAfter_qty()
    {
        return after_qty;
    }

    public void setAfter_qty(String after_qty)
    {
        this.after_qty = after_qty;
    }

    public String getAfter_remark()
    {
        return after_remark;
    }

    public void setAfter_remark(String after_remark)
    {
        this.after_remark = after_remark;
    }

    public String getParentPn()
    {
        return parentPn;
    }

    public void setParentPn(String parentPn)
    {
        this.parentPn = parentPn;
    }

    public List<MergedRegionInfo> getMergedRegionInfos()
    {
        return mergedRegionInfos;
    }

    public void setMergedRegionInfos(List<MergedRegionInfo> mergedRegionInfos)
    {
        this.mergedRegionInfos = mergedRegionInfos;
    }
}
