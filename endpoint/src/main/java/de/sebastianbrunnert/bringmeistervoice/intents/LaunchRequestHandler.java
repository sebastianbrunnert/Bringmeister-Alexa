package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Skill gestartet wird.
 *
 * @author Sebastian Brunnert
 */
public class LaunchRequestHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(requestType(LaunchRequest.class));
    }

    /**
     * Die Anfrage kann verarbeitet werden. Es wird eine statische Begrüßungs-Nachricht ausgegeben.
     *
     * @param handlerInput
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
        try {
            return handlerInput.getResponseBuilder()
                    .withSpeech("Willkommen bei Bringmeister. Du kannst Produkte in deinen Warenkorb hinzufügen und noch vieles mehr. Wenn du Hilfe benötigst, stehe Ich für dich bereit.")
                    .withSimpleCard("Bringmeister", BringmeisterVoiceMaster.getInstance().getDataService().getDeal(BringmeisterVoiceMaster.getInstance().getUserService().getZipCode(requestHelper.getAccountLinkingAccessToken())))
                    .withReprompt("Wenn du Hilfe benötigst, stehe Ich für dich bereit.")
                    .withShouldEndSession(false)
                    .build();
        } catch (WrongCredentialsException e) {
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(true)
                    .withSpeech("Entschuldigung. Du bist nicht mit Bringmeister verbunden. Bitte erledige das in der Alexa App.")
                    .build();
        }
    }
}
