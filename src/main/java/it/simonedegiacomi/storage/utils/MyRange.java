package it.simonedegiacomi.storage.utils;

import com.google.common.collect.Range;

/**
 * Created on 11/04/16.
 * @author Degiacomi Simone
 */
public class MyRange {

    public static Range parse (String rangeStr) {
        String pieces[] = rangeStr.substring(rangeStr.indexOf("=") + 1).split("-");
        long low = Long.parseLong(pieces[0]);
        long high = pieces.length == 2 ? Long.parseLong(pieces[1]) : Long.MAX_VALUE;
        return Range.open(low, high);
    }
}
