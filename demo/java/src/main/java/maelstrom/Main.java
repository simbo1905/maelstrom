package maelstrom;


import maelstrom.broadcast.BroadcastServer;

public class Main {
  public static void main(String[] args) {
    // new EchoServer().run();
    new BroadcastServer().run();
    //new TxnListAppendServer().run();
  }
}
