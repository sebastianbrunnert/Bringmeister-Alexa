package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn kein anderer Handler eine Intent bearbeit.
 *
 * @author Sebastian Brunnert
 */
public class UnhandledHandler implements IntentRequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return true;
    }

    /**
     * Die Anfrage kann verarbeitet werden.
     *
     * @param handlerInput
     * @param intentRequest
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.getResponseBuilder().withShouldEndSession(false).build();
    }
}
