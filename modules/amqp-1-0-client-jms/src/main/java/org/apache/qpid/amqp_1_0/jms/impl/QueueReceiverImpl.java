/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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
package org.apache.qpid.amqp_1_0.jms.impl;

import javax.jms.JMSException;
import org.apache.qpid.amqp_1_0.client.ConnectionErrorException;
import org.apache.qpid.amqp_1_0.client.Receiver;
import org.apache.qpid.amqp_1_0.jms.Queue;
import org.apache.qpid.amqp_1_0.jms.QueueReceiver;

import java.util.UUID;

public class QueueReceiverImpl extends MessageConsumerImpl implements QueueReceiver
{
    QueueReceiverImpl(final QueueImpl destination,
                      final SessionImpl session,
                      final String selector,
                      final boolean noLocal)
            throws JMSException
    {
        super(destination, session, selector, noLocal);
        setQueueConsumer(true);
    }

    protected Receiver createClientReceiver() throws JMSException
    {
        try
        {
            final String targetAddr =
                    getDestination().getLocalTerminus() != null ? getDestination().getLocalTerminus() : UUID
                            .randomUUID().toString();
            return getSession().getClientSession().createMovingReceiver(getSession().toAddress(getDestination()),
                                                                        targetAddr);
        }
        catch (ConnectionErrorException e)
        {
            throw new JMSException(e.getMessage(), e.getRemoteError().getCondition().toString());
        }
    }

    public Queue getQueue() throws JMSException
    {
        return (QueueImpl) getDestination();
    }

}
