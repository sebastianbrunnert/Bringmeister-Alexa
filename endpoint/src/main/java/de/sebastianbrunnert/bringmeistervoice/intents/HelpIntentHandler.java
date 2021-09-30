package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Nutzer Hilfe benötigt.
 *
 * @author Sebastian Brunnert
 */
public class HelpIntentHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("AMAZON.HelpIntent"));
    }

    /**
     * Die Anfrage kann verarbeitet werden. Es wird eine statische Hilfe-Nachricht ausgegeben.
     *
     * @param handlerInput
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech("Du hast viele Möglichkeiten: Du kannst ein Produkt in deinen Warenkorb hinzufügen, indem du beispielsweise sagt: Alexa, Füge Bananen in meinen Warenkorb hinzu. Du kannst aber auch Produkte löschen oder nach den neusten Angeboten fragen.")
                .withShouldEndSession(false)
                .build();
    }
}
