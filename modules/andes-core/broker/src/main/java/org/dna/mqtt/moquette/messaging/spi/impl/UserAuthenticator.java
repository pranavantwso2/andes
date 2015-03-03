/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dna.mqtt.moquette.server.IAuthenticator;

/**
 * This is the class used to authenticate MQTT activities against users. The configured users are used temporarily.
 * Once proper authentication model is bought for MQTT, this should support working with the carbon authentication
 * model.
 */
@Deprecated()
public class UserAuthenticator implements IAuthenticator {

    private Map<String, String> users = new HashMap<String, String>();
    
    UserAuthenticator() {

        /*List<String> list = AndesConfigurationManager.readValueList(AndesConfiguration.LIST_TRANSPORTS_MQTT_USERNAMES);

        for (int i =1; i<list.size(); i++) {
            String userName = AndesConfigurationManager.readValueOfChildByIndex(AndesConfiguration.TRANSPORTS_MQTT_USERNAME, i);
            String password = AndesConfigurationManager.readValueOfChildByIndex(AndesConfiguration.TRANSPORTS_MQTT_PASSWORD, i);

            users.put(userName, password);
        }
        */
    }
    
    public boolean checkValid(String username, String password) {
        String foundPwq = users.get(username);
        return !StringUtils.isBlank(foundPwq) && foundPwq.equals(password);
    }
    
}
