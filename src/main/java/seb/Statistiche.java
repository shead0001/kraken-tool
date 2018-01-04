package seb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
/*import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;*/

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;

public class Statistiche {

	private static final String FILENAME = "statistiche.csv";
	private static final String FILENAME_CONTATORE = "contatore.txt";
	protected static String username = "babbulongu@gmail.com";
	protected static String password = "babbulongu01";
	protected static int VALORE_INTERVALLO = 120000;

	public static void main(String[] args) throws FileNotFoundException {

		while (true) {
			invokeOHLCService("XBTEUR", FILENAME);

			
		}

	}

	public static Double invokeTickerService(String rigaSimboloPair) throws IOException {
		KrakenApi api = new KrakenApi();

		String response;
		Map<String, String> input = new HashMap<>();

		input.put("pair", rigaSimboloPair);
		response = api.queryPublic(Method.TICKER, input);
		// System.out.println(response);

		/*
		 * JSONParser parser = new JSONParser(); Object obj =
		 * parser.parse(response); JSONObject jsonObject = (JSONObject) obj;
		 * System.out.println(jsonObject); return jsonObject;
		 */
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> map = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
		});

		Map<String, Object> resultObj = (Map<String, Object>) map.get("result");
		Set<String> set = resultObj.keySet();
		Iterator<String> it = set.iterator();
		String string = null;
		if (it.hasNext()) {
			string = (String) it.next();
			// System.out.println(string);
		}
		ArrayList<String> boo = (ArrayList<String>) ((Map<String, Object>) resultObj.get(string)).get("c");
		/*
		 * String nonPArserizzabile = boo.get(0); int posC =
		 * nonPArserizzabile.indexOf("c"); int posParentesiApertura =
		 * nonPArserizzabile.indexOf("[", posC) ; // InStr(posC,
		 * nonPArserizzabile, "[") int posInizio = posParentesiApertura + 2; int
		 * posParentesiFineGraffe = nonPArserizzabile.indexOf("\"", posInizio)
		 * ;//posParentesiFineGraffe = InStr(posInizio, stra, """")
		 */String valore = boo.get(0);
		// result = Mid(stra, posInizio, posParentesiFineGraffe - posInizio)
		// System.out.println(valore);
		return Double.valueOf(valore);
	}

	public static Double invokeOHLCService(String rigaSimboloPair, String filename) {
		Long last = null;
		try {
			File counterFile = new File(FILENAME_CONTATORE);
			if (counterFile.exists()) {
				
				FileReader fileReader = new FileReader(counterFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				StringBuffer stringBuffer = new StringBuffer();
				String line;
				if ((line = bufferedReader.readLine()) != null) {
					last = Long.valueOf(line);
					
				}
			}

			KrakenApi api = new KrakenApi();

			String response;
			Map<String, String> input = new HashMap<>();

			input.put("pair", rigaSimboloPair);
			if(last != null)
			{
				input.put("since", last.toString());
			}
			response = api.queryPublic(Method.OHLC, input);
			// System.out.println(response);

			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> map = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
			});

			Map<String, Object> resultObj = (Map<String, Object>) map.get("result");

			
			if (resultObj != null) {
				Set<String> set = resultObj.keySet();
				Iterator<String> it = set.iterator();
				String string = null;
				if (it.hasNext()) {
					string = (String) it.next();
					// System.out.println(string);
				}
				ArrayList lista = (ArrayList<String>) resultObj.get(string);
				Iterator ite = lista.iterator();

				File idea = new File(filename);
				if (!idea.exists()) {

					FileWriter pw = new FileWriter(filename, true);
					pw.append("timestamp").append(";");
					pw.append("time").append(";");
					pw.append("open").append(";");
					pw.append("high").append(";");
					pw.append("low").append(";");
					pw.append("close").append(";");
					pw.append("vwap").append(";");
					pw.append("volume").append(";");
					pw.append("count").append(";");
					pw.append("\n");

					pw.flush();
					pw.close();
				}

				FileWriter pw = new FileWriter(filename, true);

				int riga = 1;
				
				if(last != null && ite.hasNext())
				{
					ite.next();
				}
				while (ite.hasNext()) {

					List listaOHLC = (List) ite.next();
					listaOHLC.get(0);
					Integer time = (Integer) listaOHLC.get(0);
					// Date timeDate = new java.util.Date(time);
					java.util.Date timeDate = new java.util.Date((long) time.intValue() * 1000);
					String open = (String) listaOHLC.get(1);
					String high = (String) listaOHLC.get(2);
					String low = (String) listaOHLC.get(3);
					String close = (String) listaOHLC.get(4);
					String vwap = (String) listaOHLC.get(5);
					String volume = (String) listaOHLC.get(6);
					Integer count = (Integer) listaOHLC.get(7);
					// <vwap>,
					// <volume>,
					// <count>
					pw = new FileWriter(filename, true);
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

					pw.append(time.toString()).append(";"); //
					pw.append(sdf.format(timeDate)).append(";"); // 26/12/17
																	// 22:30

					pw.append(open.replace('.', ',')).append(";");
					pw.append(high.replace('.', ',')).append(";");
					pw.append(low.replace('.', ',')).append(";");
					pw.append(close.replace('.', ',')).append(";");
					pw.append(vwap.replace('.', ',')).append(";");
					pw.append(volume.replace('.', ',')).append(";");
					pw.append(count.toString().replace('.', ',')).append(";");
					pw.append("\n");
					pw.flush();

					riga = riga + 1;
					System.out.println("Added row:"+riga );

				}
				
				pw.close();
				
				FileWriter counteFw = new FileWriter(FILENAME_CONTATORE);
				counteFw.append(resultObj.get("last").toString());
				counteFw.flush();
				counteFw.close();
				
				try {
					System.out.println(" Wait " + VALORE_INTERVALLO / 1000 + " seconds!");
					Thread.sleep(VALORE_INTERVALLO);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			else {
				Map<String, Object> errorsObj = (Map<String, Object>) map.get("error");
				System.out.println("resul is null:"+errorsObj );
			}

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * String nonPArserizzabile = boo.get(0); int posC =
		 * nonPArserizzabile.indexOf("c"); int posParentesiApertura =
		 * nonPArserizzabile.indexOf("[", posC) ; // InStr(posC,
		 * nonPArserizzabile, "[") int posInizio = posParentesiApertura + 2; int
		 * posParentesiFineGraffe = nonPArserizzabile.indexOf("\"", posInizio)
		 * ;//posParentesiFineGraffe = InStr(posInizio, stra, """") String
		 * valore = boo.get(0);
		 */
		// result = Mid(stra, posInizio, posParentesiFineGraffe - posInizio)
		// System.out.println(valore);
		// return Double.valueOf(valore);
		return null;
	}

	public static void invokeTradesService(String rigaSimboloPair, String filename) throws IOException {
		KrakenApi api = new KrakenApi();

		String response;
		Map<String, String> input = new HashMap<>();

		input.put("pair", rigaSimboloPair);
		response = api.queryPublic(Method.TRADES, input);
		// System.out.println(response);

		/*
		 * JSONParser parser = new JSONParser(); Object obj =
		 * parser.parse(response); JSONObject jsonObject = (JSONObject) obj;
		 * System.out.println(jsonObject); return jsonObject;
		 */
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> map = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
		});

		Map<String, Object> resultObj = (Map<String, Object>) map.get("result");
		Set<String> set = resultObj.keySet();
		Iterator<String> it = set.iterator();
		String string = null;
		if (it.hasNext()) {
			string = (String) it.next();
			// System.out.println(string);
		}
		ArrayList lista = (ArrayList<String>) resultObj.get(string);
		Iterator ite = lista.iterator();

		int riga = 1;
		while (ite.hasNext()) {

			List listaTrades = (List) ite.next();

			// <price>, <volume>, <time>, <buy/sell>, <market/limit>,
			// <miscellaneous>

			/*
			 * Integer time = (Integer) listaTrades.get(0); // Date timeDate =
			 * new java.util.Date(time); java.util.Date timeDate = new
			 * java.util.Date((long) time.intValue() * 1000); String price =
			 * (String) listaTrades.get(1); String volume = (String)
			 * listaTrades.get(2); String time = (String) listaTrades.get(3);
			 * String buy_sell = (String) listaTrades.get(4);
			 */

			riga = riga + 1;

		}

		return;
	}

}
