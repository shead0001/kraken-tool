package seb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;

public class Main {

	protected static String username = "babbulongu@gmail.com";
	protected static String password = "babbulongu01";

	public static void main(String[] args) throws FileNotFoundException {

		String pathFile = "posizioni.xls";
		File file = new File(pathFile);
		System.out.println(file.getAbsolutePath());
		InputStream inp = new FileInputStream(pathFile);
		// InputStream inp = new FileInputStream("workbook.xlsx");

		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(inp);

			Sheet sheet = wb.getSheetAt(0);
			Row row = sheet.getRow(1);
			int indicePrimaRiga = (int) row.getCell(1).getNumericCellValue() - 1;
			int indiceUltimaRiga = (int) row.getCell(2).getNumericCellValue() - 1;
			int valoreIntervallo = 5000;
			boolean aggiornaRighe = false;
			boolean aggiornaStatistiche = false;
			boolean sendMail = false;
			try {
				aggiornaRighe = row.getCell(3).getBooleanCellValue();
				;
				valoreIntervallo = (int) row.getCell(4).getNumericCellValue();
				sendMail = row.getCell(5).getBooleanCellValue();
				aggiornaStatistiche = row.getCell(7).getBooleanCellValue();
				;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (aggiornaStatistiche) {
				System.out.println("Aggiorno statistiche!");
				for (int indiceRiga = indicePrimaRiga; indiceRiga <= indiceUltimaRiga; indiceRiga++) {
					try {
						Row riga = wb.getSheetAt(0).getRow(indiceRiga);
						System.out.println("    Aggiorno statistica: " + riga.getCell(1).getStringCellValue());
						invokeOHLCService(riga.getCell(1).getStringCellValue(),
								wb.getSheetAt(indiceRiga - indicePrimaRiga + 1));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println("       " +e.getMessage()); 
						//e.printStackTrace();
					}
				}
				// Write the output to a file
				FileOutputStream fileOut = new FileOutputStream(pathFile);
				try {
					wb.write(fileOut);
					fileOut.close();
				} catch (IOException e) {

				}
			}

			while (aggiornaRighe) {
				System.out.println("Aggiorno righe!");
				for (int indiceRiga = indicePrimaRiga; indiceRiga <= indiceUltimaRiga; indiceRiga++) {
					try {
						processaRiga(wb, indiceRiga, sendMail);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				try {
					System.out.println(" Wait " + valoreIntervallo / 1000 + " seconds!");
					Thread.sleep(valoreIntervallo);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			/*
			 * // Write the output to a file FileOutputStream fileOut = new
			 * FileOutputStream(pathFile); try { wb.write(fileOut);
			 * fileOut.close(); } catch (IOException e) { // TODO Auto-generated
			 * catch block e.printStackTrace(); }
			 */

		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void processaRiga(Workbook wb, int indiceRiga, boolean sendMail) {

		Row riga = wb.getSheetAt(0).getRow(indiceRiga);

		String rigaSimboloPair = riga.getCell(1).getStringCellValue();

		double rigaPrezzoApertura = riga.getCell(3).getNumericCellValue();

		double rigaQuantitaAcuistate = riga.getCell(4).getNumericCellValue();

		// double rigaPrezzoAttuale =
		// primaRiga.getCell(3).getNumericCellValue();

		Double rigaPrezzoAttuale = null;
		Double rigaPrezzoTarget = null;

		try {
			rigaPrezzoAttuale = invokeTickerService(rigaSimboloPair);

			riga.getCell(7).setCellValue(rigaPrezzoAttuale.doubleValue()); // aggiorno
			rigaPrezzoTarget = riga.getCell(8).getNumericCellValue(); // valore
			// attuale azione

			if (rigaPrezzoTarget != null && rigaPrezzoTarget.doubleValue() != 0) {
				if (rigaPrezzoAttuale.doubleValue() > rigaPrezzoTarget.doubleValue()) {
					System.out.println(rigaSimboloPair + " Alert:       " + " Target:" + rigaPrezzoTarget + "    Attuale:"
							+ rigaPrezzoAttuale + "<<<<<<<<<<<<<<<<<<<<<<<<<");
					if (sendMail) {
						try {
							sendMail(rigaSimboloPair + " Alert: " + " Target:" + rigaPrezzoTarget + "    Attuale:"
									+ rigaPrezzoAttuale);
						} catch (AddressException e) {
							System.out.println(" Mail doesn't work!");
						} catch (MessagingException e) {
							System.out.println(" Mail doesn't work!");
						}
					}
				}
				else
				{
					System.out.println(rigaSimboloPair + " No warning!" + "                   Target:" + rigaPrezzoTarget + "    Attuale:"
							+ rigaPrezzoAttuale);
				}	
			}

			else if (rigaPrezzoAttuale > rigaPrezzoApertura) {
				System.out.println(rigaSimboloPair + " Alert:       " + " Apertura:" + rigaPrezzoApertura + "    Attuale:"
						+ rigaPrezzoAttuale + "<<<<<<<<<<<<<<<<<<<<<<<<<");
				if (sendMail) {
					try {
						sendMail(rigaSimboloPair + " Alert: " + " Apertura:" + rigaPrezzoApertura + "    Attuale:"
								+ rigaPrezzoAttuale);
					} catch (AddressException e) {
						System.out.println(" Mail doesn't work!");
					} catch (MessagingException e) {
						System.out.println(" Mail doesn't work!");
					}
				}
			} else {
				System.out.println(rigaSimboloPair + " No warning!" + "                   Apertura:"
						+ rigaPrezzoApertura + "    Attuale:" + rigaPrezzoAttuale);
			}

		} catch (IOException e) {
			System.out.println(" Service doesn't work!");
		}

	}

	public static void sendMail(String messageTxT) throws AddressException, MessagingException {
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(username));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(username));
		message.setSubject(messageTxT);
		message.setText(messageTxT);

		Transport.send(message);

		// System.out.println("Done");

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

	public static Double invokeOHLCService(String rigaSimboloPair, Sheet sheet) throws IOException {

		KrakenApi api = new KrakenApi();

		String response;
		Map<String, String> input = new HashMap<>();

		input.put("pair", rigaSimboloPair);
		response = api.queryPublic(Method.OHLC, input);
		// System.out.println(response);

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> map = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
		});

		Map<String, Object> resultObj = (Map<String, Object>) map.get("result");
		if(resultObj != null)
		{
			Set<String> set = resultObj.keySet();
			Iterator<String> it = set.iterator();
			String string = null;
			if (it.hasNext()) {
				string = (String) it.next();
				// System.out.println(string);
			}
			ArrayList lista = (ArrayList<String>) resultObj.get(string);
			Iterator ite = lista.iterator();
			Row row = sheet.createRow(0);
			row.createCell(0).setCellValue(rigaSimboloPair);
			row.createCell(1).setCellValue("Time");
			row.createCell(2).setCellValue("Open");
			row.createCell(3).setCellValue("Close");
			int riga = 1;
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
				// <vwap>,
				// <volume>,
				// <count>
	
				// System.out.println(string2);
	
				row = sheet.createRow(riga);
				row.createCell(1).setCellValue(timeDate);
				row.createCell(2).setCellValue(Double.valueOf(open));
				row.createCell(3).setCellValue(Double.valueOf(close));
				riga = riga + 1;
	
			}
		}

		return null;
	}

	public static void cleanSheet(Sheet sheet) {
		int numberOfRows = sheet.getPhysicalNumberOfRows();

		if (numberOfRows > 0) {
			for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
				if (sheet.getRow(i) != null) {
					sheet.removeRow(sheet.getRow(i));
				} else {
					System.out.println("Info: clean sheet='" + sheet.getSheetName() + "' ... skip line: " + i);
				}
			}
		} else {
			System.out.println("Info: clean sheet='" + sheet.getSheetName() + "' ... is empty");
		}
	}
}
