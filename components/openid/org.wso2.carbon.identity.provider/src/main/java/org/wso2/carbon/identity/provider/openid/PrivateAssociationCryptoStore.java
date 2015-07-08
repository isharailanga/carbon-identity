/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.provider.openid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.openid4java.util.OpenID4JavaUtils;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.Random;

/**
 * This is the custom AssociationStore that would read the encrypted association from the openid request
 */
public class PrivateAssociationCryptoStore extends InMemoryServerAssociationStore {

    private int storeId = 0;
    private int counter;
    private int expireIn;

    private String serverKey = "jscascasjcwt3276432yvdqwd";

    private static Log log = LogFactory.getLog(PrivateAssociationCryptoStore.class);

    public PrivateAssociationCryptoStore() {
        storeId = new Random().nextInt(9999);
        counter = 0;
        String serverKey = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_PRIVATE_ASSOCIATION_SERVER_KEY);
        if(serverKey != null && serverKey.trim().length() > 0){
            this.serverKey = serverKey;
        }
    }

    @Override
    public Association load(String handle) {

        SecretKey secretKey = null;
        try{
//            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//            PBEKeySpec spec = new PBEKeySpec(serverKey.toCharArray(), handle.getBytes(), 1, 256);
//
//            secretKey = factory.generateSecret(spec);
            secretKey = new SecretKeySpec((serverKey + handle).getBytes("UTF-8"), "HmacSHA256");
        } catch (Exception e){
            log.error("Error while generating secret key", e);
        }

        String timeStamp = handle.substring((Integer.toString(storeId)).length(), handle.indexOf("-"));
        Date expireDate = new Date(Long.parseLong(timeStamp)+ this.expireIn);

        if(secretKey != null){
            return new Association(Association.TYPE_HMAC_SHA256, handle, secretKey, expireDate);
        }

        return null;
    }


    @Override
    public Association generate(String type, int expiryIn) throws AssociationException {

        long timestamp = new Date().getTime();

        String handle = storeId + timestamp + "-" + counter++;

        if(this.expireIn == 0){
            // make time in to millisecond before it is set
            this.expireIn = expiryIn*1000;
        }

        SecretKey secretKey = null;
        try{
//            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//            PBEKeySpec spec = new PBEKeySpec(serverKey.toCharArray(), handle.getBytes(), 1, 256);
//
//            secretKey = factory.generateSecret(spec);
            secretKey = new SecretKeySpec((serverKey + handle).getBytes("UTF-8"), "HmacSHA256");
        } catch (Exception e){
            log.error("Error while generating secret key", e);
        }

        Date expireDate = new Date(timestamp + this.expireIn);

        if(secretKey != null){
            Association association = new Association(Association.TYPE_HMAC_SHA256, handle, secretKey, expireDate);
            OpenID4JavaUtils.setThreadLocalAssociation(association);
            return association;
        }

        return null;
    }
}
