package thrift;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class ArithmeticClient {
 
	public ArithmeticClient() {
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		for (int i=0; i < 10000; i++) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			executorService.execute(new Worker());
		}
		executorService.shutdown();		
	}
	
	private class Worker implements Runnable {
		public void run() {
			try {

				TTransport transport = new TSocket("localhost", 8080);
	            TProtocol protocol = new TBinaryProtocol(transport);
	            ArithmeticService.Client client = new ArithmeticService.Client(protocol);
	            transport.open();
	 
	            long addResult = client.add(100, 200);
	            System.out.println("Add result: " + addResult);
	            long multiplyResult = client.multiply(20, 40);
	            System.out.println("Multiply result: " + multiplyResult);
	 
	            transport.close();

			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new ArithmeticClient();
	}
	 
}