package com.foxconn.plm.integrate.sap.maker.mapper;

import com.foxconn.plm.integrate.sap.maker.domain.Maker;
import com.foxconn.plm.integrate.sap.maker.domain.MakerInfoEntity;
import com.foxconn.plm.integrate.sap.maker.domain.rp.SearchMakerRp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface MakerMapper {

    /**
     * 查询供应商信息
     *
     * @throws Exception
     */
    public abstract List<MakerInfoEntity> searchMakerInfo(SearchMakerRp searchMakerRp);

    public abstract void addMakerSas(Maker maker);

    public abstract void addMakerInl(Maker maker);

    public abstract void deleteMakerSas(Maker maker);

    public abstract void deleteMakerInl(Maker maker);
}
