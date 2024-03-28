package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TempInfo  implements Serializable, Cloneable{


      private String name;
      private String descr;

      List<TempInfo> children;


}
