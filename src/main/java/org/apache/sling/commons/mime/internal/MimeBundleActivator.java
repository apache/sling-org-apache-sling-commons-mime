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
package org.apache.sling.commons.mime.internal;

import org.apache.sling.commons.mime.MimeTypeProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeBundleActivator implements BundleActivator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void start(final BundleContext context) throws Exception {
        try {
            context.registerService(MimeTypeProvider.class, 
                new TikaMimeTypeProvider() {}, 
                null);
            logger.info("Apache Tika detected, using it for MIME type detection");
        } catch ( final Throwable t ) {
            // don't care
        }
    }
    
    @Override
    public void stop(final BundleContext context) throws Exception {
        // nothing to do
    }
}
