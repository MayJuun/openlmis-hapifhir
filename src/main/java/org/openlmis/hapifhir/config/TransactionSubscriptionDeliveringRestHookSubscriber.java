/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.hapifhir.config;

import ca.uhn.fhir.jpa.subscription.module.subscriber.SubscriptionDeliveringRestHookSubscriber;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Primary
@Component
@Scope("prototype")
class TransactionSubscriptionDeliveringRestHookSubscriber
    extends SubscriptionDeliveringRestHookSubscriber {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TransactionSubscriptionDeliveringRestHookSubscriber.class);

  private PlatformTransactionManager transactionManager;

  TransactionSubscriptionDeliveringRestHookSubscriber(
      PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
    LOGGER.trace("Created TransactionSubscriptionDeliveringRestHookSubscriber instance");

  }

  @Override
  public void handleMessage(Message message) {
    // workaround for the problem which has been described in the following link:
    // https://groups.google.com/forum/#!topic/hapi-fhir/Hm2I3UPACCw
    LOGGER.trace("Create transaction definition");
    TransactionDefinition definition = new DefaultTransactionDefinition();

    LOGGER.debug("Get/create transaction");
    TransactionStatus transaction = transactionManager.getTransaction(definition);

    try {
      LOGGER.debug("Handling message");
      parentHandleMessage(message);

      if (!transaction.isCompleted() && !transaction.isRollbackOnly()) {
        LOGGER.debug("Commit transaction");
        transactionManager.commit(transaction);
      }

      LOGGER.debug("Handled message");
    } catch (Exception exp) {
      if (!transaction.isCompleted()) {
        LOGGER.debug("Rollback transaction");
        transactionManager.rollback(transaction);
      }

      LOGGER.error("Exception has been thrown while message has been handled", exp);
      throw exp;
    }
  }

  @VisibleForTesting
  void parentHandleMessage(Message<?> message) {
    super.handleMessage(message);
  }

}
