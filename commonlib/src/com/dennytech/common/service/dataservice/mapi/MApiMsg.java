package com.dennytech.common.service.dataservice.mapi;

/**
 * MApi层的错误封装类。通过resp.message()获取。
 * 
 * @author Jun.Deng
 * 
 */
public class MApiMsg {

	private int errorNo;
	private String errorMsg;

	public MApiMsg(int no, String msg) {
		this.errorNo = no;
		this.errorMsg = msg;
	}

	public int getErrorNo() {
		return errorNo;
	}

	public void setErrorNo(int errorNo) {
		this.errorNo = errorNo;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	@Override
	public String toString() {
		return errorMsg + " (" + errorNo + ")";
	}

}
