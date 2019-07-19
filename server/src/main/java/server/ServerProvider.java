package server;

import constants.UDPConstant;
import utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.UUID;

/*
 * @author rafa gao
 */
public class ServerProvider {
    //Provider线程只能是单例
    private static Provider providerInstance;

    /*
     * @param     开始工作,这个端口是服务器的TCP端口
     *
     * */
    public static void start(int port) {

        //创建单例
        if (providerInstance != null) {
            stop();
        } else {

            providerInstance = new Provider(UUID.randomUUID().toString().getBytes(), port);
            System.out.println("ServerProvider started");

            providerInstance.start();
        }


    }

    //结束工作
    public static void stop() {
        //不要使用close退出
        providerInstance.exit();
        providerInstance = null;

    }


    private static class Provider extends Thread {
        private final byte[] sn;
        private boolean flag = false;
        //需要广播出去的TCP端口号
        private int TCPPort;

        private DatagramSocket da = null;


        public Provider(byte[] sn, int TCPPort) {

            this.sn = sn;
            this.TCPPort = TCPPort;

        }

        @Override
        public void run() {
            super.run();

            try {
                //绑定端口
                da = new DatagramSocket(UDPConstant.PORT_SERVER);

                while (!flag) {
                    byte[] buf = new byte[128];

                    DatagramPacket receivedPacket = new DatagramPacket(buf, 0, buf.length);

                    //接收消息
                    da.receive(receivedPacket);


                    //接收到的receivedPacket后续处理
                    byte[] data = receivedPacket.getData();
                    int length = receivedPacket.getLength();
                    String hostAddress = receivedPacket.getAddress().getHostAddress();
                    int port = receivedPacket.getPort();
                    boolean isValid = (length >= (UDPConstant.HEADER.length + 4 + 2))
                            && (ByteUtils.startsWith(data, UDPConstant.HEADER));
                    System.out.println("server response data :" + data.toString() + "   from IP :" + "[" + hostAddress + "]" + "  / PORT :" + "[" + port + "]" + "dataValid" + "[" + isValid + "]");


                    if (isValid) {
                        //解析data
                        int index = UDPConstant.HEADER.length;
                        short cmd = (short) (((data[index++] << 8) & 0XFF) | (data[index++] & 0XFF));
                        int responsePort = ((data[index++] << 24) |
                                ((data[index++] << 16)) |
                                ((data[index++] << 8)) |
                                (data[index]));


                        //判断cmd和responsePort的合法性
                        if (cmd == 1 && responsePort > 0) {
                            //构建一份会送的数据，使用ByteBuffer
                            byte[] bytesResponse = new byte[128];
                            ByteBuffer byteBuffer = ByteBuffer.wrap(bytesResponse);
                            byteBuffer.put(UDPConstant.HEADER);
                            byteBuffer.putShort((short) 2);
                            byteBuffer.putInt(TCPPort);
                            byteBuffer.put(sn);
                            DatagramPacket datagramPacketResponse = new DatagramPacket(bytesResponse, 0, bytesResponse.length
                                    , receivedPacket.getAddress(), responsePort);

                            //发送回去
                            da.send(datagramPacketResponse);
                            System.out.println("server send data :" + bytesResponse.toString() + "   to IP :" + "[" + hostAddress + "]" + "  / PORT :" + "[" + responsePort + "]");


                        }


                    }

                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                exit();
            }


        }

        public void exit() {
            flag = true;
            close();
        }


        private void close() {
            if (da != null) {
                da.close();

                da = null;
            }

        }

        public byte[] getSn() {
            return sn;
        }

        public int getTCPPort() {
            return TCPPort;
        }
    }

}
