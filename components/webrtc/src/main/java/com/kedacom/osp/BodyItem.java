package com.kedacom.osp;

import java.util.Arrays;


public class BodyItem{
	private String  strBodyName;     ///消息体名称
	private byte[]  abyContent;      ///消息体内容
	
	BodyItem()
	{
		this.strBodyName = "";
		this.abyContent = null;
	}
	
	BodyItem(String strBodyName, byte[] abyContent )
	{
		this.strBodyName = strBodyName;
		
		if ( null == this.abyContent )
		{
			this.abyContent = new byte[ abyContent.length ];
		}
		
		System.arraycopy( abyContent, 0, this.abyContent, 0, abyContent.length );	
	}
	
	public String getStrBodyName() {
		return strBodyName;
	}

	public void setStrBodyName(String strBodyName) {
		this.strBodyName = strBodyName;
	}

	public byte[] getAbyContent() {
		return abyContent;
	}

	public void setAbyContent(byte[] abyContent) {
		this.abyContent = abyContent;
	}

	@Override
	public String toString() {
		return "BodyItem [strBodyName=" + strBodyName + ", abyContent="
				+ Arrays.toString(abyContent) + "]";
	}
	
	
	
}