// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

public enum ConfigOverrideDistributionStrategy {
  DYNAMIC, ON_RESTART;

  public static final ConfigOverrideDistributionStrategy DEFAULT = DYNAMIC;
}