import java.io.*;

public class Key
{
  private String key;
  private NodeIdentity keyID;
  private boolean replicant;
  private int datasize;
  private int decay; 
  private boolean multipart;
  private int numparts;

  // hos many stabilization rounds to keep a replicated key 
  // not having been contacted by the true owner of the key
  // reset whenever the owner 'touches' it.
  public static final int KEY_DECAY = 5;

  public Key(String keyident, byte[] keyval, boolean encoded)
  {
    keyID = new NodeIdentity(keyident, encoded);
    key = keyID.toString();
    replicant = false; 
    decay = KEY_DECAY;
    datasize = keyval.length;
    multipart = false;
    numparts = 1;

    try {
      FileOutputStream fs = new FileOutputStream("cache/"+key+".key");
      fs.write(keyval);
      fs.close();
    } catch (IOException e) { e.printStackTrace(); }
  }

  public Key(String keyident, int keycount, boolean encoded)
  {
    keyID = new NodeIdentity(keyident, encoded);
    key = keyID.toString();
    replicant = false;
    decay = KEY_DECAY;
    datasize = 0;
    multipart = true;
    numparts = keycount; 
  }

  public String toString()
  {
    return key;
  }

  public boolean equals(Key k)
  {
    return key.equals(k);
  }
  
//  protected void finalize() throws Throwable {
  public void remove() {
    //try {
    if (datasize > 0) { 
      File f = new File ("cache/"+key+".dat");

      f.delete();
    }
  /*  } finally {
        super.finalize();
    } */
  }

  public NodeIdentity getIdentifier()
  {
    return keyID;
  }

  public String getKeyString()
  {
    return key;
  }

  public byte[] getValueArray()
  {
    if (datasize > 0) {
      byte[] out = new byte[datasize];
 
      try {
        FileInputStream fs = new FileInputStream("cache/"+key+".key");
        fs.read(out);
        fs.close();
      } catch (IOException e) { e.printStackTrace(); }

      return out;    
    } else {
      return null;
    }
  }

  public int getDecay() 
  {
    return decay;
  }

  public void setDecay(int d)
  {
    decay = d;
  }

  public boolean isReplicant()
  {
    return replicant;
  }

  public void setReplicant(boolean r)
  {
    replicant = r;
  }

  public boolean getMulti()
  {
    return multipart;
  }

  public int getNumParts()
  {
    return numparts;
  }
}
