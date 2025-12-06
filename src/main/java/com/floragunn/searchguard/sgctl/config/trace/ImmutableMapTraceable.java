package com.floragunn.searchguard.sgctl.config.trace;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Read-only map whose keys are Traceable<K> but which allows lookup by the inner K value.
 *
 * @param <K> key inner type
 * @param <V> value inner type
 */
public final class ImmutableMapTraceable<K, V> extends ForwardingMap<Traceable<K>, Traceable<V>> {

  private final ImmutableMap<Traceable<K>, Traceable<V>> delegate;
  private final ImmutableMap<K, Traceable<V>> indexByInnerKey;

  private ImmutableMapTraceable(
      ImmutableMap<Traceable<K>, Traceable<V>> delegate,
      ImmutableMap<K, Traceable<V>> indexByInnerKey) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.indexByInnerKey = Objects.requireNonNull(indexByInnerKey, "indexByInnerKey");
  }

  @Override
  protected Map<Traceable<K>, Traceable<V>> delegate() {
    return delegate;
  }

  /**
   * Build from an ImmutableMap whose keys are Traceable<K>.
   *
   * <p>Throws IllegalArgumentException if two different Traceable keys have the same inner K value,
   * or if any inner key is null.
   */
  public static <K, V> ImmutableMapTraceable<K, V> copyOf(
      ImmutableMap<Traceable<K>, Traceable<V>> src) {

    // use temp map to detect duplicates and then build an ImmutableMap
    Map<K, Traceable<V>> temp = new HashMap<>(src.size());
    for (Map.Entry<Traceable<K>, Traceable<V>> e : src.entrySet()) {
      Traceable<K> tKey = e.getKey();
      Traceable<V> tVal = e.getValue();
      K inner = tKey.get();
      if (inner == null) {
        throw new IllegalArgumentException(
            "Traceable key contains null inner value for key: " + tKey);
      }
      Traceable<V> prev = temp.put(inner, tVal);
      if (prev != null && !prev.equals(tVal)) {
        throw new IllegalArgumentException(
            "Duplicate inner key '" + inner + "' from different Traceable keys");
      }
    }

    ImmutableMap<K, Traceable<V>> index = ImmutableMap.copyOf(temp);
    return new ImmutableMapTraceable<>(src, index);
  }

  /** Convenience to build from any Map by copying to ImmutableMap first. */
  public static <K, V> ImmutableMapTraceable<K, V> copyOf(Map<Traceable<K>, Traceable<V>> src) {
    return copyOf(ImmutableMap.copyOf(src));
  }

  /** Typed lookup by the inner key. */
  public @Nullable Traceable<V> getInner(K key) {
    return indexByInnerKey.get(key);
  }

  /** Typed contains by the inner key. */
  public boolean containsInnerKey(@Nullable K key) {
    return indexByInnerKey.containsKey(key);
  }

  /** Return Immutable backing map. */
  public ImmutableMap<Traceable<K>, Traceable<V>> asImmutableMap() {
    return delegate;
  }
}
