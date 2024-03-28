package com.foxconn.plm.integrateb2b.dataExchange.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BOMActionInfo {
    private String xfe_cn_num;
    private String xfe_mm_num;
    private String xfe_find_num;
    private String xfe_component_num;
    private String xfe_component_qty;
    private String xfe_alt_group;
    private String xfe_alt_code;
    private String xfe_unit;
    private String xfe_item_text;
    private String xfe_item_category;
    private String xfe_priority;
    private String xfe_bom_usage;
    private String xfe_alternative_bom;
    private String xfe_base_quantity;
    private String xfe_strategy;
    private String xfe_location;
    private String xfe_usage_prob;
    private String xfe_action;
    private String xfe_material_uid;
    private String xfe_component_uid;
}

