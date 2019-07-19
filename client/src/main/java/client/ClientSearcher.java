package client;

import constants.UDPConstant;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
 * @author rafa gao
 */
public class ClientSearcher {
    //监听端口
    private static int LISTEN_PORT = UDPConstant.PORT_CLIENT_RESPONSE;

    //搜索Server，并且返回服务器的信息
    public static ServerInfo searcherServer(int timeout) throws InterruptedException {
        System.out.println("UDPSearcher started");

        //启动监听（这里要求必须得到了一个serverInfo数据）
        CountDownLatch receiveLatch =new CountDownLatch(1);
        Listener listener = listen(LISTEN_PORT, receiveLatch);
        //在启动线程之后，可以在此立即发送广播
        sendBroadcast();
        receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        listener.exit();
        System.out.println("UDPSearcher finished");


        //得到ServerInfo
        if (listener==null){
            return null;
        }

        List<ServerInfo> serverInfos = listener.getServerInfosAndClose();

        if (!serverInfos.isEmpty()){
            return serverInfos.get(0);
        }

        return null;


    }


    private static Listener listen(int listenPort, CountDownLatch receiveLatch) {
        System.out.println("listen started");
        //等待Listener创建完成
        CountDownLatch startLatch = new CountDownLatch(1);
        Listener listener = new Listener(listenPort, receiveLatch, startLatch);
        //启动线程
        listener.start();
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return listener;
    }

    //监听线程
    public static class Listener extends Thread {


        private final int listenPort;
        private final CountDownLatch receiveLatch;
        private final CountDownLatch startLatch;
        private DatagramSocket ds;
        private byte[] buffer;
        private DatagramPacket receivePacket;
        private boolean flag = false;
        private int minLength=UDPConstant.HEADER.length+2+4;
        private List<ServerInfo> serverInfos=new ArrayList<>();



        public Listener(int listenPort, CountDownLatch receiveLatch, CountDownLatch startLatch) {
            super();
            this.listenPort = listenPort;
            this.receiveLatch = receiveLatch;
            this.startLatch = startLatch;
        }

        @Override
        public void run() {
            super.run();
            //初始化完成，可以解除startLatch
            startLatch.countDown();
            try {
                //监听回送端口
                ds = new DatagramSocket(listenPort);
                //创建接收消息
                buffer = new byte[128];
                receivePacket = new DatagramPacket(buffer, 0, buffer.length);
                //接收消息
                while (!flag) {
                    ds.receive(receivePacket);
                    //打印服务器端的信息
                    String address = receivePacket.getAddress().getHostAddress();
                    byte[] data = receivePacket.getData();
                    int port = receivePacket.getPort();
                    int dataLength = receivePacket.getLength();
                    boolean isValid=(dataLength>=minLength);
                    System.out.println("server response data :" + "["+data.toString() + "]"+"   from IP :" +"["+ address + "]"+"    PORT :" +"["+ port+"]"+"dataValid"+"["+isValid+"]");
                    //解析接收到的数据
                    ByteBuffer byteBuffer = ByteBuffer.wrap(data,UDPConstant.HEADER.length,dataLength-UDPConstant.HEADER.length);
                    short cmd = byteBuffer.getShort();
                    int TCPPort = byteBuffer.getInt();
                    //cmd或者端口不符合要求，立刻返回
                    if(cmd!=2||TCPPort<=0){
                        continue;
                    }
                    String sn = new String(buffer, minLength, dataLength - minLength);
                    if (sn.length()==0){
                        sn="sn is unknown";
                    }
                    serverInfos.add(new ServerInfo(sn,TCPPort,address));
                    //已经接收到了一个服务器的信息，解除receiveLatch
                    receiveLatch.countDown();
                }
            } catch (IOException e) {

            } finally {
                exit();
            }


        }

        public void exit() {
            flag = true;
            System.out.println("listen closed");
            close();
        }


        private void close() {
            if (ds != null) {
                ds.close();

                ds = null;
            }

        }

        public List<ServerInfo> getServerInfosAndClose() {
            //退出操作
            exit();
            return serverInfos;
        }
    }

    //发送广播，向服务器请求TCP端口号，并且想服务器发送自己的信息
    public static void sendBroadcast()  {
        //cmd操作
        short cmd=1;

        System.out.println("ClientSearcher started");
        try {
            //系统自动的分配端口
            DatagramSocket client = new DatagramSocket();
            //构建一份请求数据,不需要sn
            byte[] request = new byte[128];
            ByteBuffer byteBuffer = ByteBuffer.wrap(request);
            byteBuffer.put(UDPConstant.HEADER);
            byteBuffer.putShort(cmd);
            byteBuffer.putInt(LISTEN_PORT);
            DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(),byteBuffer.position()+1);
            //设置广播地址
            datagramPacket.setAddress(InetAddress.getByName("255.255.255.255"));
            datagramPacket.setPort(UDPConstant.PORT_SERVER);
            client.send(datagramPacket);
            //直接关闭
            client.close();
            System.out.println("ClientSearcher closed");



        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
