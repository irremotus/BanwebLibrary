package com.xdevbox.banweb;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by Kevin on 5/3/2015.
 */
public class Banweb {
    private static String base_url = "https://banweb7.nmt.edu/pls/PROD/";

    private Map<String, String> cookies = new HashMap<String, String>();
    private Map<String, String> getCookies() {
    	return cookies;
    }
    private void setCookies(Map<String, String> cookies) {
    	if (cookies != null)
    		this.cookies = cookies;
    }

    public Banweb() {
    }

    public boolean login(String user, String pass) {
        String loginposturl = base_url + "twbkwbis.P_ValLogin";
        String loginpageurl = base_url + "twbkwbis.P_WWWLogin";

        try {
            Connection.Response loginpage = Jsoup.connect(loginpageurl)
            		.method(Connection.Method.GET)
            		.timeout(0)
            		.execute();
            setCookies(loginpage.cookies());
            Connection.Response loginpost = Jsoup.connect(loginposturl)
            		.method(Connection.Method.POST)
                    .timeout(0)
            		.data("sid", user)
                    .data("PIN", pass).cookies(cookies).execute();
            setCookies(loginpost.cookies());
            if (loginpost.body().contains("Welcome"))
                return true;
        } catch(IOException e) {
        	e.printStackTrace();
        	return false;
        }

        return false;
    }
    
    public boolean logout() {
    	String logoutpageurl = base_url + "twbkwbis.P_Logout";
    	try {
			Jsoup.connect(logoutpageurl).cookies(cookies)
				.timeout(0).get();
		} catch (IOException e) {
			return false;
		}
    	return true;
    }
    
    
    /* check each pay period element for whether the current date is in that date range */
    public class PayPeriod {
    	private String value;
    	private String text;
    	
    	public PayPeriod(String value, String text) {
    		this.value = value;
    		this.text = text;
    	}
    	
    	public String getValue() {
    		return value;
    	}
    	
    	public String getText() {
    		return text;
    	}
    }
    
    public Element[] getPayPeriodElements() {
    	String payperiodpageurl = base_url + "bwpktais.P_SelectTimeSheetRoll";
    	Connection.Response payperiodpage = null;
    	Element[] ppes = new Element[2];
    	
    	try {
			payperiodpage = Jsoup.connect(payperiodpageurl)
					.method(Connection.Method.GET)
					.timeout(0)
					.cookies(cookies).execute();
		} catch (IOException e) {
			return null;
		}
    	
    	setCookies(payperiodpage.cookies());
		Document doc;
		try {
			doc = payperiodpage.parse();
		} catch (IOException e1) {
			return null;
		}
		
		Calendar today = Calendar.getInstance();
		DateFormat df = new SimpleDateFormat("MMM dd, yyyy");
		Elements options = doc.select("option");
		for (Element el : options) {
			Calendar d1 = Calendar.getInstance();
			Calendar d2 = Calendar.getInstance();
			String[] parts = el.text().split(" to ");
			try {
				d1.setTime(df.parse(parts[0]));
				d2.setTime(df.parse(parts[1]));
			} catch (ParseException e) {
				continue;
			}
			if (today.compareTo(d1) > 0 && today.compareTo(d2) < 0) {
				ppes[0] = el;
				/* get the associated job */
				ppes[1] = el.parent().parent().previousElementSibling()
						.previousElementSibling().previousElementSibling().child(0);
				return ppes;
			}
		}
    	
		return null;
    }
    
    public boolean clockInOut(Calendar cal, String message) {
    	Document doc = null;
    	Element[] ppes = getPayPeriodElements(); /* pay period elements */
    	if (ppes == null || ppes.length != 2) {
    		System.err.println("Did not receive pay period elements");
    		return false;
    	}
		Element ppe = ppes[0]; /* pay period element */
		Element je = ppes[1];  /* job element */
		Connection.Response res;
		
		/* pick the pay period */
		try {
			res = Jsoup.connect(base_url + "bwpkteis.P_SelectTimeSheetDriver")
					.data("Jobs", je.attr("value"))
					.data("PayPeriod", ppe.attr("value"))
					.cookies(cookies)
					.method(Connection.Method.POST)
					.timeout(0)
					.execute();
			doc = res.parse();
		} catch (IOException e) {
			System.err.println("Could not select pay period");
			e.printStackTrace();
			return false;
		}
		setCookies(res.cookies());
		
		/* hit the clockInOut button */
		Element btnClockInOut = doc.select("a[href*=ShowClockInOut]").first();
		String[] clockInOutUrl = btnClockInOut.attr("href").split("/");
		try {
			res = Jsoup.connect(base_url + clockInOutUrl[clockInOutUrl.length - 1])
					.method(Connection.Method.GET)
					.timeout(0)
					.cookies(cookies).execute();
			doc = res.parse();
		} catch (IOException e) {
			System.err.println("Could not hit the clock button");
			return false;
		}
		setCookies(res.cookies());
		
		/* update form fields and submit the form */
		Map<String, String> formData = new HashMap();
		Elements formHiddenInputs = doc.select("form[name=frmclock] input[type=hidden]"); /* hidden inputs */
		for (Element el : formHiddenInputs) {
			formData.put(el.attr("name"), el.attr("value"));
			//System.err.println(el.attr("name") + ": " + el.attr("value"));
		}
		Elements formInputs = doc.select("form[name=frmclock] input[type=text]");
		/* TODO: modify form inputs here, and get comment textarea here */
		for (Element el : formInputs) {
			formData.put(el.attr("name"), el.attr("value"));
			//System.err.println(el.attr("name") + ": " + el.attr("value"));
		}
		Elements formTextAreas = doc.select("form[name=frmclock] textarea");
		/* TODO: modify form inputs here, and get comment textarea here */
		for (Element el : formTextAreas) {
			formData.put(el.attr("name"), el.text());
			//System.err.println(el.attr("name") + ": " + el.text());
		}
		formData.put("ButtonSelected", "Save");
		try {
			res = Jsoup.connect(base_url + "bwpktclk.P_UpdateClockInOut").method(Connection.Method.POST)
					.data(formData).cookies(cookies)
					.referrer("https://banweb7.nmt.edu/pls/PROD/bwpktclk.P_UpdateClockInOut")
					.userAgent("Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36")
					.header("Connection", "keep-alive")
					.header("Origin", "https://banweb7.nmt.edu")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.timeout(0)
					.execute();
		} catch (IOException e) {
			System.err.println("Could not save timesheet");
			e.printStackTrace();
			return false;
		}
    	
    	return true;
    }


}
