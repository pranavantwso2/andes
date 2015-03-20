/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.andes.kernel.distruptor.inbound;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.kernel.AndesSubscriptionManager;
import org.wso2.andes.kernel.LocalSubscription;
import org.wso2.andes.kernel.SubscriptionAlreadyExistsException;
import org.wso2.andes.subscription.BasicSubscription;

import java.util.concurrent.ExecutionException;

import static org.wso2.andes.kernel.distruptor.inbound.AndesInboundStateEvent.StateEvent.CLOSE_SUBSCRIPTION_EVENT;
import static org.wso2.andes.kernel.distruptor.inbound.AndesInboundStateEvent.StateEvent.OPEN_SUBSCRIPTION_EVENT;

/**
 * Class to hold information relevant to open and close subscription
 */
public abstract class InboundSubscriptionEvent extends BasicSubscription implements AndesInboundStateEvent, LocalSubscription {

    private static Log log = LogFactory.getLog(InboundSubscriptionEvent.class);

    /**
     * Type of subscription event
     */
    private StateEvent eventType;

    /**
     * Reference to subscription manager to update subscription event 
     */
    private AndesSubscriptionManager subscriptionManager;

    /**
     * Future to wait for subscription open/close event to be completed. Disruptor based async call will become a 
     * blocking call by waiting on this future. 
     * This will assure ordered event processing through Disruptor plus synchronous behaviour through future get call
     * after publishing event to Disruptor.
     */
    private SettableFuture<Boolean> future = SettableFuture.create();

    public InboundSubscriptionEvent(String subscriptionAsStr) {
        super(subscriptionAsStr);
    }

    public InboundSubscriptionEvent(String subscriptionID, String destination, boolean isBoundToTopic, 
                                    boolean isExclusive, boolean isDurable, String subscribedNode, 
                                    long subscribeTime, String targetQueue, String targetQueueOwner, 
                                    String targetQueueBoundExchange, String targetQueueBoundExchangeType, 
                                    Short isTargetQueueBoundExchangeAutoDeletable, boolean hasExternalSubscriptions) {
        
        super(subscriptionID, destination, isBoundToTopic, isExclusive, isDurable, subscribedNode, subscribeTime, 
                targetQueue, targetQueueOwner, targetQueueBoundExchange, targetQueueBoundExchangeType, 
                isTargetQueueBoundExchangeAutoDeletable, hasExternalSubscriptions);
    }

    @Override
    public void updateState() throws AndesException {
        switch (eventType) {
            case OPEN_SUBSCRIPTION_EVENT:
                handleOpenSubscriptionEvent();
                break;
            case CLOSE_SUBSCRIPTION_EVENT:
                handleCloseSubscriptionEvent();
                break;
            default:
                log.error("Event type not set properly " + eventType);
                break;
        }
    }

    private void handleCloseSubscriptionEvent() {
        boolean isComplete = false;
        try {
            subscriptionManager.closeLocalSubscription(this);
            isComplete = true;
        } catch (AndesException e) {
            future.setException(e);
            log.error("Error occurred while closing subscription. Subscription id "
                    + getSubscriptionID(), e);
        } finally {
            future.set(isComplete);
        }
    }

    private void handleOpenSubscriptionEvent() {
        boolean isComplete = false;
        try {
            subscriptionManager.addSubscription(this);
            isComplete = true;
        } catch (AndesException e) {
            future.setException(e);
            log.error("Error occurred while adding subscription. Subscription id "
                    + getSubscriptionID(), e);
        } catch (SubscriptionAlreadyExistsException e) {
            // exception will be handled by receiver
            future.setException(e);
        } finally {
            future.set(isComplete);
        }
    }

    public void prepareForNewSubscription(AndesSubscriptionManager subscriptionManager) {
        eventType = OPEN_SUBSCRIPTION_EVENT;
        this.subscriptionManager = subscriptionManager;
    }
    
    public void prepareForCloseSubscription(AndesSubscriptionManager subscriptionManager) {
        eventType = CLOSE_SUBSCRIPTION_EVENT;
        this.subscriptionManager = subscriptionManager;
    }
    
    public boolean waitForCompletion() throws SubscriptionAlreadyExistsException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SubscriptionAlreadyExistsException) {
                throw (SubscriptionAlreadyExistsException) e.getCause();
            } else {
                // No point in throwing an exception here and disrupting the server. A warning is sufficient.
                log.warn("Error occurred while processing event '" + eventType  + "' for subscription id "
                        + getSubscriptionID());
            }
        }
        return false;
    }

    @Override
    public StateEvent getEventType() {
        return eventType;
    }
}
