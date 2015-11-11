package com.test;

import javax.ejb.Stateless;

@Stateless
public class SimpleEJB {

	public String sayHello() {
		return "Hello world";
	}

}