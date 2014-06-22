package realmConnection;

import java.util.Observable;
import java.util.Observer;
import packets.Packet;

public abstract class RealmServerConnector implements Runnable, Observer{
  
  protected volatile static RealmConnection connection;
  private static RealmServerConnector connector;
  
  private static String address;
  private static int port;
  
  private static String type;
  
  protected volatile static boolean connected;
  
  public RealmServerConnector(String address, int port){
    RealmServerConnector.address = address;
    RealmServerConnector.port = port;
    connected = false;
    connector = this;
    connect();
  }
  
  protected static boolean connect(){
    if(connection != null){
      connection.dispose();
    }
    while(true){
      try {
        if(connected){
          break;
        }
        connection = new RealmConnection(address, port);
        connection.addObserver(connector);
        connected = true;
        return true;
      } catch (Exception e){
        System.out.println("Failed to connect");
        connected = false;
        
        if(connection != null){
          connection.deleteObservers();
          connection.dispose();
        }
        
        
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e1) {
          
        }
      }
    }
    return false;
  }

  public RealmConnection getConnection() {
    return connection;
  }
  
  public static void write(Packet packet){
    connection.write(packet);
  }
  
  @Override
  public void update(Observable o, Object arg) {
    if(arg instanceof DisconnectException){
      //Connection disconnected we need to reconnect
      connection.deleteObservers();
      if(connected == true){
        connected = false;
        System.out.println("Disconnected from server");
        System.out.println("Attempting to reconnected");
        
        if(connect()){
          onReconnect();
        }
      }
    }
    
  }

  public static String getType() {
    return type;
  }

  public static void setType(String type) {
    RealmServerConnector.type = type;
  }

  public static boolean isConnected() {
    return connected;
  }
  
  public abstract void onReconnect();
 
}
