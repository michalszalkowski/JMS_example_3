package pl.btbw;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Storehouse implements MessageListener {

	private QueueConnection queueConnection;
	private QueueSession queueSession;
	private Queue requestQueue;

	private Map<String, Integer> storage = new HashMap<String, Integer>() {{
		put("book", 10);
		put("PC", 5);
		put("apple", 50);
	}};

	public Storehouse(String queueCF, String requestQueue) {


		try {

			Context ctx = new InitialContext(
					PropertiesUtil.getNoFileProperties()
			);

			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(queueCF);

			queueConnection = qFactory.createQueueConnection();

			queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			this.requestQueue = (Queue) ctx.lookup(requestQueue);

			queueConnection.start();

			QueueReceiver qReceiver = queueSession.createReceiver(this.requestQueue);
			qReceiver.setMessageListener(this);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void onMessage(Message incomingMessage) {
		try {

			TextMessage incomingTextMessage = (TextMessage) incomingMessage;

			String productName = incomingTextMessage.getText();

			TextMessage outgoingMessage = queueSession.createTextMessage();
			outgoingMessage.setText(
					storage.containsKey(productName)
							? "in stock: " + productName + ": " + storage.get(productName)
							: "lack"
			);
			outgoingMessage.setJMSCorrelationID(incomingMessage.getJMSMessageID());

			QueueSender queueSender = queueSession.createSender((Queue) incomingMessage.getJMSReplyTo());
			queueSender.send(outgoingMessage);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void exit() {
		try {
			queueConnection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}

	public static void main(String argv[]) {

		System.out.println("Storehouse application started");
		System.out.println("Press enter to quit application");

		Storehouse storehouse = new Storehouse("QueueCF", "StorehouseRequestQ");

		try {
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(System.in)
			);
			bufferedReader.readLine();
			storehouse.exit();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
