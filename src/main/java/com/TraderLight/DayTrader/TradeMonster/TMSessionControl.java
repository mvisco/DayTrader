/*
 * Copyright 2016 Mario Visco, TraderLight LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 **/
package com.TraderLight.DayTrader.TradeMonster;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class TMSessionControl {
	
	private static String sessionid;
	private static String token;
	private static String userId;
	private static String initialCookie="C04B45B4B0B0BE579D8CF9EFEAF475F2.retail1";
	private static String monster_cookie="";
	private static String uuid = "";
	private static String sourceApp;
	private static List<String> cookies=new ArrayList<String>();
	public static final Logger log = Logging.getLogger(true);
	
	public static String getSessionid() {
		if(sessionid==null){
			throw new RuntimeException("please login first");
		}
		return sessionid;
	}
	

	public static void setSessionid(String sessionid) {
		TMSessionControl.sessionid = sessionid;
		
	}

	public static void setToken(String token) {
		TMSessionControl.token = token;
	}

	public static void setUser(long userId) {
		TMSessionControl.userId = String.valueOf(userId);
	}

	public static void setSourceApp(String sourceApp) {
		TMSessionControl.sourceApp = sourceApp;
	}
	public static String getuserId() {
		return userId;
	}

	public static String getToken() {
		return token;
	}
	
	public static void  setCookie(List<String> cookie) {
		TMSessionControl.cookies = cookie;
	}
	
	public static List<String> getCookie() {
		return TMSessionControl.cookies;
	}
	
	public static String getInitialCookie() {
		return TMSessionControl.initialCookie;
	}
	
	public static void setMonster(String monster) {
		
		TMSessionControl.monster_cookie = monster;
		log.info("setting monster cookie " + TMSessionControl.monster_cookie);
	}
	
    public static void setUUID(String uuid) {
		
		TMSessionControl.uuid = uuid;
		log.info("setting uuid " + TMSessionControl.uuid);
	}
	
    public static String getMonster() {
    	return TMSessionControl.monster_cookie;
    }
    
    public static String getUUID() {
    	return TMSessionControl.uuid;
    }
	
    public static String getSourceApp() {
    	return TMSessionControl.sourceApp;
    }
}
