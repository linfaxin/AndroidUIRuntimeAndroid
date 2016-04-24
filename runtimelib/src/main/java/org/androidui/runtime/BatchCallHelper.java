package org.androidui.runtime;

import android.util.Log;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by linfaxin on 16/4/3.
 * BatchCallHelper
 */
public class BatchCallHelper {
    protected final static String TAG = BatchCallHelper.class.getSimpleName();
    private final static boolean BATCH_CALL_TIME_DEBUG = false;

    private static HashMap<String, Method> batchCallMethodCache = new HashMap<>();
    private static ArrayList<String> CantSkipMethodNames = new ArrayList<>();
    public static void initBatchMethodCache(){
        Class<RuntimeBridge> c = RuntimeBridge.class;
        for (Method method : c.getDeclaredMethods()) {
            BatchMethod batchMethod = method.getAnnotation(BatchMethod.class);
            if(batchMethod!=null){
                batchCallMethodCache.put(batchMethod.value(), method);
                if(batchMethod.batchCantSkip()){
                    CantSkipMethodNames.add("\n"+batchMethod.value()+"[");
                }
            }
        }
    }

    public static boolean cantSkipBatchCall(String batchCallString){
        for(String s : CantSkipMethodNames){
            if(batchCallString.contains(s)) return true;
        }
        return false;
    }

    public static void parseAndRun(RuntimeBridge runtimeBridge, String jsonString){
        try {
            long time = System.nanoTime();
            long invokeUse = 0;
            long parseUse = 0;

            BufferedReader br = new BufferedReader(new StringReader(jsonString));
            long parseStart;
            String call;
            while(true){
                parseStart = System.nanoTime();
                call=br.readLine();
                if(call==null) break;

                int methodEndIndex = 2;//call.indexOf("[");
                String methodName = call.substring(0, methodEndIndex);

                String[] argss = call.substring(methodEndIndex+1, call.length()-1).split(",");
                Object[] args = new Object[argss.length];
                for(int i = 0, length=argss.length; i<length; i++){
                    String s = argss[i];
                    argss[i] = null;

                    if(s.charAt(0) == '\"') args[i] = URLDecoder.decode(s.substring(1, s.length() - 1));
                    else if(s.contains(".")) args[i] = Float.parseFloat(s);
                    else if(s.charAt(0) == 't') args[i] = true;
                    else if(s.charAt(0) == 'f') args[i] = false;
                    else{
                        long parseValue = Long.parseLong(s);
                        if(parseValue < Integer.MAX_VALUE) args[i] = (int)parseValue;
                        else args[i] = parseValue;
                    }
                }
                parseUse += (System.nanoTime() - parseStart);

                long invokeStart = System.nanoTime();
                try {
                    batchCallMethodCache.get(methodName).invoke(runtimeBridge, args);
                } catch (Exception e) {
                    Log.w(TAG, "method " + methodName + " invoke fail, args:" + Arrays.toString(args));
                    e.printStackTrace();
                }
                invokeUse += (System.nanoTime() - invokeStart);
            }

            if (BATCH_CALL_TIME_DEBUG){
                Log.d(TAG, "draw frame use :" + (System.nanoTime() - time) / 1000000f + "ms"
                        + ", invokeUse:" + invokeUse / 1000000f + "ms" + ", parseTime use :" + parseUse / 1000000f + "ms");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface BatchMethod{
        String value();
        boolean batchCantSkip() default false;
    }
}
