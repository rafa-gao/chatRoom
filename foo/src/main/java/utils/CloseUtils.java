package utils;

import java.io.Closeable;
import java.io.IOException;

/*
 * @author rafa gao
 */
public class CloseUtils {

    /*
    * 关闭方法
    *
    * */
    public static void close(Closeable... closeables){
        if (closeables.length==0||closeables==null){
            return;
        }

        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("close Exception");
            }
        }
    }
}
