
package com.zhuoan.util;

import java.util.regex.Pattern;

public class RegexUtil {
    public RegexUtil() {
    }

    public static boolean isPlusInt(String s) {
        return s == null ? false : Pattern.compile("[1-9]\\d*").matcher(s).matches();
    }
}
