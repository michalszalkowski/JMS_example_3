package pl.btbw;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Shop {

	private QueueConnection queueConnection;
	private QueueSession queueSession;
	private Queue requestQueue;
	private Queue responseQueue;

	public Shop(String queueCF, String requestQueue, String responseQueue) {
		try {

			Context ctx = new InitialContext(
					PropertiesUtil.getNoFileProperties()
			);

			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(queueCF);

			queueConnection = qFactory.createQueueConnection();
			queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			this.requestQueue = (Queue) ctx.lookup(requestQueue);
			this.responseQueue = (Queue) ctx.lookup(responseQueue);

			queueConnection.start();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void askAboutProduct(String productName) {
		try {

			TextMessage question = queueSession.createTextMessage();
			question.setText(productName);
			question.setJMSReplyTo(responseQueue);

			QueueSender queueSender = queueSession.createSender(requestQueue);
			queueSender.send(question);

			String filter = "JMSCorrelationID = '" + question.getJMSMessageID() + "'";

			QueueReceiver queueReceiver = queueSession.createReceiver(responseQueue, filter);
			TextMessage answer = (TextMessage) queueReceiver.receive(30000);
			if (answer == null) {
				System.out.println("Storehouse - problem with connection");
			} else {
				System.out.println("Storehouse answer: " + answer.getText());
			}

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
		System.exit(0);
	}

	public static void main(String argv[]) {

		Shop shop = new Shop("QueueCF", "StorehouseRequestQ", "StorehouseResponseQ");

		try {

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(System.in)
			);

			System.out.println("Shop Application Started");
			System.out.println("Press enter product name");

			while (true) {
				System.out.print("> ");

				String productName = bufferedReader.readLine();

				if (productName == null || productName.trim().length() <= 0) {
					shop.exit();
				}

				shop.askAboutProduct(productName);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
