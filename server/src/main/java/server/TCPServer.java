package server;


import server.Handle.ClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * @author rafa gao
 */
public class TCPServer implements ClientHandler.ClientHandleCallback {

    private final int TCPPort;
    private ClientListener clientListener;
    //客户端列表
    List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardExecutor;

    public TCPServer(int TCPPort) {
        this.TCPPort = TCPPort;
        forwardExecutor= Executors.newSingleThreadExecutor();
    }


    /*
     * 开始启动一个ClientListener线程，返回是否成功启动的信息
     *
     * */
    public boolean start() {
        if (this.TCPPort > 0) {
            try {
                this.clientListener = new ClientListener(TCPPort);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            //启动ClientListener线程
            clientListener.start();
            System.out.println("ClientListener started");
        } else {
            System.out.println("ClientListener failed to start due to wrong TCPort");
            return false;
        }
        return true;
    }


    /*
     * 1.将从服务器端键盘得到的消息发送给每个服务器
     * 将某个客户端的消息传给所有的客户端
     * */
    public synchronized void broadCast(String str) {
        if (clientHandlerList.isEmpty()) {
            System.out.println("no clientHandler");
        } else {

            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.send(str);
            }
        }
    }

    //将某个客户端的消息传给所有的客户端
    public  void broadCast(String str,ClientHandler clientHandler) {
        System.out.println(str);
        //因为线程池转发是一个异步的操作，此时有的客户端已经关闭了
        forwardExecutor.execute(()->{
            synchronized (TCPServer.this) {
                for (ClientHandler handler : clientHandlerList) {
                    if (!handler.equals(clientHandler)) {
                        handler.send(str);
                    }
                }
            }
        });

    }






    /*
     * 关闭服务器的操作
     *
     * */
    public void exit() {
        if (clientListener != null) {
            //关闭监听线程
            clientListener.exit();
            clientListener = null;
        }
        //关闭与客户端的连接线程
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            //清空客户端列表
            clientHandlerList.clear();
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler clientHandler) {
        if (clientHandler!=null){
         clientHandlerList.remove(clientHandler);
        }
    }

    @Override
    public void onNewMessageArrived(String str, ClientHandler clientHandler) {
        //广播
        broadCast(str,clientHandler);
    }


    /*
     * 监听客户端请求连接线程
     *
     * */
    private class ClientListener extends Thread {
        private int TCPPort;
        private ServerSocket serverSocket;
        private boolean flag = false;


        public ClientListener(int TCPPort) throws IOException {
            this.TCPPort = TCPPort;
            //创建一个serverSocket
            serverSocket = new ServerSocket(TCPPort);
            System.out.println("Server information:" + "  IP" + "[" + serverSocket.getInetAddress().toString() + "]"
                    + "  PORT" + "[" + this.TCPPort+ "]");
        }

        @Override
        public void run() {
            super.run();
            //开始一直监听
            while (!flag) {
                try {
                    //为每一个客户端连接创建一个异步的线程
                    Socket client = serverSocket.accept();
                    String sn = UUID.randomUUID().toString();
                    ClientHandler clientHandler = new ClientHandler(client, sn, TCPServer.this);
                    //启动单个的交互线程
                    clientHandler.readToPrint();
                    //加入clientHandlerList当中
                    synchronized (TCPServer.this){
                        clientHandlerList.add(clientHandler);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        public void exit() {
            flag = true;
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("ClientListener closed");
            }
        }


    }


}
