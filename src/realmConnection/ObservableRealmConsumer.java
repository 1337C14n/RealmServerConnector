package realmConnection;

import java.util.Observable;

public abstract class ObservableRealmConsumer extends Observable implements Runnable{
  protected RealmConnection connection;
  protected Object stream;
  
  protected volatile boolean connected = true;
  
  protected ObservableRealmConsumer(RealmConnection connection, Object stream){
    this.connection = connection;
    this.stream = stream;
    
    //Register with connection
    this.addObserver(connection);
  }
  
  protected void disconnected(){
    this.setChanged();
    connected = false;
    this.notifyObservers(new DisconnectException());
  }
  
  protected void disconnect(){
    connected = false;
  }
}
