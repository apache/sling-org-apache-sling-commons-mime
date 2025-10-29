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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class MimeTypeWebConsolePluginTest {

    private void loadMimeTypes(MimeTypeServiceImpl service, String path) throws IOException {
        InputStream ins = this.getClass().getResourceAsStream(path);
        assertNotNull(ins);

        try {
            service.registerMimeType(ins);
        } finally {
            try {
                ins.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Test method for {@link org.apache.sling.commons.mime.internal.MimeTypeWebConsolePlugin#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void testDoGet() throws IOException {
        final MimeTypeServiceImpl service = new MimeTypeServiceImpl();
        loadMimeTypes(service, MimeTypeServiceImpl.CORE_MIME_TYPES);

        final MimeTypeWebConsolePlugin plugin = new MimeTypeWebConsolePlugin(service);
        assertNotNull(plugin);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        plugin.doGet(request, response);
        pw.flush();
        String output = sw.toString();

        // Assert: basic expected content is present
        assertTrue("Output should contain a table element", output.contains("<table id='mimetabtable'"));
    }
}
