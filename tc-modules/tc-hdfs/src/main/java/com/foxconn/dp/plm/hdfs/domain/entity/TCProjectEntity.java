package com.foxconn.dp.plm.hdfs.domain.entity;

import com.foxconn.plm.entity.Entity;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;

import java.util.Objects;

@Data
public class TCProjectEntity extends Entity {
    String id;
    Integer sid;

    @ApiModelProperty(value = "专案当前所在阶段")
    String phase;

    @ApiModelProperty(value = "专案名称")
    String name;

    @ApiModelProperty(value = "事业处")
    String bu;

    @ApiModelProperty(value = "顶层文件夹ID")
    String folderId;
    @ApiModelProperty(value = "客户名称")
    String customerName;
    Long customerId;

    @ApiModelProperty(value = "产品线名称")
    String productLine;
    Long productLineId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TCProjectEntity tcProject = (TCProjectEntity) o;

        return Objects.equals(id, tcProject.id);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
