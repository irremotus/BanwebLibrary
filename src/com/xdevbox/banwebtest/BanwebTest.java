package com.xdevbox.banwebtest;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

import com.xdevbox.banweb.Banweb;

public class BanwebTest {

	@Test
	public void test() throws IOException {
		Banweb bw = new Banweb();
		boolean status = bw.login("username", "password");
		if (status) {
			System.out.println("Logged in");
		} else {
			System.out.println("Error logging in");
		}
		
		status = bw.clockInOut(null, null);
		if (status)
			System.out.println("Clocked in/out");
		else
			System.out.println("Failed to clock in/out");
		
		status = bw.logout();
		if (status)
			System.out.println("Logged out");
		else
			System.out.println("Error logging out");
	}

}
