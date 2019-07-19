package client;

import utils.CloseUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/*
 * @author rafa gao
 */
class TCPClient {


    /*
     * 根据serverInfo信息来建立TCP连接
     * @param ServerInfo：服务器信息
     *
     *
     * */
     static void linkWith(ServerInfo serverInfo) {
        //创建套接字
        Socket client = new Socket();
        try {
            //初始化
            initSocket(client);
            //连接到服务器
            client.connect(new InetSocketAddress(serverInfo.getAddress(), serverInfo.getPort()), 10000);
            System.out.println("TCP connection started");
            System.out.println("Client information:" + "  IP:" + "[" + client.getLocalAddress().toString() + "]" + "   PORT" + "[" + client.getLocalPort() + "]");
            System.out.println("Server information:" + "  IP:" + "[" + client.getInetAddress().toString() + "]" + "   PORT" + "[" + client.getPort() + "]");
            //发送以及接收消息
            ReadHandle readHandle = new ReadHandle(client.getInputStream());
            readHandle.start();
            write(client);
            //释放资源
            CloseUtils.close(client);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void initSocket(Socket socket) throws SocketException {
        //设置读取超时时间
//        socket.setSoTimeout(3000);
        //设置是否复用端口号
        socket.setReuseAddress(true);
        socket.setSoLinger(true, 20);
        socket.setTcpNoDelay(false);
        socket.setOOBInline(true);
        socket.setKeepAlive(true);
        socket.setPerformancePreferences(1, 1, 1);
    }

    private static void write(Socket client) throws IOException {
        //得到输入和输出流
        OutputStream outputStream = client.getOutputStream();
        //构建键盘的输入流(字符)
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        //构建发送给服务器的流
        PrintStream printStream = new PrintStream(outputStream);

        while (true) {
            //读取键盘上的一行
            String str = bufferedReader.readLine();
            //发送给服务器
            printStream.println(str);
            //得到返回的数据
            if ("bye".equalsIgnoreCase(str)) {
                break;
            }
        }
        //关闭资源
        CloseUtils.close(outputStream, bufferedReader, printStream);
    }

    private static class ReadHandle extends Thread {
        //这个线程只需要得到输入流即可
        private final InputStream inputStream;

        private ReadHandle(InputStream inputStream) {
            this.inputStream = inputStream;
        }


        @Override
        public void run() {
            super.run();
            try {
                //开始进行接收客户端的消息
                todo(inputStream);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("read data exception");
            } finally {
                exit();
            }

        }

        private void todo(InputStream inputStream) throws IOException {
            //构建从客户端得到的流
            BufferedReader bufferedReaderSocket = new BufferedReader(new InputStreamReader(inputStream));
            while (true) {
                String data = bufferedReaderSocket.readLine();
                //已经无法再读取消息，退出整个客户端
                if (data == null) {
                    System.out.println("connection closed");
                    bufferedReaderSocket.close();

                    break;
                }
                System.out.println(data);
            }
            bufferedReaderSocket.close();
        }

        private void exit() {
            CloseUtils.close(inputStream);
        }
    }
}
