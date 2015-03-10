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

import org.apache.qpid.amqp_1_0.jms.QueueConnection;

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.ServerSessionPool;

public class QueueConnectionImpl extends ConnectionImpl implements QueueConnection
{
    QueueConnectionImpl(String host, int port, String username, String password, String clientId)
            throws JMSException
    {
        super(host, port, username, password, clientId);
    }

    public QueueSessionImpl createQueueSession(final boolean b, final int i) throws JMSException
    {
        return null;  //TODO
    }

    public ConnectionConsumer createConnectionConsumer(final Queue queue,
                                                       final String s,
                                                       final ServerSessionPool serverSessionPool,
                                                       final int i) throws JMSException
    {
        return null;  //TODO
    }
}
