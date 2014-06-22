package realmConnection;

import java.io.IOException;
import java.io.ObjectInputStream;

import packets.HeartBeat;
import packets.Packet;

public class RealmReader extends ObservableRealmConsumer {

  protected RealmReader(RealmConnection connection, Object stream) {
    super(connection, stream);
  }

  @Override
  public void run() {
    while (connected) {
      try {
        Packet packet = (Packet) ((ObjectInputStream) stream).readObject();
        
        if(!(packet instanceof HeartBeat)){
          System.out.println("<------ " + packet.getClass().getSimpleName());
        }
        
        if(packet instanceof HeartBeat){
          synchronized(connection.getSynchro(Synchronizer.HEARTBEAT)){
            connection.getSynchro(Synchronizer.HEARTBEAT).notify();
          }
        } else {
          connection.incomingPacketQueue.add(packet);
          synchronized (connection.getSynchro(Synchronizer.READER)) {
            connection.getSynchro(Synchronizer.READER).notify();
          }
        }   
      } catch (ClassNotFoundException e){
        System.out.println("Malformed Packet");
      } catch (IOException e) {
        this.disconnected();
      }
    }
  }

}
