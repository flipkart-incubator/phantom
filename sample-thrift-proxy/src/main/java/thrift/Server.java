package thrift;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

public class Server {
 
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void start(int port) {
        try {

            TServerSocket serverTransport = new TServerSocket(port);
 
			ArithmeticService.Processor processor = new ArithmeticService.Processor(new ArithmeticServiceImpl());
 
            TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

            System.out.println("Starting server on port " + port);

            server.serve();

        } catch (TTransportException e) {
            e.printStackTrace();
        }
    }
 
    public static void main(String[] args) {
        new Server().start(8082);
    }
 
}