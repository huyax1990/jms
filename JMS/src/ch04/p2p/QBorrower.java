package ch04.p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

public class QBorrower {
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue responseQ = null;
	private Queue requestQ = null;

	public QBorrower(String queuecf, String requestQueue, String responseQueue) {
		try {
			// 连接提供者获得JMS连接
			Context ctx = new InitialContext();
			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(queuecf);
			qConnect = qFactory.createQueueConnection();
			// 创建JMS会话
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			// 查找请求和响应队列
			responseQ = (Queue) ctx.lookup(responseQueue);
			requestQ = (Queue) ctx.lookup(requestQueue);

			// 现在创建完成 启动连接
			qConnect.start();
		} catch (Exception e) {
			System.exit(1);
		}
	}

	private void sendLoanRequest(double salary, double loanAmt) {
		try {
			// 创建JMS消息
			MapMessage msg = qSession.createMapMessage();
			msg.setDouble("Salary", salary);
			msg.setDouble("LoanAmount", loanAmt);
			msg.setJMSReplyTo(responseQ);

			// 创建发送者并发送消息
			QueueSender qSender = qSession.createSender(requestQ);
			qSender.send(msg);

			// 等待查看贷款申请被接收或拒绝
			String filter = "JMSCorrelationID = '" + msg.getJMSMessageID() + "'";
			QueueReceiver qReceiver = qSession.createReceiver(responseQ, filter);
			TextMessage tmsg = (TextMessage) qReceiver.receive(30000);
			if (tmsg == null) {
				System.out.println("QLender not responding");
			} else {
				System.out.println("Loan request was " + tmsg.getText());
			}

		} catch (Exception e) {
			System.exit(1);
		}
	}

	private void exit() {
		try {
			qConnect.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String[] args) {
		String queuecf = null;
		String requestq = null;
		String responseq = null;
		if (args.length == 3) {
			queuecf = args[0];
			requestq = args[1];
			responseq = args[2];
		} else {
			System.out.println("Invalid arguments. Should be: ");
			System.out.println("java QBorrower factory requestQueue responseQueue");
			System.exit(0);
		}

		QBorrower borrower = new QBorrower(queuecf, requestq, responseq);

		try {
			// 读取所有标准输入，并将其作为一条消息发送
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QBorrower Application Started");
			System.out.println("Press enter to quit application");
			System.out.println("Enter:Salary,Loan_Amount");
			System.out.println("\ne.g. 50000，120000");

			while (true) {
				System.out.println(">");
				String loanRequest = stdin.readLine();
				if (loanRequest == null || loanRequest.trim().length() <= 0) {
					borrower.exit();
				}

				// 解析交易说明
				StringTokenizer st = new StringTokenizer(loanRequest, ",");
				double salary = Double.valueOf(st.nextToken().trim()).doubleValue();
				double loanAmt = Double.valueOf(st.nextToken().trim()).doubleValue();
				borrower.sendLoanRequest(salary, loanAmt);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
