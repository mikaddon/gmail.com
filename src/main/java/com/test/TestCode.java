package com.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/*
Given a collection of 5-digit ZIP code ranges (each range includes both their upper and lower bounds),
 provide an algorithm that produces the minimum number of ranges required to represent the same restrictions 
 as the input.
*/
public class TestCode {
    private Map<String, Set<Long>> mapZipRanges;//Hold final range of zip codes
    public static void main(String[] args) {
         new TestCode().calculateRangeLimits();
    }
/*
    Initiate the workflow
    Read Zip code input from text file.
    Process every zip range on file next
    Print out the result

*/
    public void calculateRangeLimits(){
        mapZipRanges = Collections.synchronizedMap(new HashMap<>());
        List<String> lstRangesZpCodes = readZipCodes();
        lstRangesZpCodes.stream().forEach(this::calculateZipRange);
        //Prin out the expected output. The separator used is <->
        /*
            If the input = [94133,94133] [94200,94299] [94600,94699]
            Then the output should be = [94133,94133] [94200,94299] [94600,94699]

            If the input = [94133,94133] [94200,94299] [94226,94399]
            Then the output should be = [94133,94133] [94200,94399]
        */
        mapZipRanges.forEach((k, v) -> System.out.println(k));
    }

    /*
        Giving a zip code range 94600<->94699, we process base on lower limit
        Take initial range to mapZipRanges
        Next calculate if any provide range can be assigned to 
        1. a new range inside the output
        2. It is already in one of the current ranges
        3. The Upper limit can replace existing ones e.g. 94200<->94299 transforms to 94200<->94399
    */
    private void calculateZipRange(String zips){
        long lowerLimit = 0;
        long upperLimit = 0;
        String[] arrZips=null;

        arrZips = zips.split(",");
        Set<String> setZips = Arrays.asList(arrZips).stream()
        .map(e -> e.trim())
        .sorted()
        .collect(Collectors.toSet());
        
        lowerLimit = Long.valueOf(setZips.toArray(new String[]{})[0]);
        upperLimit = Long.valueOf(setZips.toArray(new String[]{})[setZips.size()-1]); 
            
        if(mapZipRanges == null || mapZipRanges.size() == 0){
            mapZipRanges.put(lowerLimit+"<->"+upperLimit, new HashSet<Long>());
        }else{
            insideRanks(zips);
        }  
    }
    /*
    To avoid concurrent issues we use a clone of the current stored mapZipRanges
    */
    private Map<String, Set<Long>> cloneMap(){
        Map<String, Set<Long>> cloneMapZipRanges = new HashMap<>();
        Iterator<String> it = mapZipRanges.keySet().iterator();
        String key = "";
        while(it.hasNext()){
            key = it.next();
            cloneMapZipRanges.put(key, mapZipRanges.get(key));
        }
        return cloneMapZipRanges;
    }
    /*
    Calculate ranges base on
        1. a new range inside the output
        2. It is already in one of the current ranges
        3. The Upper limit can replace existing ones e.g. 94200<->94299 transforms to 94200<->94399
  
    */
    private void insideRanks(String arrVals){
        String[] arrRanges = arrVals.split(",");
        Map<String, Set<Long>> tempMapZipRanges = null;
        Iterator<String> it = null;
        boolean bolFnd = false;
        long lastVal = 0;
        for(String value: arrRanges){
            tempMapZipRanges = cloneMap();
            it = tempMapZipRanges.keySet().iterator();
            bolFnd = false;
            Long val = Long.parseLong(value.trim());
            while(it.hasNext() && !bolFnd){
                String k = it.next();
                if(k.indexOf("<->") > -1){
                    long lowerLimit = Long.parseLong(k.substring(0, k.indexOf("<->")).trim());
                Long upperLimit = Long.parseLong(k.substring(k.indexOf("<->")+3).trim());
                //System.out.println("lowerLimit ->"+lowerLimit+" upperLimit "+upperLimit + " val "+val);
                lastVal = -1;
                if( val > lowerLimit && upperLimit == -1){
                    try{
                  //      System.out.println("Updating a rank for "+k);
                        mapZipRanges.put(lowerLimit+"<->"+val, new HashSet<>());
                        mapZipRanges.remove(k);
                        bolFnd = true;
                    } catch(Exception e){
                        e.printStackTrace();
                    } 
                }else if(val > lowerLimit && val > upperLimit){
                    String scanVal = calculateRank(val);
                    lastVal = val;
                    if(scanVal == null){
                    //    System.out.println("Creating a rank for "+val);
                        mapZipRanges.put(val+"<->"+(-1), new HashSet<>());
                    }
                    bolFnd = true;
                }
                }
            }
        }    
        if(lastVal != -1){
            String scanVal = calculateUpperRange(lastVal);
            if(scanVal != null && !scanVal.trim().equals("")){
                mapZipRanges.remove(scanVal);
                mapZipRanges.put(scanVal.substring(0, scanVal.indexOf("<->")+3)+lastVal, new HashSet<>());
            }        
        }
        //Delete -1 (Dummy values which are not on any range)
        String key = "";
        it = mapZipRanges.keySet().iterator();
        Set<String> setKeysDel = new HashSet<String>();
        while(it.hasNext()){
            key = it.next();
            //System.out.println(key+" SUBSTRING "+key.substring(key.indexOf("<->")+5).trim());
            if(key.indexOf("<->-1") > -1){
                setKeysDel.add(key);
            }
        } 
        setKeysDel.stream().forEach(el ->{
            mapZipRanges.remove(el);
        });
    }
    /*
    Calculate last Upper limit that replace existing ones e.g. 94200<->94299 transforms to 94200<->94399
  
    */
    private String calculateUpperRange(Long keySearch){
        Iterator<String>it = mapZipRanges.keySet().iterator();
        String key = "";
        long lowerLimit = 0;
        long upperLimit = 0;
        String lastRange = null;
        while(it.hasNext()){
            key = it.next();
            //System.out.println("=>"+key+" keySearch "+keySearch);
            //System.out.println(key+" SUBSTRING "+key.substring(key.indexOf("<->")+5).trim());
            if(key.indexOf("<->") > -1){
                lowerLimit = Long.valueOf(key.substring(0, key.indexOf("<->")).trim());
                upperLimit = Long.valueOf(key.substring(key.indexOf("<->")+3).trim());
            /* if the input = [94133,94133] [94200,94299] [94226,94399]
            Then the output should be = [94133,94133] [94200,94399]
            */
 
            if(upperLimit!= -1){
                 if(keySearch >= lowerLimit){
                     if(keySearch >= upperLimit){
                         lastRange = key;
                         //System.out.println("lastRange ->"+lastRange);
                     }
                    }
                }
            }
        }
        return lastRange;
          
    }
    /*
        !. VErify if a given zip code is inside a current range of zip codes
        2. If  not tell caller create a new RANGE
        3. If yes not use that zip code because it is already in our current zip ranges
    */
    private String calculateRank(Long keySearch){
        Iterator<String>it = mapZipRanges.keySet().iterator();
        String key = "";
        long lowerLimit = 0;
        long upperLimit = 0;
        List<String> lstSlots = new ArrayList<String>();
        int idx = -1;
        String lastRange = "";
        while(it.hasNext()){
            key = it.next();
            if(key.indexOf("<->") > -1){
                lowerLimit = Long.valueOf(key.substring(0, key.indexOf("<->")).trim());
            upperLimit = Long.valueOf(key.substring(key.indexOf("<->")+3).trim());
            /* if the input = [94133,94133] [94200,94299] [94226,94399]
            Then the output should be = [94133,94133] [94200,94399]
            */
 
            if(upperLimit!= -1){
                if(lowerLimit >= keySearch){
                    if(upperLimit <= keySearch){
                        idx++;
                        lstSlots.add(idx, key);
                    }else if(upperLimit > keySearch){
                        idx++;
                        lstSlots.add(idx, key);
                    }
                 } 
                }
            }
        }
          
        if(lstSlots != null && lstSlots.size() >0){
            return lstSlots.get(lstSlots.size() - 1);
        }

        return null;
    }
    /*
    Read from file the user input. That way there is not limit on the provided Zip Ranges
    [94133,94133] [94200,94299] [94600,94699]
    translate in our file

    94133,94133
    94200,94299 
    94600,94699

    [94133,94133] [94200,94299] [94226,94399]

    translate in our file 

    94133,94133
    94200,94299 
    94226,94399

    The text file is read base on the class path. The class loader search for the default locations.
    */
    private List<String> readZipCodes(){
        List<String> lstRangesZpCodes = new ArrayList<>();
        File file = null;    //creates a new file instance  
        FileReader fr = null;   //reads the file  
        BufferedReader br = null;  //creates a buffering character input stream  
        
        try {  
            File path = new File(".");
            file = new File("./zipCodes.txt");    //creates a new file instance  
            fr = new FileReader(file);   //reads the file  
            br = new BufferedReader(fr);  //creates a buffering character input stream  
            String line;  
            while((line=br.readLine())!=null)  
            {  
                lstRangesZpCodes.add(line);
            }  
            fr.close();    
        }  
            catch(IOException e)  
        {  
            e.printStackTrace();  
        }  finally {
            file = null;
            br = null;
            fr = null;
        }
        return lstRangesZpCodes;
    }
}