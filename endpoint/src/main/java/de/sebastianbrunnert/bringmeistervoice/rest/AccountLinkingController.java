package de.sebastianbrunnert.bringmeistervoice.rest;

import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.User;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring Boot Controller, der für das Account Linking seitens Alexa verwendet wird.
 *
 * @author Sebastian Brunnert
 */
@Controller
@RequestMapping("account")
public class AccountLinkingController {

    /**
     * Instanz der Hauptklasse
     */
    @Autowired
    private BringmeisterVoiceMaster bringmeisterVoiceMaster;

    /**
     * Diese Seite wird aufgerufen, wenn der Nutzer in der Alexa App sein Benutzerkonto verbinden möchte.
     * Es wird das Template login_form.html dargestellt (besteht lediglich aus einem Formular).
     * Die benötigten Daten werden im Spring Model gesichert.
     *
     * @param state Wird von Alexa selbst angegeben, um die Anfrage zu identifizieren
     * @param redirectUri Wird von Alexa selbst angeben, damit bei erfolgreicher Anmeldung die Anfrage übermittelt wird
     * @param model Für Spring Boot, damit Daten gesichert werden können
     * @return
     */
    @GetMapping("login")
    public String showLoginForm(@RequestParam("state") String state, @RequestParam("redirect_uri") String redirectUri, Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("state", state);
        model.addAttribute("redirectUri", redirectUri);
        return "login_form";
    }

    /**
     * Der Nutzer hat nun das Formular ausgefüllt.
     * Die angegebenen Daten werden nun an die Bringmeister-Server weitergeleitet.
     * War die Anmeldung erfolgreich, wird der verschlüsselte Token an Alexa weitergeleitet.
     *
     * @param state Wird von Alexa selbst angegeben, um die Anfrage zu identifizieren
     * @param redirectUri Wird von Alexa selbst angeben, damit bei erfolgreicher Anmeldung die Anfrage übermittelt wird
     * @param user Die angebenen Benutzerdaten des Nutzers, die im Formular angegeben wurden
     * @param model Für Spring Boot, damit Daten gesichert werden können
     * @return
     */
    @PostMapping("login")
    public ModelAndView login(@RequestParam("state") String state, @RequestParam("redirect_uri") String redirectUri, @ModelAttribute User user, Model model) {
        String alert = null;
        String code = "";
        if(user == null || user.getName() == null || user.getPassword() == null) {
            alert = "Bitte fülle alle Felder aus.";
        }

        try {
            code = bringmeisterVoiceMaster.getUserService().encryptToken(bringmeisterVoiceMaster.getUserService().getToken(user));
        } catch (WrongCredentialsException e) {
            alert = "Falsche Anmeldedaten.";
        }

        if(alert != null) {
            model.addAttribute("user", new User());
            model.addAttribute("state", state);
            model.addAttribute("redirectUri", redirectUri);
            model.addAttribute("alert", alert);
            return new ModelAndView("login_form");
        }

        return new ModelAndView("redirect:" + redirectUri + "?state=" + state + "&code=" + code);
    }

    /**
     * Diesen Endpoint frage Alexa an, wenn der Token in der Intent angegeben werden soll.
     *
     * @param code verschlüsselter Token, der an Alexa gegeben werden kann
     * @return nicht-verschlüsselter Token, der vom Backend zurückgeben wurde
     */
    @PostMapping("token")
    public ResponseEntity<Object> token(@RequestParam("code") String code) {
        return new ResponseEntity<>(BringmeisterVoiceMaster.getInstance().getObjectMapper().createObjectNode()
                .put("access_token", this.bringmeisterVoiceMaster.getUserService().decryptToken(code)), HttpStatus.OK);
    }

}
