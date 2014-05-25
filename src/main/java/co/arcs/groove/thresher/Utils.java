package co.arcs.groove.thresher;

import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;

class Utils {

    static String randHexChars(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[(length / 2) + (length % 2)];
        random.nextBytes(bytes);
        String s = BaseEncoding.base16().lowerCase().encode(bytes);
        return ((length % 2) == 0) ? s : s.substring(0, length);
    }
}
