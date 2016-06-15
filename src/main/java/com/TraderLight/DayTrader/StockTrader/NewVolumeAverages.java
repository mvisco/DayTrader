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

package com.TraderLight.DayTrader.StockTrader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This class reads the volume average on a minute base. For each symbol the minute averages are stored in a list of int
 * that has a length of 380.
 * 
 * @author Mario Visco
 * 
 */

public class NewVolumeAverages implements Serializable {
	
	public static final Logger log = Logging.getLogger(true);
	private static final long serialVersionUID = 3939430547766987030L;
	public List<Integer> averageVolume ;
	public  Map<String,List<Integer>> symbolAverageVolume= new HashMap<String,List<Integer>>();
	public List<String> stockList = new ArrayList<String>();
		
	/**
	 * This methods builds and returns the volume averages on a minute base.
	 * 
	 * @author Mario Visco
	 * 
	 */
	
	
	  public  static NewVolumeAverages readVolumeAverages() {
		  
	      {
		         NewVolumeAverages v1 = new NewVolumeAverages(); //deserialized object
		         
			 try
	         {     				
	    	     FileInputStream fileIn = new FileInputStream("volume.ser");
		         ObjectInputStream in = new ObjectInputStream(fileIn);		         		         
		         v1 = (NewVolumeAverages) in.readObject();
		         in.close();
		         fileIn.close();
		      }catch(IOException i)
		      {
		         i.printStackTrace();
		         return null;
		      }catch(ClassNotFoundException c)
		      {
		         System.out.println("Volume class not found");
		         c.printStackTrace();
		         return null;
		      }		      
			  
		       return v1;
		  
	  }

     }
}
