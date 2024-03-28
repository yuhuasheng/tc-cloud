package com.foxconn.plm.tcservice.ebom.domain;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class EBOMUpdateBean {
    private EBOMLineBean newEBomBean;
    private EBOMLineBean oldEBomBean;
    private List<String> changeFiledNames = new ArrayList<String>();

    public EBOMUpdateBean(EBOMLineBean newEBomBean, EBOMLineBean oldEBomBean) {
        this.newEBomBean = newEBomBean;
        this.oldEBomBean = oldEBomBean;
    }

    public void change(String[] changeField) throws Exception {
        for (String fieldName : changeField) {
            Field field = EBOMLineBean.class.getDeclaredField(fieldName);
            ReflectionUtils.makeAccessible(field);
            // TCPropName tcProp = field.getAnnotation(TCPropName.class);
            Object nOb = field.get(newEBomBean);
            Object oOb = field.get(oldEBomBean);
            if (!nOb.equals(oOb)) {
                changeFiledNames.add(field.getName());
            }
        }
    }

    public List<EBOMLineBean> getAdd2nd() {
        List<EBOMLineBean> add = new ArrayList<>();
        if (newEBomBean.getSecondSource() != null) {
            add.addAll(newEBomBean.getSecondSource());
            if (oldEBomBean.getSecondSource() != null) {
                add.removeAll(oldEBomBean.getSecondSource());
            }
        }
        return add;
    }

    public List<EBOMLineBean> getDel2nd() {
        List<EBOMLineBean> del = new ArrayList<EBOMLineBean>();
        if (oldEBomBean.getSecondSource() != null) {
            del.addAll(oldEBomBean.getSecondSource());
            if (newEBomBean.getSecondSource() != null) {
                del.removeAll(newEBomBean.getSecondSource());
            }
        }
        return del;
    }

    public EBOMLineBean getNewEBomBean() {
        return newEBomBean;
    }

    public void setNewEBomBean(EBOMLineBean newEBomBean) {
        this.newEBomBean = newEBomBean;
    }

    public EBOMLineBean getOldEBomBean() {
        return oldEBomBean;
    }

    public void setOldEBomBean(EBOMLineBean oldEBomBean) {
        this.oldEBomBean = oldEBomBean;
    }

    public List<String> getChangeFiledNames() {
        return changeFiledNames;
    }

    public void setChangeFiledNames(List<String> changeFiledNames) {
        this.changeFiledNames = changeFiledNames;
    }
}
