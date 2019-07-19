package client;

/*
 * @author rafa gao
 */
public class Client {

    public static void main(String[] args) {
        try {
            ServerInfo serverInfo = ClientSearcher.searcherServer(10000);
            System.out.println(serverInfo);

            //建立TCP连接
            if (serverInfo != null) {
                TCPClient.linkWith(serverInfo);
                System.out.println("TCP connection is closed");
            } else {
                System.out.println("Search server failed,we have no serverInfo");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
