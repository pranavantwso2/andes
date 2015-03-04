
/*
*
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
*
*/


package org.apache.qpid.proton.amqp.security;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;

public final class SaslInit
      implements SaslFrameBody
{

    private Symbol _mechanism;
    private Binary _initialResponse;
    private String _hostname;

    public Symbol getMechanism()
    {
        return _mechanism;
    }

    public void setMechanism(Symbol mechanism)
    {
        if( mechanism == null )
        {
            throw new NullPointerException("the mechanism field is mandatory");
        }

        _mechanism = mechanism;
    }

    public Binary getInitialResponse()
    {
        return _initialResponse;
    }

    public void setInitialResponse(Binary initialResponse)
    {
        _initialResponse = initialResponse;
    }

    public String getHostname()
    {
        return _hostname;
    }

    public void setHostname(String hostname)
    {
        _hostname = hostname;
    }


    public <E> void invoke(SaslFrameBodyHandler<E> handler, Binary payload, E context)
    {
        handler.handleInit(this, payload, context);
    }

    @Override
    public String toString()
    {
        return "SaslInit{" +
               "mechanism=" + _mechanism +
               ", initialResponse=" + _initialResponse +
               ", hostname='" + _hostname + '\'' +
               '}';
    }
}
