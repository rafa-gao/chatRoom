package utils;

/*
 * @author rafa gao
 */
public class ByteUtils {

    /*
    *
    * @param comparedArray 被比较的byte数组
    * @param buf 用来比较的数组
    * */
    public static boolean startsWith(byte[] comparedArray ,byte[] buf){
        if (comparedArray.length<buf.length||comparedArray.length==0){
            return false;
        }

        int length = buf.length;
        for(int i=0;i<length;i++){
            if (comparedArray[i]!=buf[i]){
                return false;
            }
        }

        return true;
    }



}
