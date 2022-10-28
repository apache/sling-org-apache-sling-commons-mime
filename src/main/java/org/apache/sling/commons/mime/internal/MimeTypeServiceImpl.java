/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.mime.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.commons.mime.MimeTypeService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>MimeTypeServiceImpl</code> is the official implementation of the
 * {@link MimeTypeService} interface.
 */
@Component(service = MimeTypeService.class,
    property = {
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
            Constants.SERVICE_DESCRIPTION + "=Apache Sling Commons MIME Type Service"
    })
@Designate(ocd = MimeTypeServiceImpl.Config.class)
public class MimeTypeServiceImpl implements MimeTypeService, BundleListener {

    public static final String CORE_MIME_TYPES = "/META-INF/core_mime.types";

    public static final String MIME_TYPES = "/META-INF/mime.types";
    
    @ObjectClassDefinition(name = "Apache Sling Commons MIME Type Service",
            description = "The Sling Commons MIME Type Service provides support for " +
                "maintaining and configuring MIME Type mappings.")
    public @interface Config {

        @AttributeDefinition(name = "MIME Types",
                description = "Configures additional MIME type mappings in the "+
                 "traditional mime.types file format: Each property is a blank space separated "+
                 "list of strings where the first string is the MIME type and the rest of the "+
                 "strings are filename extensions referring to the MIME type. Using this "+
                 "property additional MIME type mappings may be defined. Existing MIME type "+
                 "mappings cannot be redefined and setting such mappings in this property "+
                 "has no effect. For a list of existing mappings refer to the MIME Types page.")
        String[] mime_types();
    }

    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    private Map<String, String> mimeMap = new HashMap<>();

    private Map<String, String> extensionMap = new HashMap<>();

    private MimeTypeProvider[] typeProviders;

    private List<MimeTypeProvider> typeProviderList = new ArrayList<>();

    // --------- MimeTypeService interface

    @Override
    public String getMimeType(String name) {
        if (name == null) {
            return null;
        }

        String ext = name.substring(name.lastIndexOf('.') + 1);
        ext = ext.toLowerCase();

        String type = this.mimeMap.get(ext);
        if (type == null) {
            MimeTypeProvider[] mtp = this.getMimeTypeProviders();
            for (int i = 0; type == null && i < mtp.length; i++) {
                type = mtp[i].getMimeType(ext);
            }
        }

        return type;
    }

    @Override
    public String getExtension(String mimeType) {
        if (mimeType == null) {
            return null;
        }

        // compare using lowercase only
        mimeType = mimeType.toLowerCase();

        String ext = this.extensionMap.get(mimeType);
        if (ext == null) {
            MimeTypeProvider[] mtp = this.getMimeTypeProviders();
            for (int i = 0; ext == null && i < mtp.length; i++) {
                ext = mtp[i].getExtension(mimeType);
            }
        }
        return ext;
    }

    @Override
    public void registerMimeType(String mimeType, String... extensions) {
        if (mimeType == null || mimeType.length() == 0 || extensions == null
            || extensions.length == 0) {
            return;
        }

        mimeType = mimeType.toLowerCase();

        String defaultExtension = extensionMap.get(mimeType);
        String firstExtension = null;

        for (String extension : extensions) {
            if (extension != null && extension.length() > 0) {
                extension = extension.toLowerCase();
                if(firstExtension == null) {
                    firstExtension = extension;
                }

                String oldMimeType = mimeMap.get(extension);
                if (oldMimeType == null) {

                    log.debug("registerMimeType: Add mapping "
                        + extension + "=" + mimeType);

                    this.mimeMap.put(extension, mimeType);

                    if (defaultExtension == null) {
                        defaultExtension = extension;
                    }

                } else {

                    log.info(
                        "registerMimeType: Ignoring mapping " + extension + "="
                            + mimeType + ": Mapping " + extension + "="
                            + oldMimeType + " already exists");

                }

            }
        }
        addToExtensions(defaultExtension, mimeType, firstExtension);
    }

    private void addToExtensions(String defaultExtension, String mimeType, String extension) {
        if (defaultExtension != null) {
            this.extensionMap.put(mimeType, defaultExtension);
        } else {
            // support multiple mime types to an extension
            this.extensionMap.putIfAbsent(mimeType, extension);
        }
    }

    @Override
    public void registerMimeType(InputStream mimeTabStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            mimeTabStream, "ISO-8859-1"));

        String line;
        while ((line = br.readLine()) != null) {

            // ignore comment lines
            if (line.startsWith("#")) {
                continue;
            }

            registerMimeType(line);
        }
    }

    // ---------- SCR implementation -------------------------------------------

    @Activate
    protected void activate(final BundleContext context, final Config config) {
        context.addBundleListener(this);

        // register core and default sling mime types
        Bundle bundle = context.getBundle();
        registerMimeType(bundle.getEntry(CORE_MIME_TYPES));
        registerMimeType(bundle.getEntry(MIME_TYPES));

        // register maps of existing bundles
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.RESOLVED | Bundle.STARTING
                | Bundle.ACTIVE | Bundle.STOPPING)) != 0
                && bundles[i].getBundleId() != bundle.getBundleId()) {
                this.registerMimeType(bundles[i].getEntry(MIME_TYPES));
            }
        }

        // register configuration properties
        if (config.mime_types() != null) {
            for (final String configType : config.mime_types()) {
                registerMimeType(configType);
            }
        }
    }

    @Deactivate
    protected void deactivate(final BundleContext context) {
        context.removeBundleListener(this);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindMimeTypeProvider(MimeTypeProvider mimeTypeProvider) {
        synchronized (this.typeProviderList) {
            this.typeProviderList.add(mimeTypeProvider);
            this.typeProviders = null;
        }
    }

    protected void unbindMimeTypeProvider(MimeTypeProvider mimeTypeProvider) {
        synchronized (this.typeProviderList) {
            this.typeProviderList.remove(mimeTypeProvider);
            this.typeProviders = null;
        }
    }

    // ---------- BundleListener ----------------------------------------------

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED) {
            try {
                this.registerMimeType(event.getBundle().getEntry(MIME_TYPES));
            } catch (IllegalStateException ie) {
                log.info("bundleChanged: an issue while registering the mime type occurred");
            }
        }
    }

    // ---------- plugin support -----------------------------------------------

    public Map<String, String> getMimeMap() {
        return mimeMap;
    }

    public Map<String, String> getExtensionMap() {
        return extensionMap;
    }

    // ---------- internal -----------------------------------------------------

    private MimeTypeProvider[] getMimeTypeProviders() {
        MimeTypeProvider[] list = this.typeProviders;

        if (list == null) {
            synchronized (this.typeProviderList) {
                this.typeProviders = this.typeProviderList.toArray(new MimeTypeProvider[this.typeProviderList.size()]);
                list = this.typeProviders;
            }
        }

        return list;
    }

    private void registerMimeType(URL mimetypes) {
        if (mimetypes != null) {
            InputStream ins = null;
            try {
                ins = mimetypes.openStream();
                this.registerMimeType(ins);
            } catch (IOException ioe) {
                // log but don't actually care
                log.warn("An error occurred reading "
                    + mimetypes, ioe);
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Splits the <code>line</code> on whitespace an registers the MIME type
     * mappings provided the line contains more than one whitespace separated
     * fields.
     *
     * @throws NullPointerException if <code>line</code> is <code>null</code>.
     */
    private void registerMimeType(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length > 1) {
            String[] extensions = new String[parts.length - 1];
            System.arraycopy(parts, 1, extensions, 0, extensions.length);
            this.registerMimeType(parts[0], extensions);
        }
    }

}