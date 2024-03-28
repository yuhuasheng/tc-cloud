package com.foxconn.plm.tcservice.benefitreport.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;

public class SerializerBigDecimal extends JsonSerializer<BigDecimal> implements ContextualSerializer {

    protected DecimalFormat decimalFormat;

    public SerializerBigDecimal() {
    }

    public SerializerBigDecimal(DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        if (Objects.isNull(value)) {
            gen.writeNull();
        } else {
            if (null != decimalFormat) {
                gen.writeNumber(decimalFormat.format(value));
            } else {
                gen.writeNumber(value.setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {

        JsonFormat.Value format = findFormatOverrides(prov, property, handledType());
        if (format == null) {
            return this;
        }

        if (format.hasPattern()) {
            DecimalFormat decimalFormat = new DecimalFormat(format.getPattern());
            decimalFormat.setRoundingMode(RoundingMode.DOWN);
            return new SerializerBigDecimal(decimalFormat);
        }

        return this;
    }

    protected JsonFormat.Value findFormatOverrides(SerializerProvider provider,
                                                   BeanProperty prop, Class<?> typeForDefaults) {
        if (prop != null) {
            return prop.findPropertyFormat(provider.getConfig(), typeForDefaults);
        }
        return provider.getDefaultPropertyFormat(typeForDefaults);
    }
}
