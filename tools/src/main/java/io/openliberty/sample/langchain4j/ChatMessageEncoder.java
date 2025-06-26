package io.openliberty.sample.langchain4j;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

public class ChatMessageEncoder implements Encoder.Text<String> {

    @Override
    public String encode(String message) throws EncodeException {

        if (!message.endsWith(".")) {
            message += " ...";
        }

        message = message.replaceAll("\n", "<br/>");

        return message;

    }

}
