

package com.zhuoan.util;

import java.util.Iterator;
import java.util.Set;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONArrayUtil {
    private static final Logger log = LoggerFactory.getLogger(JSONArrayUtil.class);

    public JSONArrayUtil() {
    }

    public static JSONArray toJSONArray(Set set) {
        JSONArray array = new JSONArray();
        Iterator var2 = set.iterator();

        while(var2.hasNext()) {
            Object obj = var2.next();
            array.add(obj);
        }

        return array;
    }
}
