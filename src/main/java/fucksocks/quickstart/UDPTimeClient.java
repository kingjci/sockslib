package fucksocks.quickstart;

import fucksocks.client.Socks5;
import fucksocks.client.Socks5DatagramSocket;
import fucksocks.common.UsernamePasswordCredentials;
import fucksocks.common.net.MonitorDatagramSocketWrapper;
import fucksocks.common.net.NetworkMonitor;
import fucksocks.utils.ArgUtil;
import fucksocks.utils.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * @author Youchao Feng
 * @version 1.0
 * @date Sep 24, 2015 9:53 AM
 */
public class UDPTimeClient {

  private static final Logger logger = LoggerFactory.getLogger(UDPTimeClient.class);

  public static void main(String[] args) {
    new UDPTimeClient().start(args);
  }

  public void start(@Nullable String[] args) {
    String host = "localhost";
    int port = 5050;
    String proxyHost = null;
    int proxyPort = 1080;
    boolean useProxy = false;
    String username = null;
    String password = null;
    String message = "Hi, I am UDP client";

    if (args != null) {
      for (String arg : args) {
        if (arg.equals("-h") || arg.equals("--help")) {
          showHelp();
          System.exit(0);
        } else if (arg.startsWith("--proxy-host=")) {
          proxyHost = ArgUtil.valueOf(arg);
          useProxy = true;
        } else if (arg.startsWith("--proxy-port=")) {
          proxyPort = ArgUtil.intValueOf(arg);
        } else if (arg.startsWith("--proxy-user=")) {
          username = ArgUtil.valueOf(arg);
        } else if (arg.startsWith("--proxy-password=")) {
          password = ArgUtil.valueOf(arg);
        } else if (arg.startsWith("--host=")) {
          host = ArgUtil.valueOf(arg);
        } else if (arg.startsWith("--port=")) {
          port = ArgUtil.intValueOf(arg);
        } else if (arg.startsWith("--message=")) {
          message = ArgUtil.valueOf(arg);
        } else {
          logger.error("Unknown argument [{}]", arg);
          System.exit(-1);
        }
      }
    }

    if (useProxy && proxyHost == null) {
      logger.error("Please use [--proxy-host] to set proxy server's hostname if you want to use "
          + "SOCKS proxy");
      System.exit(-1);
    }

    DatagramSocket clientSocket = null;
    try {
      NetworkMonitor networkMonitor = new NetworkMonitor();
      if (useProxy) {
        Socks5 proxy = new Socks5(new InetSocketAddress(proxyHost, proxyPort));
        if (username != null && password != null) {
          proxy.setCredentials(new UsernamePasswordCredentials(username, password));
        }
        logger.info("Connect server [{}:{}] by proxy:{}", host, port, proxy);
        clientSocket = new Socks5DatagramSocket(proxy);
      } else {
        logger.info("Connect server [{}:{}]", host, port);
        clientSocket = new DatagramSocket();
      }
      clientSocket = new MonitorDatagramSocketWrapper(clientSocket, networkMonitor);
      byte[] sendBuffer = message.getBytes();
      DatagramPacket packet =
          new DatagramPacket(sendBuffer, sendBuffer.length, new InetSocketAddress(host, port));
      clientSocket.send(packet);

      //Received response message from UDP server.
      byte[] receiveBuf = new byte[100];
      DatagramPacket receivedPacket = new DatagramPacket(receiveBuf, receiveBuf.length);
      clientSocket.receive(receivedPacket);
      String receiveStr = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
      logger.info("Server response:{}", receiveStr);
      logger.info("Total Sent:{}, Total Received:{}, Total:{}", networkMonitor.getTotalSend(),
          networkMonitor.getTotalReceive(), networkMonitor.getTotal());

    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    } finally {
      ResourceUtil.close(clientSocket);
    }

  }

  public void showHelp() {
    System.out.println("Usage: [Options]");
    System.out.println("\t--host=<val>\tUDP time server host, default \"localhost\"");
    System.out.println("\t--port=<val>\tUDP time server port, default \"5050\"");
    System.out.println("\t--proxy-host=<val>\tHost of SOCKS5 proxy server");
    System.out.println("\t--proxy-port=<val>\tPort of SOCKS5 proxy server, default \"1080\"");
    System.out.println("\t--proxy-user=<val>\tUsername of SOCKS5 proxy server");
    System.out.println("\t--proxy-password=<val>\tPassword of SOCKS5 proxy server");
    System.out.println("\t--message=<val>\tThe message which will send to server");
    System.out.println("\t-h or --help\tShow help");
  }

}