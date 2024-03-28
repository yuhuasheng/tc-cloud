package com.foxconn.plm.entity.response;

import lombok.Data;

@Data
public class HealthStatusRv    {
    public String service;
    public String status = "正常";
    public String jvmMaxMemory;
    public String jvmTotalMemory;
    public String jvmUsedMemory;
    public String jvmThreadNum;
    public String faultTime;

    public HealthStatusRv(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJvmMaxMemory() {
        return jvmMaxMemory;
    }

    public void setJvmMaxMemory(String jvmMaxMemory) {
        this.jvmMaxMemory = jvmMaxMemory;
    }

    public String getJvmTotalMemory() {
        return jvmTotalMemory;
    }

    public void setJvmTotalMemory(String jvmTotalMemory) {
        this.jvmTotalMemory = jvmTotalMemory;
    }

    public String getJvmUsedMemory() {
        return jvmUsedMemory;
    }

    public void setJvmUsedMemory(String jvmUsedMemory) {
        this.jvmUsedMemory = jvmUsedMemory;
    }

    public String getJvmThreadNum() {
        return jvmThreadNum;
    }

    public void setJvmThreadNum(String jvmThreadNum) {
        this.jvmThreadNum = jvmThreadNum;
    }

    public String getFaultTime() {
        return faultTime;
    }

    public void setFaultTime(String faultTime) {
        this.faultTime = faultTime;
    }
}
