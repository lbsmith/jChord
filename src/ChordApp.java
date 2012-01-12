public interface ChordApp
{
  public static final int IDENTIFIER_BITS = 160; 
  public static final int NUM_REPLICATE = 4;
//  public static final int KEY_DECAY = 5;
  public static final int KEY_TEST_SIZE = 1024;
  public static final int KEY_CHUNK_SIZE = 1024 * 20;

  public void Print(String s);
  public void Log(String s);
  public void LogEx(String s);

//  public void ProcessLookup(String filename, byte[] data);
  public void ProcessLookup(MultiPartFile f);
}
