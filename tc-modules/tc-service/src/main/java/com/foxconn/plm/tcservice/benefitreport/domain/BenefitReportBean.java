package com.foxconn.plm.tcservice.benefitreport.domain;

public class BenefitReportBean {
    private String custom = "";
    private String difficultLevel = "";
    private String phase = "";
    private String projectDellForDT = "";
    private String projectLenovoForDT = "";
    private String projectHPForDT = "";
    private String projectAllForMNT = "";
    private String projectPrinterForPRT = "";
    private String projectIIDForPRT = "";
    private String benefit = "";

    public BenefitReportBean() {
        super();
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    public String getDifficultLevel() {
        return difficultLevel;
    }

    public void setDifficultLevel(String difficultLevel) {
        this.difficultLevel = difficultLevel;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getProjectDellForDT() {
        return projectDellForDT;
    }

    public void setProjectDellForDT(String projectDellForDT) {
        this.projectDellForDT = projectDellForDT;
    }

    public String getProjectLenovoForDT() {
        return projectLenovoForDT;
    }

    public void setProjectLenovoForDT(String projectLenovoForDT) {
        this.projectLenovoForDT = projectLenovoForDT;
    }

    public String getProjectHPForDT() {
        return projectHPForDT;
    }

    public void setProjectHPForDT(String projectHPForDT) {
        this.projectHPForDT = projectHPForDT;
    }

    public String getProjectAllForMNT() {
        return projectAllForMNT;
    }

    public void setProjectAllForMNT(String projectAllForMNT) {
        this.projectAllForMNT = projectAllForMNT;
    }

    public String getProjectPrinterForPRT() {
        return projectPrinterForPRT;
    }

    public void setProjectPrinterForPRT(String projectPrinterForPRT) {
        this.projectPrinterForPRT = projectPrinterForPRT;
    }

    public String getProjectIIDForPRT() {
        return projectIIDForPRT;
    }

    public void setProjectIIDForPRT(String projectIIDForPRT) {
        this.projectIIDForPRT = projectIIDForPRT;
    }

    public String getBenefit() {
        return benefit;
    }

    public void setBenefit(String benefit) {
        this.benefit = benefit;
    }

    @Override
    public String toString() {
        return "BenefitReportBean [custom=" + custom + ", difficultLevel="
                + difficultLevel + ", phase=" + phase + ", projectDellForDT="
                + projectDellForDT + ", projectLenovoForDT="
                + projectLenovoForDT + ", projectHPForDT=" + projectHPForDT
                + ", projectAllForMNT=" + projectAllForMNT
                + ", projectPrinterForPRT=" + projectPrinterForPRT
                + ", projectIIDForPRT=" + projectIIDForPRT + ", benefit="
                + benefit + "]";
    }


}
