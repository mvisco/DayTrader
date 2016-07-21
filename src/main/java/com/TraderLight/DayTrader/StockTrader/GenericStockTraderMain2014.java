package com.noname.TraderLight;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.noname.AccountMgmt.GenericAccountMgrOptionsSimulation;
import com.noname.AccountMgmt.GenericAccountMgrStocksSimulation;
import com.noname.AccountMgmt.OptionTradeHistory;
import com.noname.AccountMgmt.StockTradeRecord;
import com.noname.MarketDataStorage.GetFromStorageJDBC;
import com.noname.MarketDataStorage.Level1Quote;
import com.noname.Statistics.NewVolumeAverages;


public class GenericStockTraderMain2014 {
	
	public static final Logger log = Logging.getLogger(true);
	public static List<Stock1> stockList = new ArrayList<Stock1>();
	public static Map<Date, Double> stats_trade = new HashMap<Date,Double>();
	public static Level1Quote prevQuote;
	public static Map<String,List<StockTradeRecord>> allTrades=new HashMap<String,List<StockTradeRecord>>();
	public static double totalTradeCost=0;
	public static double capital_available = 160000.0;
	public static int max_number_of_positions = 2;
	public static int strategy_n =2 ; 
	
	
	public static void main(String[] args) {
		
		int windowSize = 5;
		List<Integer> initialDayIndex = new ArrayList<Integer>();
		List<Integer> finalDayIndex = new ArrayList<Integer>();
		int morningOpenIndex;
		int closeOpeningIndex;
		EPServiceProvider epService;
		int n =1; // used for calculating portfolio value
		double portfolioLoss=0;
		double portfolioLossThreshold=-100000;
	    final double DAILYGAIN=10000;
		double dailyGain=DAILYGAIN;
		
/*
initialDayIndex.add(667);    // Mon Jan 06 00:00:00 MST 2014
initialDayIndex.add(130590);    // Tue Jan 07 00:00:00 MST 2014
initialDayIndex.add(262332);    // Wed Jan 08 00:00:00 MST 2014
initialDayIndex.add(392238);    // Thu Jan 09 00:00:00 MST 2014
initialDayIndex.add(523692);    // Fri Jan 10 00:00:00 MST 2014
initialDayIndex.add(655398);    // Mon Jan 13 00:00:00 MST 2014
initialDayIndex.add(785916);    // Tue Jan 14 00:00:00 MST 2014
initialDayIndex.add(917658);    // Wed Jan 15 00:00:00 MST 2014
initialDayIndex.add(1047528);    // Thu Jan 16 00:00:00 MST 2014
initialDayIndex.add(1179180);    // Fri Jan 17 00:00:00 MST 2014
initialDayIndex.add(1308744);    // Tue Jan 21 00:00:00 MST 2014
initialDayIndex.add(1438146);    // Wed Jan 22 00:00:00 MST 2014
initialDayIndex.add(1567620);    // Thu Jan 23 00:00:00 MST 2014
initialDayIndex.add(1698084);    // Fri Jan 24 00:00:00 MST 2014
initialDayIndex.add(1828836);    // Mon Jan 27 00:00:00 MST 2014
initialDayIndex.add(1958184);    // Tue Jan 28 00:00:00 MST 2014
initialDayIndex.add(2087748);    // Wed Jan 29 00:00:00 MST 2014
initialDayIndex.add(2217474);    // Thu Jan 30 00:00:00 MST 2014
initialDayIndex.add(2345508);    // Fri Jan 31 00:00:00 MST 2014


initialDayIndex.add(2474532);    // Mon Feb 03 00:00:00 MST 2014
initialDayIndex.add(2603160);    // Tue Feb 04 00:00:00 MST 2014
initialDayIndex.add(2733390);    // Wed Feb 05 00:00:00 MST 2014
initialDayIndex.add(2861946);    // Thu Feb 06 00:00:00 MST 2014
initialDayIndex.add(2992302);    // Fri Feb 07 00:00:00 MST 2014
initialDayIndex.add(3122334);    // Mon Feb 10 00:00:00 MST 2014
initialDayIndex.add(3252402);    // Tue Feb 11 00:00:00 MST 2014
initialDayIndex.add(3382866);    // Wed Feb 12 00:00:00 MST 2014
initialDayIndex.add(3513240);    // Thu Feb 13 00:00:00 MST 2014
initialDayIndex.add(3643398);    // Fri Feb 14 00:00:00 MST 2014
initialDayIndex.add(3772674);    // Tue Feb 18 00:00:00 MST 2014
initialDayIndex.add(3903210);    // Wed Feb 19 00:00:00 MST 2014
initialDayIndex.add(4031262);    // Thu Feb 20 00:00:00 MST 2014
initialDayIndex.add(4162410);    // Fri Feb 21 00:00:00 MST 2014
initialDayIndex.add(4293882);    // Mon Feb 24 00:00:00 MST 2014
initialDayIndex.add(4424256);    // Tue Feb 25 00:00:00 MST 2014
initialDayIndex.add(4554738);    // Wed Feb 26 00:00:00 MST 2014
initialDayIndex.add(4685958);    // Thu Feb 27 00:00:00 MST 2014
initialDayIndex.add(4815756);    // Fri Feb 28 00:00:00 MST 2014

		
	
initialDayIndex.add(4947480);    // Mon Mar 03 00:00:00 MST 2014
initialDayIndex.add(5076774);    // Tue Mar 04 00:00:00 MST 2014
initialDayIndex.add(5205870);    // Wed Mar 05 00:00:00 MST 2014
initialDayIndex.add(5335308);    // Thu Mar 06 00:00:00 MST 2014
initialDayIndex.add(5465700);    // Fri Mar 07 00:00:00 MST 2014
initialDayIndex.add(5595192);    // Mon Mar 10 00:00:00 MDT 2014
initialDayIndex.add(5723784);    // Tue Mar 11 00:00:00 MDT 2014
initialDayIndex.add(5852160);    // Wed Mar 12 00:00:00 MDT 2014
initialDayIndex.add(5981526);    // Fri Mar 14 00:00:00 MDT 2014
initialDayIndex.add(6111720);    // Mon Mar 17 00:00:00 MDT 2014
initialDayIndex.add(6240852);    // Tue Mar 18 00:00:00 MDT 2014
initialDayIndex.add(6370182);    // Wed Mar 19 00:00:00 MDT 2014
initialDayIndex.add(6499692);    // Thu Mar 20 00:00:00 MDT 2014
initialDayIndex.add(6628086);    // Fri Mar 21 00:00:00 MDT 2014
initialDayIndex.add(6758298);    // Mon Mar 24 00:00:00 MDT 2014
initialDayIndex.add(6888888);    // Tue Mar 25 00:00:00 MDT 2014
initialDayIndex.add(7018128);    // Wed Mar 26 00:00:00 MDT 2014
initialDayIndex.add(7149078);    // Thu Mar 27 00:00:00 MDT 2014
initialDayIndex.add(7279362);    // Fri Mar 28 00:00:00 MDT 2014
initialDayIndex.add(7409214);    // Mon Mar 31 00:00:00 MDT 2014



initialDayIndex.add(7539210);    // Tue Apr 01 00:00:00 MDT 2014
initialDayIndex.add(7668252);    // Wed Apr 02 00:00:00 MDT 2014
initialDayIndex.add(7796754);    // Thu Apr 03 00:00:00 MDT 2014
initialDayIndex.add(7925364);    // Fri Apr 04 00:00:00 MDT 2014
initialDayIndex.add(8054162);    // Mon Apr 07 00:00:00 MDT 2014
initialDayIndex.add(8184825);    // Tue Apr 08 00:00:00 MDT 2014
initialDayIndex.add(8315431);    // Wed Apr 09 00:00:00 MDT 2014
initialDayIndex.add(8445410);    // Thu Apr 10 00:00:00 MDT 2014
initialDayIndex.add(8575370);    // Fri Apr 11 00:00:00 MDT 2014
initialDayIndex.add(8706546);    // Mon Apr 14 00:00:00 MDT 2014
initialDayIndex.add(8837418);    // Tue Apr 15 00:00:00 MDT 2014
initialDayIndex.add(8968746);    // Wed Apr 16 00:00:00 MDT 2014
initialDayIndex.add(9099400);    // Thu Apr 17 00:00:00 MDT 2014
initialDayIndex.add(9232304);    // Mon Apr 21 00:00:00 MDT 2014
initialDayIndex.add(9484757);    // Tue Apr 22 00:00:00 MDT 2014
initialDayIndex.add(9736355);    // Wed Apr 23 00:00:00 MDT 2014
initialDayIndex.add(9989758);    // Thu Apr 24 00:00:00 MDT 2014
initialDayIndex.add(10242629);    // Fri Apr 25 00:00:00 MDT 2014
initialDayIndex.add(10496887);    // Mon Apr 28 00:00:00 MDT 2014
initialDayIndex.add(10750138);    // Tue Apr 29 00:00:00 MDT 2014
initialDayIndex.add(11004909);    // Wed Apr 30 00:00:00 MDT 2014


initialDayIndex.add(11259224);    // Thu May 01 00:00:00 MDT 2014
initialDayIndex.add(11513463);    // Mon May 05 00:00:00 MDT 2014
initialDayIndex.add(11766790);    // Tue May 06 00:00:00 MDT 2014
initialDayIndex.add(12020307);    // Wed May 07 00:00:00 MDT 2014
initialDayIndex.add(12274432);    // Thu May 08 00:00:00 MDT 2014
initialDayIndex.add(12528652);    // Fri May 09 00:00:00 MDT 2014
initialDayIndex.add(12780820);    // Mon May 12 00:00:00 MDT 2014
initialDayIndex.add(13031031);    // Tue May 13 00:00:00 MDT 2014
initialDayIndex.add(13284225);    // Wed May 14 00:00:00 MDT 2014
initialDayIndex.add(13536778);    // Thu May 15 00:00:00 MDT 2014
initialDayIndex.add(13788922);    // Fri May 16 00:00:00 MDT 2014
initialDayIndex.add(14041774);    // Mon May 19 00:00:00 MDT 2014
initialDayIndex.add(14291719);    // Wed May 21 00:00:00 MDT 2014
initialDayIndex.add(14546642);    // Thu May 22 00:00:00 MDT 2014
initialDayIndex.add(14800881);    // Fri May 23 00:00:00 MDT 2014
initialDayIndex.add(15057685);    // Tue May 27 00:00:00 MDT 2014
initialDayIndex.add(15312057);    // Wed May 28 00:00:00 MDT 2014
initialDayIndex.add(15563617);    // Thu May 29 00:00:00 MDT 2014
initialDayIndex.add(15815918);    // Fri May 30 00:00:00 MDT 2014

initialDayIndex.add(16068238);    // Mon Jun 02 00:00:00 MDT 2014
initialDayIndex.add(16319627);    // Tue Jun 03 00:00:00 MDT 2014
initialDayIndex.add(16572118);    // Wed Jun 04 00:00:00 MDT 2014
initialDayIndex.add(16823583);    // Thu Jun 05 00:00:00 MDT 2014
initialDayIndex.add(17075409);    // Fri Jun 06 00:00:00 MDT 2014
initialDayIndex.add(17327235);    // Mon Jun 09 00:00:00 MDT 2014
initialDayIndex.add(17428486);    // Tue Jun 10 00:00:00 MDT 2014
initialDayIndex.add(17679989);    // Wed Jun 11 00:00:00 MDT 2014
initialDayIndex.add(17933715);    // Thu Jun 12 00:00:00 MDT 2014
initialDayIndex.add(18187061);    // Fri Jun 13 00:00:00 MDT 2014
initialDayIndex.add(18439476);    // Mon Jun 16 00:00:00 MDT 2014
initialDayIndex.add(18691454);    // Tue Jun 17 00:00:00 MDT 2014
initialDayIndex.add(18942539);    // Wed Jun 18 00:00:00 MDT 2014
initialDayIndex.add(19193225);    // Thu Jun 19 00:00:00 MDT 2014
initialDayIndex.add(19444538);    // Mon Jun 23 00:00:00 MDT 2014
initialDayIndex.add(19697162);    // Tue Jun 24 00:00:00 MDT 2014
initialDayIndex.add(19948399);    // Wed Jun 25 00:00:00 MDT 2014
initialDayIndex.add(20201650);    // Thu Jun 26 00:00:00 MDT 2014
initialDayIndex.add(20455224);    // Fri Jun 27 00:00:00 MDT 2014
initialDayIndex.add(20707734);    // Mon Jun 30 00:00:00 MDT 2014
*/

initialDayIndex.add(20959921);    // Tue Jul 01 00:00:00 MDT 2014
initialDayIndex.add(21212374);    // Wed Jul 02 00:00:00 MDT 2014
initialDayIndex.add(21468249);    // Wed Jul 09 00:00:00 MDT 2014
initialDayIndex.add(21961602);    // Thu Jul 10 00:00:00 MDT 2014
initialDayIndex.add(22388034);    // Fri Jul 11 00:00:00 MDT 2014
initialDayIndex.add(23108959);    // Mon Jul 14 00:00:00 MDT 2014
initialDayIndex.add(23715472);    // Tue Jul 15 00:00:00 MDT 2014
initialDayIndex.add(24334654);    // Wed Jul 16 00:00:00 MDT 2014
initialDayIndex.add(24945964);    // Thu Jul 17 00:00:00 MDT 2014
initialDayIndex.add(25548623);    // Fri Jul 18 00:00:00 MDT 2014
initialDayIndex.add(26147592);    // Mon Jul 21 00:00:00 MDT 2014
initialDayIndex.add(26748119);    // Tue Jul 22 00:00:00 MDT 2014
initialDayIndex.add(27357871);    // Wed Jul 23 00:00:00 MDT 2014
initialDayIndex.add(27967459);    // Thu Jul 24 00:00:00 MDT 2014
initialDayIndex.add(28579056);    // Fri Jul 25 00:00:00 MDT 2014
initialDayIndex.add(29187004);    // Mon Jul 28 00:00:00 MDT 2014
initialDayIndex.add(29787531);    // Tue Jul 29 00:00:00 MDT 2014
initialDayIndex.add(30389493);    // Wed Jul 30 00:00:00 MDT 2014
initialDayIndex.add(30998261);    // Thu Jul 31 00:00:00 MDT 2014

initialDayIndex.add(31604938);    // Fri Aug 01 00:00:00 MDT 2014
initialDayIndex.add(32203456);    // Mon Aug 04 00:00:00 MDT 2014
initialDayIndex.add(32809600);    // Tue Aug 05 00:00:00 MDT 2014
initialDayIndex.add(33427224);    // Wed Aug 06 00:00:00 MDT 2014
initialDayIndex.add(34048415);    // Thu Aug 07 00:00:00 MDT 2014
initialDayIndex.add(34678954);    // Fri Aug 08 00:00:00 MDT 2014
initialDayIndex.add(35304942);    // Mon Aug 11 00:00:00 MDT 2014
initialDayIndex.add(35930028);    // Tue Aug 12 00:00:00 MDT 2014
initialDayIndex.add(36549825);    // Wed Aug 13 00:00:00 MDT 2014
initialDayIndex.add(37169499);    // Thu Aug 14 00:00:00 MDT 2014
initialDayIndex.add(37774864);    // Fri Aug 15 00:00:00 MDT 2014
initialDayIndex.add(38395276);    // Mon Aug 18 00:00:00 MDT 2014
initialDayIndex.add(39024626);    // Tue Aug 19 00:00:00 MDT 2014
initialDayIndex.add(39651639);    // Wed Aug 20 00:00:00 MDT 2014
initialDayIndex.add(40303129);    // Thu Aug 21 00:00:00 MDT 2014
initialDayIndex.add(40960523);    // Fri Aug 22 00:00:00 MDT 2014
initialDayIndex.add(41615826);    // Mon Aug 25 00:00:00 MDT 2014
initialDayIndex.add(42266209);    // Tue Aug 26 00:00:00 MDT 2014
initialDayIndex.add(42917933);    // Wed Aug 27 00:00:00 MDT 2014
initialDayIndex.add(43555823);    // Thu Aug 28 00:00:00 MDT 2014
initialDayIndex.add(44196079);    // Fri Aug 29 00:00:00 MDT 2014

initialDayIndex.add(44835310);    // Tue Sep 02 00:00:00 MDT 2014
initialDayIndex.add(45467120);    // Wed Sep 03 00:00:00 MDT 2014
initialDayIndex.add(46124883);    // Thu Sep 04 00:00:00 MDT 2014
initialDayIndex.add(46755381);    // Fri Sep 05 00:00:00 MDT 2014
initialDayIndex.add(47375629);    // Mon Sep 08 00:00:00 MDT 2014
initialDayIndex.add(48010145);    // Tue Sep 09 00:00:00 MDT 2014
initialDayIndex.add(48645399);    // Wed Sep 10 00:00:00 MDT 2014
initialDayIndex.add(49255602);    // Thu Sep 11 00:00:00 MDT 2014
initialDayIndex.add(49888232);    // Fri Sep 12 00:00:00 MDT 2014
initialDayIndex.add(50525946);    // Mon Sep 15 00:00:00 MDT 2014
initialDayIndex.add(51156321);    // Tue Sep 16 00:00:00 MDT 2014
initialDayIndex.add(51780628);    // Wed Sep 17 00:00:00 MDT 2014
initialDayIndex.add(52388658);    // Thu Sep 18 00:00:00 MDT 2014
initialDayIndex.add(53018658);    // Fri Sep 19 00:00:00 MDT 2014
initialDayIndex.add(53653385);    // Mon Sep 22 00:00:00 MDT 2014
initialDayIndex.add(54280193);    // Tue Sep 23 00:00:00 MDT 2014
initialDayIndex.add(54912085);    // Wed Sep 24 00:00:00 MDT 2014
initialDayIndex.add(55542050);    // Thu Sep 25 00:00:00 MDT 2014
initialDayIndex.add(56171933);    // Fri Sep 26 00:00:00 MDT 2014
initialDayIndex.add(56809401);    // Mon Sep 29 00:00:00 MDT 2014
initialDayIndex.add(57431248);    // Tue Sep 30 00:00:00 MDT 2014

/*
initialDayIndex.add(58066789);    // Wed Oct 01 00:00:00 MDT 2014
initialDayIndex.add(58682000);    // Thu Oct 02 00:00:00 MDT 2014
initialDayIndex.add(59321717);    // Fri Oct 03 00:00:00 MDT 2014
initialDayIndex.add(59957434);    // Mon Oct 06 00:00:00 MDT 2014
initialDayIndex.add(60593865);    // Tue Oct 07 00:00:00 MDT 2014
initialDayIndex.add(61223010);    // Wed Oct 08 00:00:00 MDT 2014
initialDayIndex.add(61857772);    // Thu Oct 09 00:00:00 MDT 2014
initialDayIndex.add(62483145);    // Fri Oct 10 00:00:00 MDT 2014
initialDayIndex.add(63125287);    // Mon Oct 13 00:00:00 MDT 2014
initialDayIndex.add(63750729);    // Tue Oct 14 00:00:00 MDT 2014
initialDayIndex.add(64386447);    // Wed Oct 15 00:00:00 MDT 2014
initialDayIndex.add(65000996);    // Thu Oct 16 00:00:00 MDT 2014
initialDayIndex.add(65637029);    // Fri Oct 17 00:00:00 MDT 2014
initialDayIndex.add(66258671);    // Mon Oct 20 00:00:00 MDT 2014
initialDayIndex.add(66881871);    // Tue Oct 21 00:00:00 MDT 2014
initialDayIndex.add(67498142);    // Wed Oct 22 00:00:00 MDT 2014
initialDayIndex.add(68122777);    // Thu Oct 23 00:00:00 MDT 2014
initialDayIndex.add(68750412);    // Mon Oct 27 00:00:00 MDT 2014
initialDayIndex.add(69384962);    // Tue Oct 28 00:00:00 MDT 2014
initialDayIndex.add(69996354);    // Wed Oct 29 00:00:00 MDT 2014
initialDayIndex.add(70621317);    // Thu Oct 30 00:00:00 MDT 2014
initialDayIndex.add(71255013);    // Fri Oct 31 00:00:00 MDT 2014
 


initialDayIndex.add(71885709);    // Mon Nov 03 00:00:00 MST 2014
initialDayIndex.add(72526956);    // Wed Nov 05 00:00:00 MST 2014
initialDayIndex.add(73164096);    // Thu Nov 06 00:00:00 MST 2014
initialDayIndex.add(73801605);    // Fri Nov 07 00:00:00 MST 2014
initialDayIndex.add(74432114);    // Mon Nov 10 00:00:00 MST 2014
initialDayIndex.add(75072318);    // Tue Nov 11 00:00:00 MST 2014
initialDayIndex.add(75698675);    // Wed Nov 12 00:00:00 MST 2014
initialDayIndex.add(76329501);    // Thu Nov 13 00:00:00 MST 2014
initialDayIndex.add(76967953);    // Fri Nov 14 00:00:00 MST 2014
initialDayIndex.add(77594228);    // Mon Nov 17 00:00:00 MST 2014
initialDayIndex.add(78231819);    // Tue Nov 18 00:00:00 MST 2014
initialDayIndex.add(78868508);    // Wed Nov 19 00:00:00 MST 2014
initialDayIndex.add(79517251);    // Thu Nov 20 00:00:00 MST 2014
initialDayIndex.add(80160090);    // Fri Nov 21 00:00:00 MST 2014
initialDayIndex.add(80808177);    // Mon Nov 24 00:00:00 MST 2014
initialDayIndex.add(81445112);    // Tue Nov 25 00:00:00 MST 2014
initialDayIndex.add(82084507);    // Wed Nov 26 00:00:00 MST 2014


initialDayIndex.add(82724353);    // Tue Dec 02 00:00:00 MST 2014
initialDayIndex.add(83359033);    // Wed Dec 03 00:00:00 MST 2014
initialDayIndex.add(83995968);    // Thu Dec 04 00:00:00 MST 2014
initialDayIndex.add(84634051);    // Fri Dec 05 00:00:00 MST 2014
initialDayIndex.add(85268034);    // Mon Dec 08 00:00:00 MST 2014
initialDayIndex.add(85907017);    // Tue Dec 09 00:00:00 MST 2014
initialDayIndex.add(86547316);    // Wed Dec 10 00:00:00 MST 2014
initialDayIndex.add(87179823);    // Thu Dec 11 00:00:00 MST 2014
initialDayIndex.add(87812248);    // Fri Dec 12 00:00:00 MST 2014
initialDayIndex.add(88452673);    // Mon Dec 15 00:00:00 MST 2014
initialDayIndex.add(89088098);    // Tue Dec 16 00:00:00 MST 2014
initialDayIndex.add(89714197);    // Wed Dec 17 00:00:00 MST 2014
initialDayIndex.add(90356708);    // Thu Dec 18 00:00:00 MST 2014
initialDayIndex.add(90994996);    // Fri Dec 19 00:00:00 MST 2014

/*

finalDayIndex.add(130590);    // Mon Jan 06 00:00:00 MST 2014
finalDayIndex.add(262332);    // Tue Jan 07 00:00:00 MST 2014
finalDayIndex.add(392238);    // Wed Jan 08 00:00:00 MST 2014
finalDayIndex.add(523692);    // Thu Jan 09 00:00:00 MST 2014
finalDayIndex.add(655398);    // Fri Jan 10 00:00:00 MST 2014
finalDayIndex.add(785916);    // Mon Jan 13 00:00:00 MST 2014
finalDayIndex.add(917658);    // Tue Jan 14 00:00:00 MST 2014
finalDayIndex.add(1047528);    // Wed Jan 15 00:00:00 MST 2014
finalDayIndex.add(1179180);    // Thu Jan 16 00:00:00 MST 2014
finalDayIndex.add(1308744);    // Fri Jan 17 00:00:00 MST 2014
finalDayIndex.add(1438146);    // Tue Jan 21 00:00:00 MST 2014
finalDayIndex.add(1567620);    // Wed Jan 22 00:00:00 MST 2014
finalDayIndex.add(1698084);    // Thu Jan 23 00:00:00 MST 2014
finalDayIndex.add(1828836);    // Fri Jan 24 00:00:00 MST 2014
finalDayIndex.add(1958184);    // Mon Jan 27 00:00:00 MST 2014
finalDayIndex.add(2087748);    // Tue Jan 28 00:00:00 MST 2014
finalDayIndex.add(2217474);    // Wed Jan 29 00:00:00 MST 2014
finalDayIndex.add(2345508);    // Thu Jan 30 00:00:00 MST 2014
finalDayIndex.add(2474532);    // Fri Jan 31 00:00:00 MST 2014


finalDayIndex.add(2603160);    // Mon Feb 03 00:00:00 MST 2014
finalDayIndex.add(2733390);    // Tue Feb 04 00:00:00 MST 2014
finalDayIndex.add(2861946);    // Wed Feb 05 00:00:00 MST 2014
finalDayIndex.add(2992302);    // Thu Feb 06 00:00:00 MST 2014
finalDayIndex.add(3122334);    // Fri Feb 07 00:00:00 MST 2014
finalDayIndex.add(3252402);    // Mon Feb 10 00:00:00 MST 2014
finalDayIndex.add(3382866);    // Tue Feb 11 00:00:00 MST 2014
finalDayIndex.add(3513240);    // Wed Feb 12 00:00:00 MST 2014
finalDayIndex.add(3643398);    // Thu Feb 13 00:00:00 MST 2014
finalDayIndex.add(3772674);    // Fri Feb 14 00:00:00 MST 2014
finalDayIndex.add(3903210);    // Tue Feb 18 00:00:00 MST 2014
finalDayIndex.add(4031262);    // Wed Feb 19 00:00:00 MST 2014
finalDayIndex.add(4162410);    // Thu Feb 20 00:00:00 MST 2014
finalDayIndex.add(4293882);    // Fri Feb 21 00:00:00 MST 2014
finalDayIndex.add(4424256);    // Mon Feb 24 00:00:00 MST 2014
finalDayIndex.add(4554738);    // Tue Feb 25 00:00:00 MST 2014
finalDayIndex.add(4685958);    // Wed Feb 26 00:00:00 MST 2014
finalDayIndex.add(4815756);    // Thu Feb 27 00:00:00 MST 2014
finalDayIndex.add(4947480);    // Fri Feb 28 00:00:00 MST 2014


finalDayIndex.add(5076774);    // Mon Mar 03 00:00:00 MST 2014
finalDayIndex.add(5205870);    // Tue Mar 04 00:00:00 MST 2014
finalDayIndex.add(5335308);    // Wed Mar 05 00:00:00 MST 2014
finalDayIndex.add(5465700);    // Thu Mar 06 00:00:00 MST 2014
finalDayIndex.add(5595192);    // Fri Mar 07 00:00:00 MST 2014
finalDayIndex.add(5723784);    // Mon Mar 10 00:00:00 MDT 2014
finalDayIndex.add(5852160);    // Tue Mar 11 00:00:00 MDT 2014
finalDayIndex.add(5981526);    // Wed Mar 12 00:00:00 MDT 2014
finalDayIndex.add(6111720);    // Fri Mar 14 00:00:00 MDT 2014
finalDayIndex.add(6240852);    // Mon Mar 17 00:00:00 MDT 2014
finalDayIndex.add(6370182);    // Tue Mar 18 00:00:00 MDT 2014
finalDayIndex.add(6499692);    // Wed Mar 19 00:00:00 MDT 2014
finalDayIndex.add(6628086);    // Thu Mar 20 00:00:00 MDT 2014
finalDayIndex.add(6758298);    // Fri Mar 21 00:00:00 MDT 2014
finalDayIndex.add(6888888);    // Mon Mar 24 00:00:00 MDT 2014
finalDayIndex.add(7018128);    // Tue Mar 25 00:00:00 MDT 2014
finalDayIndex.add(7149078);    // Wed Mar 26 00:00:00 MDT 2014
finalDayIndex.add(7279362);    // Thu Mar 27 00:00:00 MDT 2014
finalDayIndex.add(7409214);    // Fri Mar 28 00:00:00 MDT 2014
finalDayIndex.add(7539210);    // Mon Mar 31 00:00:00 MDT 2014


finalDayIndex.add(7668252);    // Tue Apr 01 00:00:00 MDT 2014
finalDayIndex.add(7796754);    // Wed Apr 02 00:00:00 MDT 2014
finalDayIndex.add(7925364);    // Thu Apr 03 00:00:00 MDT 2014
finalDayIndex.add(8054100);    // Fri Apr 04 00:00:00 MDT 2014
finalDayIndex.add(8184825);    // Mon Apr 07 00:00:00 MDT 2014
finalDayIndex.add(8315431);    // Tue Apr 08 00:00:00 MDT 2014
finalDayIndex.add(8445410);    // Wed Apr 09 00:00:00 MDT 2014
finalDayIndex.add(8575370);    // Thu Apr 10 00:00:00 MDT 2014
finalDayIndex.add(8706546);    // Fri Apr 11 00:00:00 MDT 2014
finalDayIndex.add(8837418);    // Mon Apr 14 00:00:00 MDT 2014
finalDayIndex.add(8968746);    // Tue Apr 15 00:00:00 MDT 2014
finalDayIndex.add(9099400);    // Wed Apr 16 00:00:00 MDT 2014
finalDayIndex.add(9231440);    // Thu Apr 17 00:00:00 MDT 2014
finalDayIndex.add(9484757);    // Mon Apr 21 00:00:00 MDT 2014
finalDayIndex.add(9736355);    // Tue Apr 22 00:00:00 MDT 2014
finalDayIndex.add(9989758);    // Wed Apr 23 00:00:00 MDT 2014
finalDayIndex.add(10242629);    // Thu Apr 24 00:00:00 MDT 2014
finalDayIndex.add(10496887);    // Fri Apr 25 00:00:00 MDT 2014
finalDayIndex.add(10750138);    // Mon Apr 28 00:00:00 MDT 2014
finalDayIndex.add(11004909);    // Tue Apr 29 00:00:00 MDT 2014
finalDayIndex.add(11259224);    // Wed Apr 30 00:00:00 MDT 2014


finalDayIndex.add(11513463);    // Thu May 01 00:00:00 MDT 2014
finalDayIndex.add(11766790);    // Mon May 05 00:00:00 MDT 2014
finalDayIndex.add(12020307);    // Tue May 06 00:00:00 MDT 2014
finalDayIndex.add(12274432);    // Wed May 07 00:00:00 MDT 2014
finalDayIndex.add(12528652);    // Thu May 08 00:00:00 MDT 2014
finalDayIndex.add(12780820);    // Fri May 09 00:00:00 MDT 2014
finalDayIndex.add(13031031);    // Mon May 12 00:00:00 MDT 2014
finalDayIndex.add(13284225);    // Tue May 13 00:00:00 MDT 2014
finalDayIndex.add(13536778);    // Wed May 14 00:00:00 MDT 2014
finalDayIndex.add(13788922);    // Thu May 15 00:00:00 MDT 2014
finalDayIndex.add(14041774);    // Fri May 16 00:00:00 MDT 2014
finalDayIndex.add(14291719);    // Mon May 19 00:00:00 MDT 2014
finalDayIndex.add(14546642);    // Wed May 21 00:00:00 MDT 2014
finalDayIndex.add(14800881);    // Thu May 22 00:00:00 MDT 2014
finalDayIndex.add(15057685);    // Fri May 23 00:00:00 MDT 2014
finalDayIndex.add(15312057);    // Tue May 27 00:00:00 MDT 2014
finalDayIndex.add(15563617);    // Wed May 28 00:00:00 MDT 2014
finalDayIndex.add(15815918);    // Thu May 29 00:00:00 MDT 2014
finalDayIndex.add(16068238);    // Fri May 30 00:00:00 MDT 2014

finalDayIndex.add(16319627);    // Mon Jun 02 00:00:00 MDT 2014
finalDayIndex.add(16572118);    // Tue Jun 03 00:00:00 MDT 2014
finalDayIndex.add(16823583);    // Wed Jun 04 00:00:00 MDT 2014
finalDayIndex.add(17075409);    // Thu Jun 05 00:00:00 MDT 2014
finalDayIndex.add(17327235);    // Fri Jun 06 00:00:00 MDT 2014
finalDayIndex.add(17428486);    // Mon Jun 09 00:00:00 MDT 2014
finalDayIndex.add(17679989);    // Tue Jun 10 00:00:00 MDT 2014
finalDayIndex.add(17933715);    // Wed Jun 11 00:00:00 MDT 2014
finalDayIndex.add(18187061);    // Thu Jun 12 00:00:00 MDT 2014
finalDayIndex.add(18439476);    // Fri Jun 13 00:00:00 MDT 2014
finalDayIndex.add(18691454);    // Mon Jun 16 00:00:00 MDT 2014
finalDayIndex.add(18942539);    // Tue Jun 17 00:00:00 MDT 2014
finalDayIndex.add(19193225);    // Wed Jun 18 00:00:00 MDT 2014
finalDayIndex.add(19444538);    // Thu Jun 19 00:00:00 MDT 2014
finalDayIndex.add(19697162);    // Mon Jun 23 00:00:00 MDT 2014
finalDayIndex.add(19948399);    // Tue Jun 24 00:00:00 MDT 2014
finalDayIndex.add(20201650);    // Wed Jun 25 00:00:00 MDT 2014
finalDayIndex.add(20455224);    // Thu Jun 26 00:00:00 MDT 2014
finalDayIndex.add(20707734);    // Fri Jun 27 00:00:00 MDT 2014
finalDayIndex.add(20959921);    // Mon Jun 30 00:00:00 MDT 2014

*/

finalDayIndex.add(21212374);    // Tue Jul 01 00:00:00 MDT 2014
finalDayIndex.add(21464675);    // Wed Jul 02 00:00:00 MDT 2014
finalDayIndex.add(21961602);    // Wed Jul 09 00:00:00 MDT 2014
finalDayIndex.add(22388034);    // Thu Jul 10 00:00:00 MDT 2014
finalDayIndex.add(22985000);    // Fri Jul 11 00:00:00 MDT 2014
finalDayIndex.add(23715472);    // Mon Jul 14 00:00:00 MDT 2014
finalDayIndex.add(24334654);    // Tue Jul 15 00:00:00 MDT 2014
finalDayIndex.add(24945964);    // Wed Jul 16 00:00:00 MDT 2014
finalDayIndex.add(25548623);    // Thu Jul 17 00:00:00 MDT 2014
finalDayIndex.add(26147592);    // Fri Jul 18 00:00:00 MDT 2014
finalDayIndex.add(26748119);    // Mon Jul 21 00:00:00 MDT 2014
finalDayIndex.add(27357871);    // Tue Jul 22 00:00:00 MDT 2014
finalDayIndex.add(27967459);    // Wed Jul 23 00:00:00 MDT 2014
finalDayIndex.add(28579056);    // Thu Jul 24 00:00:00 MDT 2014
finalDayIndex.add(29187004);    // Fri Jul 25 00:00:00 MDT 2014
finalDayIndex.add(29787531);    // Mon Jul 28 00:00:00 MDT 2014
finalDayIndex.add(30389493);    // Tue Jul 29 00:00:00 MDT 2014
finalDayIndex.add(30998261);    // Wed Jul 30 00:00:00 MDT 2014
finalDayIndex.add(31604938);    // Thu Jul 31 00:00:00 MDT 2014

finalDayIndex.add(32203456);    // Fri Aug 01 00:00:00 MDT 2014
finalDayIndex.add(32809600);    // Mon Aug 04 00:00:00 MDT 2014
finalDayIndex.add(33427224);    // Tue Aug 05 00:00:00 MDT 2014
finalDayIndex.add(34048415);    // Wed Aug 06 00:00:00 MDT 2014
finalDayIndex.add(34678954);    // Thu Aug 07 00:00:00 MDT 2014
finalDayIndex.add(35304942);    // Fri Aug 08 00:00:00 MDT 2014
finalDayIndex.add(35930028);    // Mon Aug 11 00:00:00 MDT 2014
finalDayIndex.add(36549825);    // Tue Aug 12 00:00:00 MDT 2014
finalDayIndex.add(37169499);    // Wed Aug 13 00:00:00 MDT 2014
finalDayIndex.add(37774864);    // Thu Aug 14 00:00:00 MDT 2014
finalDayIndex.add(38395276);    // Fri Aug 15 00:00:00 MDT 2014
finalDayIndex.add(39024626);    // Mon Aug 18 00:00:00 MDT 2014
finalDayIndex.add(39651639);    // Tue Aug 19 00:00:00 MDT 2014
finalDayIndex.add(40303129);    // Wed Aug 20 00:00:00 MDT 2014
finalDayIndex.add(40960523);    // Thu Aug 21 00:00:00 MDT 2014
finalDayIndex.add(41615826);    // Fri Aug 22 00:00:00 MDT 2014
finalDayIndex.add(42266209);    // Mon Aug 25 00:00:00 MDT 2014
finalDayIndex.add(42917933);    // Tue Aug 26 00:00:00 MDT 2014
finalDayIndex.add(43555823);    // Wed Aug 27 00:00:00 MDT 2014
finalDayIndex.add(44196079);    // Thu Aug 28 00:00:00 MDT 2014
finalDayIndex.add(44835310);    // Fri Aug 29 00:00:00 MDT 2014

finalDayIndex.add(45467120);    // Tue Sep 02 00:00:00 MDT 2014
finalDayIndex.add(46124883);    // Wed Sep 03 00:00:00 MDT 2014
finalDayIndex.add(46755381);    // Thu Sep 04 00:00:00 MDT 2014
finalDayIndex.add(47375629);    // Fri Sep 05 00:00:00 MDT 2014
finalDayIndex.add(48010145);    // Mon Sep 08 00:00:00 MDT 2014
finalDayIndex.add(48645399);    // Tue Sep 09 00:00:00 MDT 2014
finalDayIndex.add(49255602);    // Wed Sep 10 00:00:00 MDT 2014
finalDayIndex.add(49888232);    // Thu Sep 11 00:00:00 MDT 2014
finalDayIndex.add(50525946);    // Fri Sep 12 00:00:00 MDT 2014
finalDayIndex.add(51156321);    // Mon Sep 15 00:00:00 MDT 2014
finalDayIndex.add(51780628);    // Tue Sep 16 00:00:00 MDT 2014
finalDayIndex.add(52388658);    // Wed Sep 17 00:00:00 MDT 2014
finalDayIndex.add(53018658);    // Thu Sep 18 00:00:00 MDT 2014
finalDayIndex.add(53653385);    // Fri Sep 19 00:00:00 MDT 2014
finalDayIndex.add(54280193);    // Mon Sep 22 00:00:00 MDT 2014
finalDayIndex.add(54912085);    // Tue Sep 23 00:00:00 MDT 2014
finalDayIndex.add(55542050);    // Wed Sep 24 00:00:00 MDT 2014
finalDayIndex.add(56171933);    // Thu Sep 25 00:00:00 MDT 2014
finalDayIndex.add(56809401);    // Fri Sep 26 00:00:00 MDT 2014
finalDayIndex.add(57431248);    // Mon Sep 29 00:00:00 MDT 2014
finalDayIndex.add(58066789);    // Tue Sep 30 00:00:00 MDT 2014

/*
finalDayIndex.add(58682000);    // Wed Oct 01 00:00:00 MDT 2014
finalDayIndex.add(59321717);    // Thu Oct 02 00:00:00 MDT 2014
finalDayIndex.add(59957434);    // Fri Oct 03 00:00:00 MDT 2014
finalDayIndex.add(60593865);    // Mon Oct 06 00:00:00 MDT 2014
finalDayIndex.add(61223010);    // Tue Oct 07 00:00:00 MDT 2014
finalDayIndex.add(61857772);    // Wed Oct 08 00:00:00 MDT 2014
finalDayIndex.add(62483145);    // Thu Oct 09 00:00:00 MDT 2014
finalDayIndex.add(63125287);    // Fri Oct 10 00:00:00 MDT 2014
finalDayIndex.add(63750729);    // Mon Oct 13 00:00:00 MDT 2014
finalDayIndex.add(64386447);    // Tue Oct 14 00:00:00 MDT 2014
finalDayIndex.add(65000996);    // Wed Oct 15 00:00:00 MDT 2014
finalDayIndex.add(65637029);    // Thu Oct 16 00:00:00 MDT 2014
finalDayIndex.add(66258671);    // Fri Oct 17 00:00:00 MDT 2014
finalDayIndex.add(66881871);    // Mon Oct 20 00:00:00 MDT 2014
finalDayIndex.add(67498142);    // Tue Oct 21 00:00:00 MDT 2014
finalDayIndex.add(68122777);    // Wed Oct 22 00:00:00 MDT 2014
finalDayIndex.add(68750412);    // Thu Oct 23 00:00:00 MDT 2014
finalDayIndex.add(69384962);    // Mon Oct 27 00:00:00 MDT 2014
finalDayIndex.add(69996354);    // Tue Oct 28 00:00:00 MDT 2014
finalDayIndex.add(70621317);    // Wed Oct 29 00:00:00 MDT 2014
finalDayIndex.add(71255013);    // Thu Oct 30 00:00:00 MDT 2014
finalDayIndex.add(71885709);    // Fri Oct 31 00:00:00 MDT 2014

finalDayIndex.add(72526956);    // Mon Nov 03 00:00:00 MST 2014
finalDayIndex.add(73164096);    // Wed Nov 05 00:00:00 MST 2014
finalDayIndex.add(73801605);    // Thu Nov 06 00:00:00 MST 2014
finalDayIndex.add(74432114);    // Fri Nov 07 00:00:00 MST 2014
finalDayIndex.add(75072318);    // Mon Nov 10 00:00:00 MST 2014
finalDayIndex.add(75698675);    // Tue Nov 11 00:00:00 MST 2014
finalDayIndex.add(76329501);    // Wed Nov 12 00:00:00 MST 2014
finalDayIndex.add(76967953);    // Thu Nov 13 00:00:00 MST 2014
finalDayIndex.add(77594228);    // Fri Nov 14 00:00:00 MST 2014
finalDayIndex.add(78231819);    // Mon Nov 17 00:00:00 MST 2014
finalDayIndex.add(78868508);    // Tue Nov 18 00:00:00 MST 2014
finalDayIndex.add(79517251);    // Wed Nov 19 00:00:00 MST 2014
finalDayIndex.add(80160090);    // Thu Nov 20 00:00:00 MST 2014
finalDayIndex.add(80808177);    // Fri Nov 21 00:00:00 MST 2014
finalDayIndex.add(81445112);    // Mon Nov 24 00:00:00 MST 2014
finalDayIndex.add(82084507);    // Tue Nov 25 00:00:00 MST 2014
finalDayIndex.add(82724353);    // Wed Nov 26 00:00:00 MST 2014

finalDayIndex.add(83359033);    // Tue Dec 02 00:00:00 MST 2014
finalDayIndex.add(83995968);    // Wed Dec 03 00:00:00 MST 2014
finalDayIndex.add(84634051);    // Thu Dec 04 00:00:00 MST 2014
finalDayIndex.add(85268034);    // Fri Dec 05 00:00:00 MST 2014
finalDayIndex.add(85907017);    // Mon Dec 08 00:00:00 MST 2014
finalDayIndex.add(86547316);    // Tue Dec 09 00:00:00 MST 2014
finalDayIndex.add(87179823);    // Wed Dec 10 00:00:00 MST 2014
finalDayIndex.add(87812248);    // Thu Dec 11 00:00:00 MST 2014
finalDayIndex.add(88452673);    // Fri Dec 12 00:00:00 MST 2014
finalDayIndex.add(89088098);    // Mon Dec 15 00:00:00 MST 2014
finalDayIndex.add(89714197);    // Tue Dec 16 00:00:00 MST 2014
finalDayIndex.add(90356708);    // Wed Dec 17 00:00:00 MST 2014
finalDayIndex.add(90994996);    // Thu Dec 18 00:00:00 MST 2014
finalDayIndex.add(91621927);    // Fri Dec 19 00:00:00 MST 2014

		
		

        stockList = populateStock();
		log.info("Stock List is " + stockList) ;
		
		/*
		for (Stock stock : stockList ) {
			log.info("Stock symbol is " + stock.symbol);
			log.info("Tradeable is " + stock.tradeable);
			log.info("Volume is " + stock.volume);
			log.info("Change is " + stock.change);
			log.info("Profit is " + stock.profit);
			log.info("ClosePosition is " + stock.closePosition);
			log.info("clearMean is " + stock.clearMean);
		}
		*/
		
		
		
		GenericAccountMgrStocksSimulation account = new GenericAccountMgrStocksSimulation(stockList, max_number_of_positions,capital_available);
		
		NewVolumeAverages v;
		v = NewVolumeAverages.readVolumeAverages();
		
		
		for (Stock1 stock : stockList ) {
			// set  average volume for each stock
			stock.setVolumeVector(v.symbolAverageVolume.get(stock.symbol));
			//log.info("Volume averages for symbol " + stock.symbol);
			//log.info(v.symbolAverageVolume.get(stock.symbol));
			// Instantiate strategy
			stock.instantiateStrategy(account);
		}
		
			
		
		
		Configuration configuration = new Configuration();
	       
        configuration.addEventType("StockTick", Level1Quote.class.getName());
        
        epService = EPServiceProviderManager.getProvider("StockSimulation", configuration);
        epService.initialize();
        
	    // Create the stock streams from  the global stream
        
        for (Stock1 stock : stockList ) {
        	String selectSymbol = "select * from StockTick(symbol='"+stock.symbol+"').win:length_batch("+windowSize+")";
        	EPStatement streamSymbol = epService.getEPAdministrator().createEPL(selectSymbol);
        	streamSymbol.addListener(new UpdateListener()
            {
                public void update(EventBean[] newEvents, EventBean[] oldEvents)
                {
                    // Send the stock quotes in this time window to the strategy              
                   
                    List<Level1Quote> QuoteList = new ArrayList<Level1Quote>();
                    Level1Quote lastQuote = null;
                    
                    
                    for (int i=0; i<newEvents.length; i++) {
                    	QuoteList.add((Level1Quote)newEvents[i].getUnderlying()); 

                    	if ( i == (newEvents.length -1)) {
                    		 lastQuote = (Level1Quote)newEvents[i].getUnderlying();
                    	}
                    }
                    GenericStockTraderMain2014.moveStrategyToState(lastQuote, QuoteList);               
                }
    	    });
        }	
        
        Date getDate1= null;
		GetFromStorageJDBC getRecord = new GetFromStorageJDBC();		
		// get connection
		Connection con = getRecord.establishConnection(); 
		if (con == null) {
			log.error("Cannot create a connection to the DB");
			return;
		}
		
		// overwrite the lot size based on the max capital available 
		double single_stock_capital = capital_available/(double) max_number_of_positions;
		
		for (Stock1 stock : stockList ) {
			
			String symb = stock.getSymbol();
			if (stock.getTradeable()) {
				
				// Get a quote to have a market price of the stock
				Level1Quote quote = new Level1Quote();
				int i = initialDayIndex.get(1) + 50000;
				quote = getRecord.getLevel1Quote(i,con,quote,symb,100);
				double price_bid = quote.getBid();
				log.info("price_bid is " + price_bid);
				int lot_size = (int) (single_stock_capital/((price_bid)));
				log.info("symb is " + symb + " lot size is " + lot_size);
				// update lot_size in the strategy
				stock.updateLot(lot_size);
				
			}
			
		}
		
		
		
		
		 for (int j =0; j < initialDayIndex.size(); j++ )  { 
			  
			    morningOpenIndex = initialDayIndex.get(j);
			    closeOpeningIndex = finalDayIndex.get(j);
			    String tradeDay= "Day"+(j+1);
			    n=1;
			    		    
			    for (int i = morningOpenIndex;i < closeOpeningIndex  ;i++ ) {
					
					
					// Use jdbc driver
					Level1Quote newQuote = new Level1Quote();
					newQuote = getRecord.getLevel1Quote(i, con, newQuote);	
					
					if (i >= (closeOpeningIndex - 1500)) {
						// Close all positions we are in the last 20 quotes
						for (Stock1 stock : stockList) {
							String symbol = newQuote.getSymbol();
							if (stock.getSymbol().contentEquals(symbol)) {
								stock.strategy.closePositions(newQuote);
								getDate1 =  newQuote.getCurrentDateTime();
								break;
							}
						}
						continue;
					}
					
					
					epService.getEPRuntime().sendEvent(newQuote);
					//check portfolio value every 1000 symbols approx 20 seconds
					
			    }	
			    account.analyzeTrades();
				allTrades.put(tradeDay, account.getTradeHistory());
				createTradingStats(getDate1,account.getTradeHistory());
				
				moveStrategiesToInitialState();
				totalTradeCost+=account.getTradeCost();
				account.resetAcctMgr();
				for (Stock1 stock : stockList) {
					stock.updateStrategy(false);
				}
				//moveStrategiesToInitialState();
				log.info(" Done with the day");
				portfolioLoss=0;
				dailyGain=DAILYGAIN;
				
				/*
				// calculate average volume of previous day
				if (j != (initialDayIndex.size() - 1) ) {
				    NewVolumeAverages v1 = new NewVolumeAverages();
				    NewVolumeAverages.createVolumeVector(morningOpenIndex,closeOpeningIndex , v1, true);
				    for (Stock1 stock : stockList ) {
					   stock.setVolumeVector(v1.symbolAverageVolume.get(stock.symbol));
					    stock.updateStrategy(true);
				    }					
				}
				*/
				
				if (j == initialDayIndex.size() - 1) {
					// this is the end so analyze all trades
					analyzeAllTrades();
				}
				n++;
				
				
		 }
		 getRecord.closeConnection();
	}
		
		
	
	
	public static List<Stock1> populateStock() {
		
		
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		Stock1 stock = null;
		List<Stock1> stockList = new ArrayList<Stock1>();
	    
		try {
			
			InputStream in = new FileInputStream("stock1.xml");
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
						
			while (eventReader.hasNext()) {
		        XMLEvent event = eventReader.nextEvent();
		        
		        if (event.isStartElement()) {
		            StartElement startElement = event.asStartElement();
		            // If we have an item element, we create a new item
		            if (startElement.getName().getLocalPart() == "stock") {
		            	 stock = new Stock1();
		              continue;
		            }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("symbol")) {
		        		event = eventReader.nextEvent();
		                stock.symbol = event.asCharacters().getData();
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("change")) {
		        		event = eventReader.nextEvent();
		                stock.change = Double.valueOf(event.asCharacters().getData());		                
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("profit")) {
		        		event = eventReader.nextEvent();
		                stock.profit = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("lot")) {
		        		event = eventReader.nextEvent();
		                stock.lot = Integer.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("volume")) {
		        		event = eventReader.nextEvent();
		                stock.volume = Integer.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("closePosition")) {
		        		event = eventReader.nextEvent();
		                stock.closePosition = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("tradeable")) {
		        		event = eventReader.nextEvent();
		                stock.tradeable = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("clearMean")) {
		        		event = eventReader.nextEvent();
		                stock.clearMean = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("bidAskSpread")) {
		        		event = eventReader.nextEvent();
		                stock.bid_ask_spread = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("impVol")) {
		        		event = eventReader.nextEvent();
		                stock.impVol = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("trend")) {
		        		event = eventReader.nextEvent();
		                stock.trend = Integer.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isEndElement()) {
		            EndElement endElement = event.asEndElement();
		            if (endElement.getName().getLocalPart() == "stock") {
		              stockList.add(stock);
		            }
		          }
		        
			}
		 
		} catch (FileNotFoundException e) {
	      e.printStackTrace();
		} catch (XMLStreamException e) {
		      e.printStackTrace();
	    }
			
		return stockList;
	}
	
	public static void  moveStrategyToState(Level1Quote quote, List<Level1Quote> QuoteArray) {
		
		String symbol = quote.getSymbol();
		//log.info("Received message from Esper");
		//log.info("symbol is " + symbol);
		//log.info("Quote is " + quote + " list is " + QuoteArray);
		for (Stock1 stock : stockList) {
			if (stock.getSymbol().contentEquals(symbol)) {
				stock.strategy.stateTransition(quote, prevQuote, QuoteArray);
				prevQuote = quote;
			}
		}
		

		
	}
	
	public static void  moveStrategiesToInitialState() {
		
		for (Stock1 stock : stockList) {			
			stock.strategy.setStateToS0();
			stock.strategy.resetPricesProfits();
			stock.strategy.clearMean();	
			stock.strategy.setTradeableFlag(stock.tradeable);
			stock.strategy.clearVWAP();
			stock.updateStrategy(false);
		}		
	}
	
	public static void analyzeAllTrades() {
		
		StockTradeRecord trade;
		int numberOfTrades=0;
		int numberOfWinningTrades =0;
		int numberofLoosingTrades=0;
		double currentGain=0D;
		double currentLoss=0D;
		double totalGain;
		
		log.info("                   ");
		
		
		if (!allTrades.isEmpty()) {
			
			//Iterator<Entry<String, LinkedList<TradeObject>>> it = allTrades.entrySet().iterator();
			
			Iterator<Entry<String, List<StockTradeRecord>>> it = allTrades.entrySet().iterator();
			
			 while (it.hasNext()) {
				 
			        Map.Entry<String, List<StockTradeRecord>> pairs = (Entry<String, List<StockTradeRecord>>)it.next();			        
			        Iterator<StockTradeRecord> iterator = pairs.getValue().iterator();	
			        
			        while (iterator.hasNext()){
						trade = (StockTradeRecord)iterator.next();
						numberOfTrades=numberOfTrades+1;
						if (trade.getGain()>0) {
							numberOfWinningTrades = numberOfWinningTrades + 1;
							currentGain=currentGain+trade.getGain();
						} else if (trade.getGain()<0) {
							numberofLoosingTrades = numberofLoosingTrades +1;
							currentLoss=currentLoss+trade.getGain();
						}
						
					}
			        
			 }			
			
		}
		

		totalGain = currentGain+currentLoss;
		log.info("Total for the simulation run *****************************START ");
		log.info("Total for the simulation run *****************************START ");
		log.info("Total for the simulation run *****************************START ");
		log.info("Number of Trades is: " + numberOfTrades);
		log.info("Number of Winning Trades is: " + numberOfWinningTrades+ " Percentage is: " + ((double)numberOfWinningTrades/numberOfTrades));
		log.info("Number of Loosing Trades is " + numberofLoosingTrades+ " Percentage is: " + ((double)numberofLoosingTrades/numberOfTrades) );
		log.info("Gain from the trades is " + currentGain);
		log.info("Loss from the trades is " + currentLoss);		
		log.info("Total Gain is: " + totalGain);
		log.info("Trade Cost is : " + totalTradeCost);
		log.info("Gain minus trade cost is: " + (totalGain - totalTradeCost) );
		log.info("Return on capital " + (totalGain-totalTradeCost)/(capital_available/4.0));
		log.info("Capital allocated " + (capital_available/4.0) + " Max number of positions " + max_number_of_positions + " strategy n is " + strategy_n);
		log.info("Total for the simulation run *****************************END ");
		log.info("Total for the simulation run *****************************END ");
		log.info("Total for the simulation run *****************************END ");
		log.info(" " );
		log.info(" " );
		analyze_trading_stats();
				
	}
	
	public static void createTradingStats(Date date, List<StockTradeRecord> a) {
		
		double gain = 0.0;
		
		for (StockTradeRecord s : a) {
			gain = gain + s.getGain() - s.getCost();
		}
		
		stats_trade.put(date, gain);
		
	}
	
	public static void analyze_trading_stats() {
		
		double max_loss = Double.MAX_VALUE;
		double max_gain = Double.MIN_VALUE;
		double sharpe_ratio=0;		
		Date loss_date = null;
		Date gain_date = null;
		DescriptiveStatistics m = new DescriptiveStatistics();
		
		
		for ( Entry<Date,Double> a : stats_trade.entrySet()) {
			
			log.info("Date is " + a.getKey() + " Gain for the day is " + a.getValue());
			if (a.getValue() < max_loss) {
				max_loss = a.getValue();
				loss_date = a.getKey();
			}
			
			if (a.getValue() > max_gain) {
				max_gain = a.getValue();
				gain_date = a.getKey();
			}
			double allocated_capital = capital_available/4.0;
			m.addValue(a.getValue()/allocated_capital);
			
		}
		
		sharpe_ratio = m.getMean()/m.getStandardDeviation();
		
		log.info(" Maximum Day Loss " + max_loss + " " + loss_date);
		log.info("Maximum Day Gain " + max_gain + " " + gain_date);
		log.info("Sharpe ratio is " + sharpe_ratio);
		
		
		
		
		
		
	}
	

}
