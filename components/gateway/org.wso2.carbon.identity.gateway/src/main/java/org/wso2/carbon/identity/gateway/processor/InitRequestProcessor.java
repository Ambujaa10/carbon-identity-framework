/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.gateway.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.framework.IdentityProcessor;
import org.wso2.carbon.identity.framework.cache.IdentityMessageContextCache;
import org.wso2.carbon.identity.framework.FrameworkException;
import org.wso2.carbon.identity.framework.message.IdentityRequest;
import org.wso2.carbon.identity.framework.message.IdentityResponse;
import org.wso2.carbon.identity.gateway.element.authentication.handler.MultiStepAuthenticationHandler;
import org.wso2.carbon.identity.gateway.element.authentication.handler.BasicAuthenticationHandler;
import org.wso2.carbon.identity.gateway.element.response.SAMLResponseHandler;
import org.wso2.carbon.identity.gateway.handler.util.SessionDataCleanupHandler;
import org.wso2.carbon.identity.gateway.element.validation.SAMLValidationHandler;

import java.util.HashMap;


/*
    This processor handler the initial identity requests that comes to the Identity Gateway.
 */
public class InitRequestProcessor extends IdentityProcessor {

    private static final Logger log = LoggerFactory.getLogger(InitRequestProcessor.class);

    @SuppressWarnings("unchecked")
    @Override
    public IdentityResponse process(IdentityRequest identityRequest) throws FrameworkException {

        if (log.isDebugEnabled()) {
            log.debug(getName() + " starting to process the initial Identity Request.");
        }

        // Build the message context
        MessageContext context = new MessageContext(identityRequest, new IdentityResponse());
        context.setInitialIdentityRequest(identityRequest);

        // Create a sessionDataKey and add it to the message context
        String sessionDataKey = context.getSessionDataKey();

        // add the context reference to the cache.
        IdentityMessageContextCache.getInstance().put(sessionDataKey, context);

        // Add an authentication context map TODO: this should be initialized by the context
        context.addParameter(sessionDataKey, new HashMap<>());

        /*
            Handler Chain Begin
        */
        SAMLValidationHandler samlValidationHandler = new SAMLValidationHandler();

        MultiStepAuthenticationHandler multiStepAuthenticationHandler = new MultiStepAuthenticationHandler();
        BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler();

        SAMLResponseHandler samlResponseHandler = new SAMLResponseHandler();
        SessionDataCleanupHandler cleanupHandler = new SessionDataCleanupHandler();

        samlValidationHandler.setNextHandler(multiStepAuthenticationHandler);
        multiStepAuthenticationHandler.addIdentityGatewayEventHandler(basicAuthenticationHandler);
        basicAuthenticationHandler.setNextHandler(samlResponseHandler);
        samlResponseHandler.setNextHandler(cleanupHandler);

        /*
            Handler Chain End
         */

        // start executing the handler element
        samlValidationHandler.execute(context);
        return context.getIdentityResponse();
    }

    @Override
    public String getName() {

        return "InitialRequestProcessor";
    }

    @Override
    public int getPriority() {

        return 100;
    }

    @Override
    public boolean canHandle(IdentityRequest identityRequest) {

        return true;
    }
}
