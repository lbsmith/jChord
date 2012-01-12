import java.math.*;
import java.util.*;
import java.net.*;

import org.apache.xmlrpc.*;

public class VirtualNode
{
  private Hashtable chordNodes;
  private ChordApp parent;
  private WebServer rpcserv;
  private Random rand = new Random();
  private VNodeStab stab;
  
  public VirtualNode(ChordApp p, int port)
  {
    parent = p;
    chordNodes = new Hashtable();
        
    try 
    {
      rpcserv = new WebServer(port);
      rpcserv.start();
    } catch (java.io.IOException e) { parent.Log("error starting rpc"); }

    stab = new VNodeStab(this);
    stab.start();
  }
  
  public String toString() 
  {
    String out = "";
    Enumeration e = chordNodes.elements();

    while (e.hasMoreElements())
    {
      LocalNode ln = (LocalNode)e.nextElement();
      try 
      {      
        out += ln.toString()+"\n";
      } 
      catch (NullPointerException ex) { out += "(null)"; }
    }

    return out;
  }
  
  public int size()
  {
    return chordNodes.size();
  }

  public Enumeration nodes()
  {
    return chordNodes.elements();
  }
  
  public void add(LocalNode l)
  {
    chordNodes.put((l.getIdentifier()).toString(), l);
    
    if (chordNodes.size() == 1)
      rpcserv.addHandler("discovery", l.getRPC());

    rpcserv.addHandler((l.getIdentifier()).toString(), l.getRPC());
  }

  public void fail()
  {
    LocalNode removenode = randomNode();

    removenode.fail();
    chordNodes.remove((removenode.getIdentifier()).toString());
    rpcserv.removeHandler((removenode.getIdentifier()).toString());
  }

  public void remove()
  {
    LocalNode removenode = randomNode();

    removenode.leave();
    chordNodes.remove((removenode.getIdentifier()).toString());
    rpcserv.removeHandler((removenode.getIdentifier()).toString());
  }
  
  public LocalNode randomNode()
  {
    Enumeration e = chordNodes.elements();

    for (int i = 0; i < rand.nextInt(chordNodes.size()); i++)
      e.nextElement();

    return (LocalNode)e.nextElement();
  }

  public LocalNode randomNodeAvoid(NodeIdentity node)
  {
    LocalNode n2;

    do 
    {
      n2 = randomNode();
    } while (n2.getIdentifier().equals(node));

    return n2;
  }
}
