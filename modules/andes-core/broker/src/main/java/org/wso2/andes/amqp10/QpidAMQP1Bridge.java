/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.andes.amqp10;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the mediation point between Qpid and AMQP1.0.
 * Any interaction should be mediated through this bridge
 */
public class QpidAMQP1Bridge {
    private static Log log = LogFactory.getLog(QpidAMQP1Bridge.class);
    private static QpidAMQP1Bridge qpidAMQP1Bridge = null;

    /**
     * get QpidAMQPBridge instance
     *
     * @return QpidAMQPBridge instance
     */
    public static synchronized QpidAMQP1Bridge getInstance() {
        if (qpidAMQP1Bridge == null) {
            qpidAMQP1Bridge = new QpidAMQP1Bridge();
        }
        return qpidAMQP1Bridge;
    }



}
