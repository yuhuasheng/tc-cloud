package com.foxconn.plm.tcservice.connandcable.domain;

import lombok.Data;

@Data
public class TCCablePojo {
	private String itemId;
	private String itemRev;
	private String itemName;
	private String itemDesc;
	private String customer3DRev;
	private String customer2DRev;
	private String customerDrawingNumber;
	private String hhpn;
	private String customer;
	private String customerPN;
	private String chineseDesc;
	private String englishDesc;
}
