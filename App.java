package com.springCore.sfRestApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.json.JSONTokener;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import aj.org.objectweb.asm.TypeReference;

import org.json.JSONArray;
import org.json.JSONException;

//@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
//@Component
public class App {

	static final String USERNAME = "ritikamathur1997@outlook.com";
	static final String PASSWORD = "Admin12345YObRiUTVtkF8uS1npdatDZxv3";
	static final String LOGINURL = "https://login.salesforce.com";
	static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
	static final String CLIENTID = "3MVG9fe4g9fhX0E41Kb0yYzJsImU5j2GMXIZ8PTsay8KVULSw9MB8nZYqilhxwpQboRDGwwmePa8GYKcl2hNg";
	static final String CLIENTSECRET = "78F2DE48C8F1C73D1034AC9D004F456BFB8EC5C72226FAFFEE9E20CF1151135B";
	private static final String REST_ENDPOINT = "/services/async";
	private static final String API_VERSION = "/v55.0";
	private static String jobId;
	private static String baseUri;
	private static Header oauthHeader;
	private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
	private static String LeadFirstName;
	private static String LeadCompany;
	private static String LeadId;

	public static void main(String[] args) throws IOException, Exception {
		//SpringApplication.run(App.class, args);
		toJson();
	}
	
	public static void authenticate() throws IOException, Exception {

		HttpClient httpclient = HttpClientBuilder.create().build();

		String loginURL = LOGINURL + GRANTSERVICE + "&client_id=" + CLIENTID + "&client_secret=" + CLIENTSECRET
				+ "&username=" + USERNAME + "&password=" + PASSWORD;

		System.out.println(loginURL);
		HttpPost httpPost = new HttpPost(loginURL);
		HttpResponse response = null;

		try {
			// Execute the login POST request
			response = httpclient.execute(httpPost);
		} catch (ClientProtocolException cpException) {
			cpException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			System.out.println(" Error authenticating to Force.com : " + statusCode);
			// Error is in EntityUtils.toString ( response.getEntity ( ) )
			return;
		}
		String getResult = null;

		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceUrl = null;

		try {
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
			System.out.println(jsonObject);
			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
		}

		baseUri = loginInstanceUrl + REST_ENDPOINT + API_VERSION;
		System.out.println("Base URI" + baseUri);

		oauthHeader = new BasicHeader("Authorization", "Bearer " + loginAccessToken);
		System.out.println("oauthHeader1 : " + oauthHeader);
		System.out.println("\n" + response.getStatusLine());
		System.out.println(response.getStatusLine());
		System.out.println(" Successful login ");
		System.out.println(" instance URL : " + loginInstanceUrl);
		System.out.println(" access token / session ID : " + loginAccessToken);

		httpPost.releaseConnection();
		createJob();
	
	}

	public static List<Account> parseCSV() throws IOException, Exception {

		List<Account> accounts = getAccountDetails("C:/Users/ritik/eclipse-workspace/sfRestApi/src/main/java/com/springCore/sfRestApi/account.csv");
	
		for(Account a: accounts) {
			System.out.println(a.getName());
			System.out.println(a.getShippingCity());
		}
		return accounts;
		
	}
	private static List<Account> getAccountDetails(String file) {
		List<Account> accounts = new ArrayList<Account>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			System.out.println("Buffer Reader "+br.toString());
			String row = br.readLine();
			System.out.println("Buffer Reader "+row);
			
			while(row != null) {
				String attributes[] = row.split(",");
				Account account = getAccount(attributes);
				accounts.add(account);
				row = br.readLine();
				System.out.println("List .."+accounts.get(0).getName());
			}
			br.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return accounts;
		
	}

	private static Account getAccount(String[] attributes) {
		String Name = attributes[0];
		String ShippingCity = attributes[1];
		Account a = new Account(Name, ShippingCity);
		// TODO Auto-generated method stub
		System.out.println(a.getName());
		return a;
	}

	public static void createJob() throws IOException, Exception {

		System.out.println("\n_ CREATE JOB__");

		// String uri = baseUri + "/sobjects/Account/";
		String uri = baseUri + "/jobs/ingest/";
		
		//String uri = baseUri +API_VERSION+ "/job";
		//httpPostPOST https://mycompany-42b-dev-ed.my.salesforce.com/services/data/v55.0/services/async/v55.0/job HTTP/1.1
		try {
			// create the JS0N object containing the new lead details .

			// Reading JSON from file systes
			BufferedReader br = new BufferedReader(new FileReader(
					"C:/Users/ritik/eclipse-workspace/sfRestApi/src/main/java/com/springCore/sfRestApi/job.json"));
			String line;
			/*
			 * JSONObject lead = new JSONObject(); lead.put("FirstName", "REST API");
			 * lead.put("LastName", "Lead"); lead.put("Company", "bispsolutions.com");
			 * System.out.println("JSON for lead record to be inserted : \n" +
			 * lead.toString(1));
			 */
			
			StringBuilder sBuilderObj = new StringBuilder();

			while ((line = br.readLine()) != null) {
				sBuilderObj.append(line);
			}

			System.out.println("Json " + sBuilderObj.toString());
			
			  //JSONObject ac = new JSONObject();
			  
			  JSONObject jsonObject = new JSONObject(sBuilderObj.toString());
			
			  System.out.println("Json Obj " +jsonObject);

			HttpClient httpClient = HttpClientBuilder.create().build();

			System.out.println("URL " + uri);

			HttpPost httpPost = new HttpPost(uri);
			
			System.out.println("oauthHeaderrr " + oauthHeader);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);

			StringEntity body = new StringEntity(jsonObject.toString());
			body.setContentType("application/json");
			httpPost.setEntity(body);

			System.out.println("HEADER " + httpPost.getAllHeaders().toString());

			System.out.println("httpPost" + httpPost);

			// Make the request
			HttpResponse response = httpClient.execute(httpPost);

			System.out.println("RESPONSE " + response);

			// Process the results
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				String response_string = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(response_string);

				// Store the retrieved lead id to use when we update the lead .
				jobId = json.getString("id");
				 
				System.out.println("Job started with job id" +jobId);

			} else {
				System.out.println(" Job unscuccess " + statusCode);
				// The message we are going to post
			}

			br.close();
		} catch (JSONException e) {
			System.out.println(" Issue creating JSON or processing results ");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		
	//createLeads();
		}

	public static int queryLeads() {
		System.out.println("\n_Lead QUERY_\n");
		int statusCode = 0;
		try {
			// Set up the HTTP objects needed to make the request .
			HttpClient httpClient = HttpClientBuilder.create().build();
			String uri = baseUri + "/query?q=Select+Company+From+Lead+Limit+3";
			System.out.println(" Query URL : " + uri);
			HttpGet httpGet = new HttpGet(uri);
			System.out.println("oautnHeader2 : " + oauthHeader);
			httpGet.addHeader(oauthHeader);
			httpGet.addHeader(prettyPrintHeader);
			HttpResponse response = httpClient.execute(httpGet);

			// Process the result
			statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				String response_string = EntityUtils.toString(response.getEntity());
				try {
					JSONObject json = new JSONObject(response_string);
					System.out.println(" JSON result of Query : \n " + json.toString(1));
					JSONArray j = json.getJSONArray("records");
					for (int i = 0; i < j.length(); i++) {
						LeadCompany = json.getJSONArray("records").getJSONObject(i).getString("Company");
					}
				} catch (JSONException je) {
					// TODO: handle exception
					je.printStackTrace();
				}
			} else {
				System.out.println("Query was unsuccessful . Status code returned is " + statusCode);
				System.out.println(" An error has occured . Http status : " + response.getStatusLine().getStatusCode());
				// System.out.println ( getBody( response.getEntity ( ) . getContent ( ) ) ) ;
				System.exit(-1);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		
		return statusCode;
	}

	public static void createLeads() throws Exception, IOException {
		System.out.println("\n_ Account INSERT__");

		 String uri = baseUri + "/sobjects/Account/";
	//	String uri = baseUri + "jobs/ingest/" +jobId+"/batches/";
		try {
			// create the JS0N object containing the new lead details .

			// Reading JSON from file systes
			BufferedReader br = new BufferedReader(new FileReader(
					"C:/Users/ritik/eclipse-workspace/sfRestApi/src/main/java/com/springCore/sfRestApi/accounts.json"));
			String line;
			/*
			 * JSONObject lead = new JSONObject(); lead.put("FirstName", "REST API");
			 * lead.put("LastName", "Lead"); lead.put("Company", "bispsolutions.com");
			 * System.out.println("JSON for lead record to be inserted : \n" +
			 * lead.toString(1));
			 */

			StringBuilder sBuilderObj = new StringBuilder();

			while ((line = br.readLine()) != null) {
				sBuilderObj.append(line);
			}

			System.out.println("Json " + sBuilderObj.toString());
			
			  //JSONObject ac = new JSONObject();
			  
			  JSONObject lead = new JSONObject(); 
			  JSONObject jsonObject = new JSONObject(sBuilderObj.toString());
			  
				lead.put("Name", jsonObject.getString("Name"));
				
			 // ac.put("Name", "Account4"); ac.put("ShippingCity","City1");
			  
			  
			  System.out.println("Json Obj " +jsonObject);
			  
				/*
				 * JSONArray j = jsonObject.getJSONArray("accounts");
				 * 
				 * for (int i = 0; i < j.length(); i++) { //System.out.println("Array[i] " +
				 * j.getJSONObject(i).getString("Company")); lead.put("Name",
				 * j.getJSONObject(i).getString("Name")); lead.put("ShippingCity",
				 * j.getJSONObject(i).getString("ShippingCity")); lead.put("NumberOfEmployees",
				 * j.getJSONObject(i).getString("NumberOfEmployees")); }
				 */
				System.out.println("JSON for lead record to be inserted : \n" + lead.toString(1));
				 
			// Construct the objects needed for the request
				
				 System.out.println("JSON for lead record to be inserted : \n" +
						lead.toString(1));
			HttpClient httpClient = HttpClientBuilder.create().build();

			System.out.println("URL " + uri);

			HttpPost httpPost = new HttpPost(uri);
			
			System.out.println("oauthHeaderrr " + oauthHeader);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);

			StringEntity body = new StringEntity(lead.toString());
			body.setContentType("application/json");
			httpPost.setEntity(body);

			System.out.println("HEADER " + httpPost.getAllHeaders().toString());

			System.out.println("httpPost" + httpPost);

			// Make the request
			HttpResponse response = httpClient.execute(httpPost);

			System.out.println("RESPONSE " + response);

			// Process the results
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 201) {
				String response_string = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(response_string);

				// Store the retrieved lead id to use when we update the lead .
				 LeadId = json.getString("id");
				System.out.println("New Leads inserted successfully" +LeadId);

			} else {
				System.out.println(" Insertion unsuccessful . Status code returned is " + statusCode);
				// The message we are going to post
			}

			br.close();
		} catch (JSONException e) {
			System.out.println(" Issue creating JSON or processing results ");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		
		updateaccount();
		
	}
	public static void updateaccount() throws Exception, IOException {
		System.out.println("\n_ Account UPSERT__");

		// String uri = baseUri + "/sobjects/Account/";
		String uri = baseUri + "/sobjects/Account/" +LeadId;
		try {
			// create the JS0N object containing the new lead details .

			// Reading JSON from file systes
			BufferedReader br = new BufferedReader(new FileReader(
					"C:/Users/ritik/eclipse-workspace/sfRestApi/src/main/java/com/springCore/sfRestApi/updateaccounts.json"));
			String line;
			/*
			 * JSONObject lead = new JSONObject(); lead.put("FirstName", "REST API");
			 * lead.put("LastName", "Lead"); lead.put("Company", "bispsolutions.com");
			 * System.out.println("JSON for lead record to be inserted : \n" +
			 * lead.toString(1));
			 */

			StringBuilder sBuilderObj = new StringBuilder();

			while ((line = br.readLine()) != null) {
				sBuilderObj.append(line);
			}

			System.out.println("Json " + sBuilderObj.toString());
			
			  //JSONObject ac = new JSONObject();
			  
			  JSONObject lead = new JSONObject(); 
			  JSONObject jsonObject = new JSONObject(sBuilderObj.toString());
			  
			  lead.put("Name", jsonObject.getString("Name"));
				
			 // ac.put("Name", "Account4"); ac.put("ShippingCity","City1");
			  
			  
			  System.out.println("Json Obj " +jsonObject);
			  
			 // JSONArray j = jsonObject.getJSONArray("records");

				/*
				 * JSONObject lead = new JSONObject(); lead.put("FirstName", "REST API");
				 * lead.put("LastName", "Lead"); lead.put("Company", "bispsolutions.com");
				 * System.out.println("JSON for lead record to be inserted : \n" +
				 * lead.toString(1));
				 */
			// Construct the objects needed for the request
				
				 System.out.println("JSON for lead record to be inserted : \n" +
						lead.toString(1));
			HttpClient httpClient = HttpClientBuilder.create().build();

			System.out.println("URL " + uri);

			HttpPatch httpPatch = new HttpPatch(uri);
			System.out.println("oauthHeaderrr " + oauthHeader);
			httpPatch.addHeader(oauthHeader);
			httpPatch.addHeader(prettyPrintHeader);

			StringEntity body = new StringEntity(lead.toString());
			body.setContentType("application/json");
			httpPatch.setEntity(body);

			System.out.println("HEADER " + httpPatch.getAllHeaders().toString());

			System.out.println("httpPost" + httpPatch);

			// Make the request
			HttpResponse response = httpClient.execute(httpPatch);
			System.out.println("RESPONSE " + response);

			// Process the results
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == 204) {
				/*
				 * String response_string = EntityUtils.toString(response.getEntity());
				 * JSONObject json = new JSONObject(response_string);
				 * 
				 * // Store the retrieved lead id to use when we update the lead . LeadId =
				 * json.getString("id");
				 */	System.out.println("Updated Account successfully" +LeadId);

			} else {
				System.out.println(" Updation unsuccessful . Status code returned is " + statusCode);
				// The message we are going to post
			}

			br.close();
		} catch (JSONException e) {
			System.out.println(" Issue creating JSON or processing results ");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
	}
	
	
	public static void upsert() throws Exception, IOException {
		int statusCode = queryLeads() ;
		if(statusCode == 404) {
			createLeads();
		}
		else if(statusCode == 200) {
			updateaccount();
		}
	}
	
	public static void toJson() {
		
		try {
			
			List<Account> accounts = parseCSV();

			for (Account a : accounts) {
	
				ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(new File("C:/Users/ritik/eclipse-workspace/sfRestApi/targer/account1.json"), a);
			
			}

			/*
			 * Account a = new Account(); a.setName("Ritu"); a.setShippingCity("Ajmer");
			 */
		
		} catch (Exception e) {
			// TODO: handle exception
		}
	} 
	
}