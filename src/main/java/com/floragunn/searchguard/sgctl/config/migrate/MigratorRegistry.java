package com.floragunn.searchguard.sgctl.config.migrate;

import com.floragunn.fluent.collections.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** A singleton registry for managing sub-migrators. */
public class MigratorRegistry {
  private static MigratorRegistry instance;
  private final List<SubMigrator> subMigrators = new ArrayList<>();
  private boolean isFinalized = false;

  private MigratorRegistry() {}

  /**
   * Singleton get instance of the MigratorRegistry.
   *
   * @return The instance of the MigratorRegistry.
   */
  public static MigratorRegistry getInstance() {
    if (instance == null) {
      instance = new MigratorRegistry();
    }
    return instance;
  }

  /**
   * Gets the list of sub-migrators. Throws an exception if the registry has not been finalized.
   *
   * @return The list of sub-migrators.
   */
  public List<SubMigrator> getSubMigrators() {
    if (isFinalized) {
      return ImmutableList.of(subMigrators);
    } else {
      throw new IllegalStateException("Migrator registry has not been finalized!");
    }
  }

  /**
   * Gets the list of sub-migrators statically. Throws an exception if the registry has not been
   * finalized.
   *
   * @return The list of sub-migrators.
   */
  public static List<SubMigrator> getSubMigratorsStatic() {
    instance = MigratorRegistry.getInstance();
    return instance.getSubMigrators();
  }

  /**
   * Registers a sub-migrator statically.
   *
   * @param subMigrator The sub-migrator to register.
   */
  public static void registerSubMigratorStatic(SubMigrator subMigrator) {
    instance = MigratorRegistry.getInstance();
    instance.registerSubMigrator(subMigrator);
  }

  /**
   * Registers a sub-migrator.
   *
   * @param subMigrator The sub-migrator to register.
   */
  public void registerSubMigrator(SubMigrator subMigrator) {
    if (isFinalized) {
      throw new IllegalStateException("Migrator registry has already been finalized!");
    }
    subMigrators.add(subMigrator);
  }

  /**
   * Unregisters a sub-migrator statically.
   *
   * @param subMigrator The sub-migrator to unregister.
   */
  public static void unregisterSubMigratorStatic(SubMigrator subMigrator) {
    instance = MigratorRegistry.getInstance();
    instance.unregisterSubMigrator(subMigrator);
  }

  /**
   * Unregisters a sub-migrator.
   *
   * @param subMigrator The sub-migrator to unregister.
   */
  public void unregisterSubMigrator(SubMigrator subMigrator) {
    if (isFinalized) {
      throw new IllegalStateException("Migrator registry has already been finalized!");
    }
    subMigrators.remove(subMigrator);
  }

  /** Finalizes the sub-migrator registry statically. */
  public static void finalizeMigratorsStatic() {
    instance = MigratorRegistry.getInstance();
    instance.finalizeSubMigrators();
  }

  /** Finalizes the sub-migrators. */
  public void finalizeSubMigrators() {
    isFinalized = true;
    // TODO: Finalize
  }

  /** Attempts to un-finalize sub-migrator registry statically. */
  public static void unfinalizeMigratorsStatic() {
    instance = MigratorRegistry.getInstance();
    instance.unfinalizeMigrators();
  }

  /** Attempts to un-finalize sub-migrator registry. */
  public void unfinalizeMigrators() {
    if (subMigrators.isEmpty()) {
      isFinalized = false;
    } else {
      throw new IllegalStateException("Migrator registry can not be unfinalized!");
    }
  }

  /** Attempts to reset sub-migrator registry. */
  public void reset() {
    if (subMigrators.isEmpty() || !isFinalized) {
      throw new IllegalStateException(
          "Migrator registry can not be reset! Is already reset or not finalized!"
              + "Did you forget to finalize?");
    }
    subMigrators.clear();
    unfinalizeMigratorsStatic();
  }

  /**
   * Checks if the migrators are finalized statically.
   *
   * @return True if the migrators are finalized, false otherwise.
   */
  public static boolean isFinalizedStatic() {
    instance = MigratorRegistry.getInstance();
    return instance.isFinalized();
  }

  /**
   * Checks if the sub-migrators are finalized.
   *
   * @return True if the sub-migrators are finalized, false otherwise.
   */
  public boolean isFinalized() {
    return isFinalized;
  }
}
