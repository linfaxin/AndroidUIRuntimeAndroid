package org.androidui.runtime;

import android.support.v4.util.Pools;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by linfaxin on 16/4/3.
 * BatchCallHelper
 */
public class BatchCallHelper {
    protected final static String TAG = BatchCallHelper.class.getSimpleName();
    public static boolean DEBUG_BATCH_CALL_TIME = false;

    private static final char ARG_BOOLEAN_FALSE = 'f';
    private static final char ARG_BOOLEAN_TRUE = 't';
    private static final char ARG_BOOLEAN_STRING = '"';

    private static HashMap<String, Method> batchCallMethodCache = new HashMap<>();
    private static ArrayList<String> CantSkipMethodNames = new ArrayList<>();

    public static void initBatchMethodCache(){
        Class<RuntimeBridge> c = RuntimeBridge.class;
        for (Method method : c.getDeclaredMethods()) {
            BatchMethod batchMethod = method.getAnnotation(BatchMethod.class);
            if(batchMethod!=null){
                batchCallMethodCache.put(batchMethod.value(), method);
                if(batchMethod.batchCantSkip()){
                    CantSkipMethodNames.add("\n\n"+batchMethod.value()+"\n");
                }
            }
        }
    }

    public static boolean cantSkipBatchCall(BatchCallHelper.BatchCallParseResult batchResult){
        return cantSkipBatchCall(batchResult.batchCallString);
    }
    public static boolean cantSkipBatchCall(String batchCallString){
        for(String s : CantSkipMethodNames){
            if(batchCallString.contains(s)) return true;
        }
        return false;
    }

    public static BatchCallParseResult parse(RuntimeBridge runtimeBridge, String batchCallString){
        try {
            BatchCallParseResult batchCallParseResult = BatchCallParseResult.obtain(runtimeBridge, batchCallString);

            int methodEndIndex = 2;//call.indexOf("["); //all method name now 10 - 99
            String methodName = null;
            Method method;
            long longValue;
            char tmpChar;
            ArrayList<Object> argList = new ArrayList<>();

            long parseStart = System.nanoTime();

            String[] lines = batchCallString.split("\n");

            for (String line : lines) {
                if(methodName == null){
                    methodName = line.substring(0, methodEndIndex);
                    continue;
                }

                if(line.length() == 0){//a call end
                    Object[] args = new Object[argList.size()];
                    argList.toArray(args);
                    argList.clear();

                    method = batchCallMethodCache.get(methodName);
                    if(method==null){
                        throw new RuntimeException("not found method: "+ methodName);
                    }
                    batchCallParseResult.add(method, args);
                    methodName = null;

                } else {
                    tmpChar = line.charAt(0);
                    if(tmpChar == ARG_BOOLEAN_FALSE){
                        argList.add(false);

                    }else if(tmpChar == ARG_BOOLEAN_TRUE){
                        argList.add(true);

                    }else if(tmpChar == ARG_BOOLEAN_STRING){
                        argList.add(line.substring(1, line.length() - 1).replaceAll("\\n", "\n"));

                    }else{
                        try {
                            longValue = fastParseLong(line);
                            if(longValue < Integer.MAX_VALUE){
                                argList.add((int)longValue);//int
                            }else{
                                argList.add(longValue);//long
                            }
                        } catch (Exception ignore) {
                            try {
                                float floatValue = Float.parseFloat(line);
                                argList.add(floatValue);//float
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            if (DEBUG_BATCH_CALL_TIME){
                Log.d(TAG, "parse batch call time use :" + (System.nanoTime()-parseStart) / 1000000f + "ms");
            }
            return batchCallParseResult;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static long fastParseLong( final String s ) {
        // Check for a sign.
        long num  = 0;
        int sign = -1;
        final int len  = s.length( );
        final char ch  = s.charAt( 0 );
        if ( ch == '-' )
            sign = 1;
        else
            num = '0' - ch;

        // Build the number.
        int i = 1;
        char tmpChar;
        while ( i < len ){
            tmpChar = s.charAt( i++ );
            if(tmpChar < '0' || tmpChar > '9') throw new NumberFormatException(s);
            num = num*10 + '0' - tmpChar;
        }

        return sign * num;
    }

    private static Pools.SynchronizedPool<BatchCallParseResult> BatchCallParseResultPools = new Pools.SynchronizedPool<>(20);
    public static class BatchCallParseResult implements Runnable{
        ArrayList<Method> methodList = new ArrayList<>();
        ArrayList<Object[]> argsList = new ArrayList<>();
        RuntimeBridge runtimeBridge;
        String batchCallString;

        private BatchCallParseResult() {
        }

        public void add(Method m, Object[] args){
            methodList.add(m);
            argsList.add(args);
        }

        @Override
        public void run() {
            long invokeUse = 0;
            long invokeStart = System.nanoTime();

            Method m;
            Object[] args;
            for (int i = 0, length = methodList.size(); i < length; i++) {
                m = methodList.get(i);
                args = argsList.get(i);

                try {
                    m.invoke(runtimeBridge, args);
                } catch (Exception e) {
                    Log.w(TAG, "method " + m.getName() + " invoke fail, args:" + Arrays.toString(args));
                    e.printStackTrace();
                }
            }

            invokeUse += (System.nanoTime() - invokeStart);
            if (DEBUG_BATCH_CALL_TIME){
                Log.d(TAG, "invokeUse batch call time use :" + invokeUse / 1000000f + "ms");
            }
            runtimeBridge.trackFPS();
        }

        public static BatchCallParseResult obtain(RuntimeBridge runtimeBridge, String batchCallString){
            BatchCallParseResult batchCallParseResult = BatchCallParseResultPools.acquire();
            if(batchCallParseResult==null){
                batchCallParseResult = new BatchCallParseResult();
            }
            batchCallParseResult.runtimeBridge = runtimeBridge;
            batchCallParseResult.batchCallString = batchCallString;
            return batchCallParseResult;
        }
        public void recycle(){
            methodList.clear();
            argsList.clear();
            this.runtimeBridge = null;
            this.batchCallString = null;
            BatchCallParseResultPools.release(this);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface BatchMethod{
        String value();
        boolean batchCantSkip() default false;
    }
}
