/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.langchain4j;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class ChatMessageEncoder implements Encoder.Text<String> {

    @Override
    public String encode(String message) throws EncodeException {
       
        if (!message.endsWith(".")) {
            message += " ...";
        }
        
        Parser parser = Parser.builder().build();
        Node document = parser.parse(message);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        message = renderer.render(document);
        return message;

    }

}
