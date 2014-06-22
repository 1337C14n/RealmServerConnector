package realmConnection;

import java.util.concurrent.TimeUnit;

import packets.HeartBeat;

public class BeatingHeart extends ObservableRealmConsumer {

  protected BeatingHeart(RealmConnection connection, Object stream) {
    super(connection, stream);
  }

  @Override
  public void run() {
    while (connected) {
      try {
        Thread.sleep(5100);
        long startTime = System.nanoTime();

        connection.write(new HeartBeat(true));

        synchronized (connection.getSynchro(Synchronizer.HEARTBEAT)) {
          connection.getSynchro(Synchronizer.HEARTBEAT).wait(50000);
        }
        
        long midTime = System.nanoTime();
        if (connected && TimeUnit.NANOSECONDS.toMillis(midTime - startTime) > 5000) {
          System.out.println("Heart beat was not returned");
          this.disconnected();
          break;
        } else if (TimeUnit.NANOSECONDS.toMillis(midTime - startTime) > 5000) {
          break;
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
