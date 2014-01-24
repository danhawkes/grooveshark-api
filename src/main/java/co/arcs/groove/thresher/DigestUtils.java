package co.arcs.groove.thresher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

public class DigestUtils {

	public static String md5Hex(String s) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] digest = md5.digest(s.getBytes(Charsets.UTF_8));
			return BaseEncoding.base16().lowerCase().encode(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String shaHex(String s) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("SHA1");
			byte[] digest = md5.digest(s.getBytes(Charsets.UTF_8));
			return BaseEncoding.base16().lowerCase().encode(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
