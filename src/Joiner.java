// create a thread to keep attempting to join off of another node
// repeats until the parent node is no longer joining itself
// mainly to allow the simulator to have concurrent joins
public class Joiner extends Thread
{
  LocalNode parent;
  RemoteNode conn;

  public Joiner(LocalNode p, RemoteNode c)
  {
    parent = p;
    conn = c;
  }

  public void run()
  {
    int s = conn.getState();

    if ((s == LocalNode.STATE_NONE) || (s == LocalNode.STATE_LEAVING)) return;
    if (s == LocalNode.STATE_JOINING) {
      while (s == LocalNode.STATE_JOINING)
      {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) { }
        s = conn.getState();
      }
    }

//    {
//    if (conn.isAlive(parent)) {
    parent.updatePred(null);
    RemoteNode n = conn.findSuccessor(parent.getIdentifier());
    parent.Log("(join) node ["+parent.getIdentifier()+"] has new succ ["+n.getIdentifier()+"]");
    parent.updateFinger(1, n);
//    }
  }
}
