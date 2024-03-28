package com.foxconn.plm.tcservice.benefitreport.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.foxconn.plm.tcservice.benefitreport.util.SerializerBigDecimal;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;

@Data

public class BenefitCollectBean {
    @JsonSerialize(using = SerializerBigDecimal.class)
    @JsonFormat(pattern = "0.0000", shape = JsonFormat.Shape.STRING)
    private BigDecimal mnt = new BigDecimal(0);
    @JsonSerialize(using = SerializerBigDecimal.class)
    @JsonFormat(pattern = "0.0000", shape = JsonFormat.Shape.STRING)
    private BigDecimal dt = new BigDecimal(0);
    @JsonSerialize(using = SerializerBigDecimal.class)
    @JsonFormat(pattern = "0.0000", shape = JsonFormat.Shape.STRING)
    private BigDecimal prt = new BigDecimal(0);
    @JsonSerialize(using = SerializerBigDecimal.class)
    @JsonFormat(pattern = "0.0000", shape = JsonFormat.Shape.STRING)
    @Setter(AccessLevel.NONE)
    private BigDecimal all;


    private String name;

    public BigDecimal getAll() {
        return mnt.add(dt).add(prt);
    }

}
