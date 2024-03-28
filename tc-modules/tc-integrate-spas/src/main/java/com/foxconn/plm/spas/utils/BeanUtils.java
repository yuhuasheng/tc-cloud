/**
 * 
 */
package com.foxconn.plm.spas.utils;

import com.foxconn.plm.spas.bean.TempInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author F1028798
 *
 */
public class BeanUtils {
	public static TempInfo cloneBean(TempInfo tempInfo) {
		if (null == tempInfo) {
			return null;
		}

		TempInfo cloneBOM = null;

		cloneBOM = new TempInfo();

		
		cloneBOM.setName(tempInfo.getName());
		cloneBOM.setDescr(tempInfo.getDescr());
		cloneBOM.setChildren(cloneList(tempInfo.getChildren()));

		return cloneBOM;
	}


	public static List<TempInfo> cloneList(List<TempInfo> childList) {
		if (null == childList || childList.size() == 0) {
			return null;
		}
		List<TempInfo> list = new ArrayList<>();
		for (TempInfo bomPojo : childList) {
			TempInfo newBean = cloneBean(bomPojo);
			list.add(newBean);
		}
		return list;
	}




}
