package org.basex.util.hash;

import java.io.*;
import java.util.*;

import org.basex.io.in.DataInput;
import org.basex.io.out.DataOutput;
import org.basex.util.*;

/**
 * This is an efficient and memory-saving hash set for storing tokens.
 *
 * @author BaseX Team, BSD License
 * @author Christian Gruen
 */
public class TokenSet extends ASet implements Iterable<byte[]> {
  /** Hashed keys. */
  protected byte[][] keys;

  /**
   * Default constructor.
   */
  public TokenSet() {
    this(Array.INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity.
   * @param capacity array capacity (will be resized to a power of two)
   */
  public TokenSet(final long capacity) {
    super(capacity);
    keys = new byte[capacity()][];
  }

  /**
   * Constructor with initial keys.
   * @param keys keys to be added
   */
  public TokenSet(final byte[]... keys) {
    this(keys.length);
    for(final byte[] key : keys) add(key);
  }

  /**
   * Convenience constructor with initial strings as keys.
   * @param keys keys to be added
   */
  public TokenSet(final String... keys) {
    this(keys.length);
    for(final String key : keys) add(key);
  }

  /**
   * Constructor, specifying some initial input.
   * @param in input stream
   * @throws IOException I/O exception
   */
  public TokenSet(final DataInput in) throws IOException {
    read(in);
  }

  /**
   * Reads the data structure from the specified input.
   * @param in input stream
   * @throws IOException I/O exception
   */
  public void read(final DataInput in) throws IOException {
    keys = in.readTokens();
    next = in.readNums();
    buckets = in.readNums();
    size = in.readNum();
  }

  /**
   * Writes the data structure to the specified output.
   * @param out output stream
   * @throws IOException I/O exception
   */
  public void write(final DataOutput out) throws IOException {
    out.writeTokens(keys);
    out.writeNums(next);
    out.writeNums(buckets);
    out.writeNum(size);
  }

  /**
   * Stores the specified key if it has not been stored before.
   * @param key key to be added
   * @return {@code true} if the key did not exist yet and was stored
   */
  public final boolean add(final byte[] key) {
    return index(key) > 0;
  }

  /**
   * Stores the specified string as key if it has not been stored before.
   * @param key string to be added
   * @return {@code true} if the key did not exist yet and was stored
   */
  public final boolean add(final String key) {
    return add(Token.token(key));
  }

  /**
   * Stores the specified key and returns its id.
   * @param key key to be added
   * @return unique id of stored key (larger than zero)
   */
  public final int put(final byte[] key) {
    final int id = index(key);
    return Math.abs(id);
  }

  /**
   * Checks if the set contains the specified key.
   * @param key key to be looked up
   * @return result of check
   */
  public final boolean contains(final byte[] key) {
    return id(key) > 0;
  }

  /**
   * Returns the id of the specified key, or {@code 0} if the key does not exist.
   * @param key key to be looked up
   * @return id, or {@code 0} if key does not exist
   */
  public final int id(final byte[] key) {
    final int b = Token.hashCode(key) & capacity() - 1;
    for(int id = buckets[b]; id != 0; id = next[id]) {
      if(Token.eq(key, keys[id])) return id;
    }
    return 0;
  }

  /**
   * Returns the key with the specified id.
   * All ids start with {@code 1} instead of {@code 0}.
   * @param id id of the key to return
   * @return key
   */
  public final byte[] key(final int id) {
    return keys[id];
  }

  /**
   * Removes the entry with the specified key.
   * The deletion of keys will lead to empty entries. If {@link #size} is called after
   * deletions, the original number of entries will be returned.
   * @param key key
   * @return id of the deleted key, or {@code 0} if the key did not exist
   */
  public int remove(final byte[] key) {
    final int b = Token.hashCode(key) & capacity() - 1;
    for(int p = 0, id = buckets[b]; id != 0; p = id, id = next[id]) {
      if(!Token.eq(key, keys[id])) continue;
      if(p == 0) buckets[b] = next[id];
      else next[p] = next[next[p]];
      keys[id] = null;
      return id;
    }
    return 0;
  }

  /**
   * Stores the specified key and returns its id, or returns the negative id if the key has already
   * been stored. The public method {@link #add} can be used to check if an added value exists.
   * @param key key to be indexed
   * @return id, or negative id if key has already been stored
   */
  private int index(final byte[] key) {
    final int h = Token.hashCode(key);
    int b = h & capacity() - 1;
    for(int id = buckets[b]; id != 0; id = next[id]) {
      if(Token.eq(key, keys[id])) return -id;
    }
    final int s = size++;
    if(checkCapacity()) b = h & capacity() - 1;
    next[s] = buckets[b];
    keys[s] = key;
    buckets[b] = s;
    return s;
  }

  @Override
  protected final int hashCode(final int id) {
    return Token.hashCode(keys[id]);
  }

  @Override
  protected void rehash(final int newSize) {
    keys = Array.copyOf(keys, newSize);
  }

  @Override
  public void clear() {
    Arrays.fill(keys, null);
    super.clear();
  }

  @Override
  public final Iterator<byte[]> iterator() {
    return new ArrayIterator<>(keys, 1, size);
  }

  /**
   * Returns an array with all elements.
   * @return array
   */
  public final byte[][] toArray() {
    return Arrays.copyOfRange(keys, 1, size);
  }

  @Override
  public String toString() {
    return toString(keys);
  }
}
