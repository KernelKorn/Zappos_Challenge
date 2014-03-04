/**
 * This application will prompt the user for their email address and for 
 * the productID or productName. The application will then connect to the 
 * Zappos API and check to see if the product is at least 20% off then email
 * the user the product is on sale. If the product is less than 20% off or is 
 * not on sale the application will check every 24 hours to see if the product 
 * has gone on sale for at least 20% and will email the user. The email is sent
 * from a Gmail account using the Gmail servers. 
 * 
 * @author Ryan Corn
 * @date Feb. 25, 2014
 */

import java.io.*;
import java.net.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public class sendEmail {

	private static String USER_NAME = "testjavaemail31"; // gmail user name (just the part before "@gmail.com")
	private static String PASSWORD = "javaemail25"; // gmail password
	public static String RECIPIENT = "";
	private static String ZAPPOS_API = "http://api.zappos.com/Search/term/"; // The Zappos API call
	private static String ZAPPOS_API_KEY = "?key=52ddafbe3ee659bad97fcce7c53592916a6bfd73"; // The Key to access the Zappos API
	public static String SKU = "";
	public static String PRODUCTNAME;
	public static String PRODUCTURL;

	public static void main(String[] args) {
		
		userInterface();
		
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				String from = USER_NAME;
				String pass = PASSWORD;
				String subject = "Zappos item is on Discount";
				String[] to = { RECIPIENT }; // list of recipient email addresses
				String body = "";
				int discount;
				
				discount = retrieveDiscount(SKU);
				
				if (discount >= 20) {
					body = "Great News! The item you requested (" + PRODUCTNAME + ") is now "
							+ discount + "% off! Visit at: " + PRODUCTURL;
					sendFromGmail(from, pass, to, subject, body);
					timer.cancel();
				}
			}
		}, 0, 24 * 60 * 60 * 1000); // 24 hour timer
	} // end main
	
/*
 * This function prompts the user for their email address and the 
 * prductName / productID. It then passes the values to the main function.
 */
	public static void userInterface() {
		JTextField email = new JTextField(15);
		JTextField product = new JTextField(10);

		JPanel myPanel = new JPanel();
		myPanel.add(new JLabel("Email"));
		myPanel.add(email);
		myPanel.add(Box.createHorizontalStrut(15));
		myPanel.add(new JLabel("ProductId/ProductName"));
		myPanel.add(product);

		int result = JOptionPane.showConfirmDialog(null, myPanel,
				"Please Enter Email Address and Product Name or Product Id",
				JOptionPane.OK_CANCEL_OPTION);
		
		if (result == JOptionPane.OK_OPTION) {
			RECIPIENT = email.getText();
			SKU = product.getText().replace(" ", "");
		}
		else {
			System.exit(0);
		}
	} // end userInterface
/*
 * Connects to the Zappos API and retrieves the discount percent of the 
 * product based on the user's input. 
 */
	public static int retrieveDiscount(String sku) {
		try {
			URL url = new URL(ZAPPOS_API + sku + ZAPPOS_API_KEY);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setAllowUserInteraction(false);

			// buffer the response into a string
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			br.close();
			conn.disconnect();

			String JSONstring = sb.toString();
			String[] discountArray = discountJSONParse(JSONstring);
			int discountInt;
			for (int i = 0; i < discountArray.length; i++) {
				discountArray[i] = discountArray[i].replace("%", "");
			}

			discountInt = Integer.parseInt(discountArray[1]);

			return discountInt;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	} // end retrieveDiscount

/*
 * Parses the JSON object called from the Zappos API into a String. 
 */
	public static String[] discountJSONParse(String discounts) {
		JSONObject JSONresponse;
		try {
			JSONresponse = new JSONObject(discounts);
			JSONArray JSONproducts = JSONresponse.getJSONArray("results");
			String[] discountArray = new String[JSONproducts.length()];
			String[] productName = new String[JSONproducts.length()];
			String[] productUrl = new String[JSONproducts.length()];
			
			for (int i = 0; i < discountArray.length; i++) {
				discountArray[i] = ((JSONObject) JSONproducts.get(i))
						.getString("percentOff");
				productName[i] = ((JSONObject) JSONproducts.get(i))
						.getString("productName");
				productUrl[i] = ((JSONObject) JSONproducts.get(i))
						.getString("productUrl");
			}
			PRODUCTNAME = productName[1];
			PRODUCTURL = productUrl[1];
			return discountArray;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;

	} // end discountJSONParse

/*
 * Once the item is on sale the application will call this function to send
 * the user an email notifying them the product is now on sale for 
 * at least 20% off.
 */
	private static void sendFromGmail(String from, String pass, String[] to, String subject, String body) {
		Properties props = System.getProperties();
		String host = "smtp.gmail.com";
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", from);
		props.put("mail.smtp.password", pass);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");

		Session session = Session.getDefaultInstance(props);
		MimeMessage message = new MimeMessage(session);

		try {
			message.setFrom(new InternetAddress(from));
			InternetAddress[] toAddress = new InternetAddress[to.length];

			// To get the array of addresses
			for (int i = 0; i < to.length; i++) {
				toAddress[i] = new InternetAddress(to[i]);
			}

			for (int i = 0; i < toAddress.length; i++) {
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
			}

			message.setSubject(subject);
			message.setText(body);
			Transport transport = session.getTransport("smtp");
			transport.connect(host, from, pass);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (AddressException ae) {
			ae.printStackTrace();
		} catch (MessagingException me) {
			me.printStackTrace();
		}
	} // end sendFromGmail
} // end sendMail