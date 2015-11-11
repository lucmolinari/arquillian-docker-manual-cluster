package com.test;

import javax.ejb.Stateless;

@Stateless
public class SimpleEJB {

    public void sayHello() {
        System.out.println("==>> Hello world");
    }
    
}