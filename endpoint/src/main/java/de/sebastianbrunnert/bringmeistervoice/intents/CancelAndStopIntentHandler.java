package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.SessionEndedRequest;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amazon.ask.request.Predicates.requestType;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Nutzer den Skill schließt.
 *
 * @author Sebastian Brunnert
 */
public class CancelAndStopIntentHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(requestType(SessionEndedRequest.class)
                .or(intentName("AMAZON.StopIntent"))
                .or(intentName("AMAZON.CancelIntent"))
                .or(intentName("AMAZON.NavigateHomeIntent")));
    }

    /**
     * Die Anfrage kann verarbeitet werden. Es wird eine statische Verabschiedungs-Nachricht ausgegeben.
     *
     * @param handlerInput
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Auf Wiedersehen!")
                .withShouldEndSession(true)
                .build();
    }

}
