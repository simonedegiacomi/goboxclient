package goboxapi.utils;

import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

/**
 * Created by Degiacomi Simone on 06/01/16.
 */
public class URLParams {

    public static URL createURL (String stringUrl, JSONObject params) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append(stringUrl);
        boolean first = true;
        Iterator<String> it = params.keys();
        while(it.hasNext()) {
            String key = it.next();
            if (first) {
                builder.append('?');
                first = !first;
            } else
                builder.append('&');
            builder.append(key);
            builder.append('=');
            builder.append(params.get(key).toString());
        }
        return new URL(URLEncoder.encode(builder.toString(), "UTF-8"));
    }

    public static URL createURL (URL url, JSONObject params) throws Exception {
        return createURL(url.toString(), params);
    }
}