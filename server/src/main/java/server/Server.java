package server;

import constants.TCPConstant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/*
 * @author rafa gao
 */
public class Server {

    public static void main(String[] args) throws IOException {

        //启动服务器端的TCP线程
        TCPServer tcpServer = new TCPServer(TCPConstant.PORT_SERVER);
        boolean isSucceed = tcpServer.start();

        if (!isSucceed) {
            System.out.println("server failed to start");
        }

        //开启UDP连接，回送TCPPort给客户端
        ServerProvider.start(TCPConstant.PORT_SERVER);

        //读取键盘输入
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String str = bufferedReader.readLine();
            //如果是bye，结束 否则给每个客户端发送消息
            if (str.equalsIgnoreCase("bye")) {
                break;
            } else {
                //给所有客户端传送消息
                tcpServer.broadCast( str);
            }
        }
        tcpServer.exit();
        ServerProvider.stop();


    }
}
