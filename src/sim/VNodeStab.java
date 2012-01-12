import java.util.*;

// create a thread to keep attempting to join off of another node
// repeats until the parent node is no longer joining itself
// mainly to allow the simulator to have concurrent joins
public class VNodeStab extends Thread
{
  VirtualNode parent;

  public VNodeStab(VirtualNode p)
  {
    parent = p;
  }

  public void run()
  {
    do {
      Enumeration e = parent.nodes();
      LocalNode n;

      while (e.hasMoreElements()) {
        n = (LocalNode)e.nextElement();
        n.stabilize();
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) { }
    } while (true);

  }
}
