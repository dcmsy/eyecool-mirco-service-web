package com.eyecool.service.web.test;

import org.openjdk.jol.info.ClassLayout;

public class Test {
    public static void main(String[] args) {
        MyTest myTest = new MyTest();
        System.out.println("加锁前:"+myTest);
        System.out.println(ClassLayout.parseInstance(myTest).toPrintable());
        synchronized (myTest){
            System.out.println(ClassLayout.parseInstance(myTest).toPrintable());
            System.out.println("加锁中:"+myTest);
        }
        System.out.println(ClassLayout.parseInstance(myTest).toPrintable());
        System.out.println("加锁后:"+myTest);
    }
}
