package com.hys.common.utils.pay.mzf;

public class Test {

    public static void main(String[] args) {
        Test.testStackOutOfMemory();
    }

    public static void testStackOutOfMemory(){
        while (true) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    while(true){
                    }
                }
            });
            thread.start();
        }
    }


}
