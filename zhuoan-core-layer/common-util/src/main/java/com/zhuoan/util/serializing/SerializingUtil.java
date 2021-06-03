
package com.zhuoan.util.serializing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializingUtil {
    private static final Logger logger = LoggerFactory.getLogger(SerializingUtil.class);

    public SerializingUtil() {
    }

    public static byte[] serialize(Object source) {
        ByteArrayOutputStream byteOut = null;
        ObjectOutputStream ObjOut = null;

        try {
            byteOut = new ByteArrayOutputStream();
            ObjOut = new ObjectOutputStream(byteOut);
            ObjOut.writeObject(source);
            ObjOut.flush();
        } catch (IOException var12) {
            logger.error(source.getClass().getName() + " serialized error !", var12);
        } finally {
            try {
                if (null != ObjOut) {
                    ObjOut.close();
                }
            } catch (IOException var11) {
                ObjOut = null;
            }

        }

        return byteOut.toByteArray();
    }

    public static Object deserialize(byte[] source) {
        ObjectInputStream ObjIn = null;
        Object retVal = null;

        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(source);
            ObjIn = new ObjectInputStream(byteIn);
            retVal = ObjIn.readObject();
        } catch (Exception var12) {
            logger.error("deserialized error  !", var12);
        } finally {
            try {
                if (null != ObjIn) {
                    ObjIn.close();
                }
            } catch (IOException var11) {
                ObjIn = null;
            }

        }

        return retVal;
    }

    public static String objectSerializable(Object obj) {
        String serStr = null;

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            serStr = byteArrayOutputStream.toString("ISO-8859-1");
            serStr = URLEncoder.encode(serStr, "UTF-8");
            objectOutputStream.close();
            byteArrayOutputStream.close();
        } catch (UnsupportedEncodingException var4) {
            var4.printStackTrace();
        } catch (IOException var5) {
            var5.printStackTrace();
        }

        return serStr;
    }

    public static Object objectDeserialization(String serStr) {
        Object newObj = null;

        try {
            String redStr = URLDecoder.decode(serStr, "UTF-8");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(redStr.getBytes("ISO-8859-1"));
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            newObj = objectInputStream.readObject();
            objectInputStream.close();
            byteArrayInputStream.close();
        } catch (UnsupportedEncodingException var5) {
            var5.printStackTrace();
        } catch (ClassNotFoundException var6) {
            var6.printStackTrace();
        } catch (IOException var7) {
            var7.printStackTrace();
        }

        return newObj;
    }
}
