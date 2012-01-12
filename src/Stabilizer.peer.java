import java.util.*;

public class Stabilizer extends Thread
{
  private LocalNode parent; // owner of this stabilizer
  private ChordApp cb;
  private RemoteNode self;
  private int nextFingerCheck, nextKeyCheck;
  private boolean running;
  private int decayCtr;
  private final int DEFAULT_DECAY = 10;

  public Stabilizer(LocalNode p)
  {
    parent = p;
    self = parent.getSelf();
    nextFingerCheck = 1;
    nextKeyCheck = 0;
    running = true;
    decayCtr = DEFAULT_DECAY;
    cb = p.getCallback();
  }

  public void setActive()
  {
    decayCtr = DEFAULT_DECAY;
  }

  public void run()
  {
    do {
      stabilize();
      checkSuccessor();
      fixFingers();
      transferKeys();
      checkPredecessor();

      if (decayCtr > 0) decayCtr--;

//      try { 
//        if (decayCtr > 0) 
//          Thread.sleep(1000);
//        else
//          Thread.sleep(10000); 
//      } catch(InterruptedException e) { }
    } while (running);
  }

  public void end()
  {
    running = false;
  }

  public void stabilize()
  {
    RemoteNode s = parent.getSuccessor();
    RemoteNode p = s.getPredecessor();

    if (NodeIdentity.inRange(p, self, s) && !(s.equals(self)))
    {
      setActive();
      parent.updateFinger(1, p);
      cb.Log("(stab) node ["+self+"] has new succ ["+p+"]");
    }

    s = parent.getSuccessor();
    if((s != null) && !(s.equals(self))) 
      s.notify(parent.getIdentifier());
  }

  public void checkSuccessor()
  {
    RemoteNode s = parent.getSuccessor();
    Vector v;
    int i = 0;

    if (s != null)
      v = s.getSuccList();
    else v = null;

    if (v != null)
      parent.updateSuccList(v);
    else
    {
      setActive();
//      Simulator.Log(" * successor failure recovery");
      for (i = 0; i < cb.NUM_REPLICATE; i++)
      {
        s = (RemoteNode)parent.succList.elementAt(i);
 
        if ((s != null) && (s.isAlive())) break;
      }

      if (i >= cb.NUM_REPLICATE) {
        cb.Log("["+self+"] FAILED TO FIND SUCCESSOR IN SUCCESSOR LIST!");
        throw new NullPointerException();
      } else {
        cb.Log("(fail) node ["+self+"] has new succ ["+s+"]");
        parent.updateFinger(1, s);
        parent.updateSuccList(s.getSuccList());
      }
    }
  }

  public void fixFingers()
  {
//    nextFingerCheck = nextFingerCheck + 1;
 
    if (nextFingerCheck > cb.IDENTIFIER_BITS) nextFingerCheck = 1;

    RemoteNode r = parent.findSuccessor(
         new NodeIdentity(parent.fingerTable.getFingerStart(nextFingerCheck), 
         "test", 8888));

    parent.updateFinger(nextFingerCheck, r); 

    if (nextFingerCheck == 1 && !r.equals(parent.getSuccessor())) 
      cb.Log("(fixf) node ["+parent.getIdentifier()+"] has new succ ["+r.getIdentifier()+"]");

    parent.updateFinger(nextFingerCheck, r); 

    nextFingerCheck = nextFingerCheck + 1;
  }

  public void transferKeys()
  {
    Enumeration e = parent.keys.elements();
    int size = parent.keys.size();
    RemoteNode closest, nextsucc;

    try {
      if ((size == 0) || (decayCtr > 0)) return;

      if (nextKeyCheck > size) nextKeyCheck = 0;

      Key k = (Key)parent.keys.elementAt(nextKeyCheck);

      closest = parent.findSuccessor(k.getIdentifier());

      if (closest == null) return;

      if (!closest.equals(self))
      {
        if (!k.isReplicant()) {
          cb.Log("key ["+k+"] transferred to node ["+closest+"]");
          parent.removeKey(k);
          closest.addKey(k, false);
          // remove this key from our storage
//          parent.removeKey(k);
        } else {
          if (k.getDecay() <= 0) {
            parent.removeKey(k);
            cb.Log("key ["+k+"] decayed from node ["+parent.getIdentifier()+"]");
          } else {
            int d = k.getDecay();
            k.setDecay(d--);
          }
          // when do i get rid of this replicated key?
        }
      } else {
        if (k.isReplicant())
          k.setReplicant(false);
 
        nextsucc = closest;
       
        for (int i = 0; i < cb.NUM_REPLICATE; i++)
        {
          if (!nextsucc.hasKey(k)) {
            nextsucc.addKey(k, true);
            cb.Log("key ["+k+"] replicated to node ["+nextsucc+"]");
          }
          nextsucc = nextsucc.getSuccessor(); 

          // if we encounter an error replicating the key
          // repeat this entire effort next time through stabilization
          // in hopes that the successors are correct
          if (nextsucc == null) return;
        }
      }
    } catch (ArrayIndexOutOfBoundsException ex) { } 

    nextKeyCheck++;
  }

  public void checkPredecessor()
  {
    RemoteNode pred = parent.getPredecessor();

    if ((pred != null) && (!pred.isAlive())) 
    {
      setActive();
      parent.updatePred(null);
//      Simulator.Log("(chkp) node ["+parent.getIdentifier()+"] has new pred <null>");
    }
  }
}
