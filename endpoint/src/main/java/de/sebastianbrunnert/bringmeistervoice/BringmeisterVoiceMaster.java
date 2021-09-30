package de.sebastianbrunnert.bringmeistervoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sebastianbrunnert.bringmeistervoice.services.DataService;
import de.sebastianbrunnert.bringmeistervoice.services.UserService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * Hauptverwaltungsklasse des Projektes.
 * Hier werden sämtliche Instanzen von Services oder Tools gespeichert, die öfters benötigt werden.
 * Auch statische Daten werden hier gesichert.
 *
 * @author Sebastian Brunnert
 */
@SpringBootApplication
@Component("BringmeisterVoiceMaster")
public class BringmeisterVoiceMaster {

    /**
     * Instanz der Hauptklasse selbst, die durch Spring Boot erstellt wird
     */
    @Getter
    private static BringmeisterVoiceMaster instance;

    /**
     * Logger BringmeisterVoice-master, der freigeschaltet wurde
     */
    @Getter
    private Logger logger;

    /**
     * Um Json-Daten zu lesen oder zu bearbeiten
     */
    @Getter
    private ObjectMapper objectMapper;

    /**
     * Service zur Kommunikation über GraphQL mit den Bringmeister-Servern bezügich Benutzer-bezogenen Aktionen
     */
    @Getter @Autowired
    private UserService userService;

    /**
     * Service zur Kommunikation über REST API mit den Bringmeister-Servern bezüglich statischer Daten.
     */
    @Getter @Autowired
    private DataService dataService;

    /**
     * Symmetrischer AES-Schlüssel zum verschlüsseln der Backend Tokens
     */
    @Getter
    private SecretKey secretKey;

    /**
     * Schlüssel, um auf Wetterdaten zugreifen zu können.
     */
    @Getter
    private String openWeatherMapKey = "d0b0e96f4c09c145dfd8dd1e0d82f5e0";

    /**
     * Hauptmethode
     * Hier wird Spring Boot selbst gestartet mit gewisser Properties, die u.a. die Konsole verschönern sollen.
     *
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BringmeisterVoiceMaster.class);

        Properties properties = new Properties();

        properties = new Properties();
        properties.setProperty("spring.main.banner-mode", "OFF");
        properties.setProperty("logging.level.root", "ERROR");
        properties.setProperty("logging.level.org.springframework", "ERROR");
        properties.setProperty("logging.level.BringmeisterVoice-master", "INFO");
        properties.setProperty("logging.pattern.console","%clr(%d{yy-MM-dd E HH:mm:ss.SSS}){blue} %clr(%-5p) %clr(%logger{0}){blue} %clr(%m){faint}%n");

        application.setDefaultProperties(properties);
        long startupTime = application.run(args).getStartupDate();

        getInstance().getLogger().info("Started BringmeisterVoice-master in " + (System.currentTimeMillis()-startupTime) + "ms.");
    }

    /**
     * Konstruktor, der durch Spring Boot aufgerufen wird
     *
     * Hier werden die Instanzen der Variablen der Hauptklasse erzeugt
     */
    public BringmeisterVoiceMaster() {
        BringmeisterVoiceMaster.instance = this;

        this.logger = LoggerFactory.getLogger("BringmeisterVoice-master");
        this.objectMapper = new ObjectMapper();

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);

            this.secretKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}