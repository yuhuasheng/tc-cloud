package com.foxconn.plm.entity.response;

import com.foxconn.plm.entity.constants.TCPropName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SPASProject implements Comparable<SPASProject> {

    @TCPropName(cell = 5)
    private String projectId;

    @TCPropName(cell = 6)
    private String projectName;

    @TCPropName(cell = 3)
    private String customer;

    @TCPropName(cell = 4)
    private String productLine;

    @TCPropName(cell = 1)
    private String levels;

    @TCPropName(cell = 2)
    private String phase;

    @TCPropName(cell = 0)
    private String bu;

    private List<PhaseBean> phases = new ArrayList<>();

    /**
     * 按照专案难易程度和phase来排序
     * @param o
     * @return
     */
    @Override
    public int compareTo(SPASProject o) {
        int i = this.levels.compareTo(o.getLevels());
        if (i == 0) {
            int j = this.phase.compareTo(o.getPhase());
            if (j == 0) {
                return this.customer.compareTo(o.getCustomer());
            } else {
                return j;
            }
        }
        return i;
    }
}
