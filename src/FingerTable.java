import java.math.*;

public class FingerTable
{
  private RemoteNode finger[];
  private BigInteger fingerstart[];
  private ChordApp cb;

  public FingerTable(LocalNode parent)
  {
    cb = parent.getCallback();

    finger = new RemoteNode[cb.IDENTIFIER_BITS+1];
    fingerstart = new BigInteger[cb.IDENTIFIER_BITS+1];

    NodeIdentity nodeID = parent.getIdentifier();

    BigInteger base = new BigInteger("2");
    base = base.pow(cb.IDENTIFIER_BITS);

    BigInteger nodeNum = nodeID.getAsBigInt(); 
 
    for (int i = 1; i < finger.length; i++) 
    {
      finger[i] = null;
      fingerstart[i] = makeStart(nodeNum, base, i); 
//      finger[i] = new Finger();
//      finger[i].start = makeStart(nodeNum, base, i);

//      if (i != 1) 
//        finger[i-1].intervalEnd = finger[i].start;
//      if (i == Simulator.IDENTIFIER_BITS)  
//        finger[i].intervalEnd = nodeNum;
    }
  }

  private BigInteger makeStart(BigInteger n, BigInteger base, int i)
  {
    BigInteger out = new BigInteger("2");

    out = out.pow(i-1);
    out = out.add(n);
    out = out.mod(base);

    return out;
  } 

  public void updateFinger(int i, RemoteNode n)
  {
    finger[i] = n;
//    finger[i].node = n;
  }

  public RemoteNode getFinger(int i)
  {
    return finger[i];
//    return finger[i].node;
  }

  public BigInteger getFingerStart(int i)
  {
    return fingerstart[i];
//    return finger[i].start;
  } 
}
