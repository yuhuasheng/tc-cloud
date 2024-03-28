package com.foxconn.dp.plm.hdfs.domain.entity;

import com.foxconn.plm.entity.Entity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class FolderEntity extends Entity implements Serializable {

    long pid;
    long id;

    @ApiModelProperty(value = "文件夹名称")
    String name;

    String fldDesc;

    @ApiModelProperty(value = "所有子项统计数量")
    int totalCount;

    @ApiModelProperty(value = "本文件夹的ItemRev统计数量")
    int itemCount;

    FolderEntity parentFolder;

    List<FolderEntity> subFolder;

    public void addChild(FolderEntity e) {
        if (subFolder == null) {
            subFolder = new ArrayList<>();
        }
        subFolder.add(e);
    }

}
