package com.weatronic.bluetoothtelemetry;

import android.util.Log;

import java.util.Comparator;

/**
 * A class containing a custom comparator.
 */
public class NumberAwareAlphabeticSort {
    /**
     * A comparator that properly compares string of different lengths that end with a number.
     * For example, a basic comparator returns Function9 > Function8 > Function 10, resulting in false order (F1, F10, F11, F12, F2, F3...).
     * This comparator separates the index at the end and compares the indices, if applicable.
     * Only handles the number at the end, but can be remade to search for first number from the end and recursively process equal strings.
     */
    static Comparator<String> stringComparator = new Comparator<String>(){

        public int compare(String s1, String s2) {
            //if both end with a number
            try{
                //initial check if last symbol is a number
                int n1 = Integer.parseInt(s1.substring(s1.length() - 1));
                int n2 = Integer.parseInt(s2.substring(s2.length() - 1));
                String sCut1 = s1, sCut2 = s2;
                //get number at the end of s1
                //iteratively add one symbol from the end and see if it is still a number
                for(int chr = s1.length()-1; chr >= 0; chr--){
                    try{
                        n1 = Integer.parseInt(s1.substring(chr));
                    }catch(Exception notNumber){
                        //cut off number and break loop
                        sCut1 = s1.substring(0, chr - 1);
                        break;
                    }
                }
                //get number at the end of s2
                for(int chr = s2.length()-1; chr >= 0; chr--){
                    try{
                        n2 = Integer.parseInt(s2.substring(chr));
                    }catch(Exception notNumber){
                        //cut off number and break loop
                        sCut2 = s2.substring(0, chr - 1);
                        break;
                    }
                }
                //no negative numbers please
                n1 = Math.abs(n1);
                n2 = Math.abs(n2);
                //if strings match except for number at the end/index
                if(sCut1.equals(sCut2)){
                    //index comparison
                    Log.e("", "Comparing " + s1 + " (" + n1 + ") with " + s2 + "(" + n2 + "). Basic comparison: " + s1.compareTo(s2));
                    return n1 - n2;
                }else{
                    //normal comparison
                    return s1.compareTo(s2);
                }
            }catch(Exception notNumber){
                //normal comparison
                return s1.compareTo(s2);
            }
        }

    };

}
