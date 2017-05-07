package com.TraderLight.DayTrader.Ameritrade;


public class SessionControl {
	
	private static String sessionid;
	private static String segment;
	private static String company;
	private static String sourceApp;
	private static String url;
	private static String acct;
	
	public static String getUrl() {
		return url;
	}

	public static void setUrl(String url) {
		SessionControl.url = url;
	}

	public static String getSessionid() {
		if(sessionid==null){
			throw new RuntimeException("please login first");
		}
		return sessionid;
	}

	public static String getSourceApp() {
		return sourceApp;
	}

	public static void setSourceApp(String sourceApp) {
		SessionControl.sourceApp = sourceApp;
	}

	public static void setSessionid(String sessionid) {
		SessionControl.sessionid = sessionid;
		
	}

	public static void setSegment(String segment) {
		SessionControl.segment = segment;
	}

	public static void setCompany(String company) {
		SessionControl.company = company;
	}

	public static String getCompany() {
		return company;
	}

	public static String getSegment() {
		return segment;
	}

	public static String getAcct() {
		return acct;
	}

	public static void setAcct(String acct) {
		SessionControl.acct = acct;
	}
	
	
}
