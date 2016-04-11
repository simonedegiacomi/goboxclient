package it.simonedegiacomi.storage.utils;

import com.google.common.collect.Range;

/**
 * Created by simone on 11/04/16.
 */
public class MyRange {

    public static Range parse (String rangeStr) {
        String pieces[] = rangeStr.split("/");
        long low = Long.parseLong(pieces[0]);
        long high = Long.parseLong(pieces[1].substring(0, rangeStr.indexOf('/')));
        return Range.open(low, high);
    }
}
