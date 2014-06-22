package realmConnection;

public enum Synchronizer {
  WRITER(0), HEARTBEAT(1), READER(2);

  private int synchronizedObject;

  Synchronizer(int num) {
    this.synchronizedObject = num;
  }

  public int getSynchronizedObject() {
    return synchronizedObject;
  }
}
