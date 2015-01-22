/**
 * Transaction signers know how to calculate signatures over transactions in different contexts, for example, using
 * local private keys or fetching them from remote servers. The {@link org.spreadcoinj.core.Wallet} class uses these
 * when sending money.
 */
package org.spreadcoinj.signers;