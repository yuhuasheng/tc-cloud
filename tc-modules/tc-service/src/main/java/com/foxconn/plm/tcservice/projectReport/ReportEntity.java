package com.foxconn.plm.tcservice.projectReport;

import lombok.Data;

public class ReportEntity implements Cloneable {

    public String bu = "";
    public String customer = "";
    public String productLine = "";
    public String series = "";
    public String projectName = "";
    public String phase = "";
    public String historicalPhase = "";
    public String historicalPhaseShort = "";
    public String phaseEndDate = "";
    public String phaseShort = "";
    public String megerFlag = "";
    public boolean isCurrentPhase;
    public boolean isNeedComplete;
    public String currentPhase;
    public String needComplete;
    public float overallOutputProgress;
    public String dept = "";
    public int index;
    public boolean isSummery;
    /**
     * 當前Phase or 歷史Phase
     */
    public String phaseType;
    /**
     * 工作&流程中圖文檔數量
     * 專案過程產出物數量
     */
    public int workflowDiagramDocumentQty;
    /**
     * 專案階段產出物數量
     */

    public int archivedQty;
    /**
     * 已產出交付物數量
     * 專案階段已產出物數量
     */
    public int outputDeliverableQty;
    /**
     * 應產出交付物數量
     * 專案階段應產出物數量
     */
    public int shouldOutputDeliverableQty;
    public float outputProgress;
    public String pid = "";
    public String status = "";
    public String spm = "";
    public long overallOutputProgressFlag;

    @Override
    protected ReportEntity clone() throws CloneNotSupportedException {
        return (ReportEntity) super.clone();
    }
}
