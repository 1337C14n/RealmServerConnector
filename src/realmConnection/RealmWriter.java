package realmConnection;

import java.io.IOException;
import java.io.ObjectOutputStream;

import packets.HeartBeat;
import packets.Packet;

public class RealmWriter extends ObservableRealmConsumer {
  /*
   * Writes any and all packets left in Connection Queue;
   */
  protected RealmWriter(RealmConnection connection, ObjectOutputStream stream) {
    super(connection, stream);
  }

  @Override
  public void run() {
    while (true) {
      if (connection.hasOutgoingPackets()) {
        try {
          Packet packet = connection.readOutgoing();
          
          if (!(packet instanceof HeartBeat)) {
            System.out.println(packet.getClass().getSimpleName() + " ------>");
          }
          
          ((ObjectOutputStream) stream).writeObject(packet);
          ((ObjectOutputStream) stream).flush();

        } catch (IOException e) {
          this.disconnected();
          break;
        }
      } else {
        try {
          synchronized (connection.getSynchro(Synchronizer.WRITER)) {
            connection.getSynchro(Synchronizer.WRITER).wait();
          }
        } catch (InterruptedException e) {
          // Should never Happen
        }

      }
    }
  }
}
