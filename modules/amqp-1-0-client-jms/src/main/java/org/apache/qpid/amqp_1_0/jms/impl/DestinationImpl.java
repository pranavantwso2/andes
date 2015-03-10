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

import org.apache.qpid.amqp_1_0.jms.Destination;
import org.apache.qpid.amqp_1_0.jms.Queue;
import org.apache.qpid.amqp_1_0.jms.Topic;

import javax.jms.JMSException;
import java.util.UUID;
import java.util.WeakHashMap;

public class DestinationImpl implements Destination
{
    private static final WeakHashMap<String, DestinationImpl> DESTINATION_CACHE =
            new WeakHashMap<String, DestinationImpl>();

    private final String _address;
    private String _localTerminus;

    protected DestinationImpl(String address)
    {
        _address = address;
    }

    public String getAddress()
    {
        return _address;
    }

    public static DestinationImpl valueOf(String address)
    {
        return address == null ? null : createDestination(address);
    }

    @Override
    public int hashCode()
    {
        return _address.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        return obj != null
               && obj.getClass() == getClass()
               && _address.equals(((DestinationImpl)obj)._address);
    }

    public static synchronized DestinationImpl createDestination(String address)
    {
        DestinationImpl destination;
        if (address.endsWith("!!"))
        {
            address = address.substring(0, address.length() - 2);
            String localTerminusName = UUID.randomUUID().toString();
            destination = new DestinationImpl(address);
            destination.setLocalTerminus(localTerminusName);
        }
        else
        {
            destination = DESTINATION_CACHE.get(address);
            if (destination == null)
            {
                destination = new DestinationImpl(address);
                DESTINATION_CACHE.put(address, destination);
            }
        }
        return destination;
    }

    public String getQueueName() throws JMSException
    {
        return getAddress();
    }

    public String getTopicName() throws JMSException
    {
        return getAddress();
    }

    void setLocalTerminus(final String localTerminus)
    {
        _localTerminus = localTerminus;
    }

    String getLocalTerminus()
    {
        return _localTerminus;
    }
}
