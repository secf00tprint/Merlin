package de.reinhard.merlin.app.rest;

import de.reinhard.merlin.app.Configuration;
import de.reinhard.merlin.app.ConfigurationHandler;
import de.reinhard.merlin.app.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/configuration")
public class ConfigurationRest {
    private Logger log = LoggerFactory.getLogger(ConfigurationRest.class);

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    /**
     *
     * @param prettyPrinter If true then the json output will be in pretty format.
     * @see JsonUtils#toJson(Object, boolean)
     */
    public String getConfig(@QueryParam("prettyPrinter") boolean prettyPrinter) {
        Configuration config = new Configuration();
        config.copyFrom(ConfigurationHandler.getInstance().getConfiguration());
        if (config.getLanguage() == null) {
            config.setLanguage("default");
        }
        String json = JsonUtils.toJson(config, prettyPrinter);
        return json;
    }

    @POST
    @Path("config")
    @Produces(MediaType.TEXT_PLAIN)
    public void setConfig(String jsonConfig) {
        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        Configuration config = configurationHandler.getConfiguration();
        Configuration srcConfig = JsonUtils.fromJson(Configuration.class, jsonConfig);
        config.copyFrom(srcConfig);
        configurationHandler.save();
    }

    /**
     * Resets the settings to default values (deletes all settings).
     */
    @GET
    @Path("reset")
    @Produces(MediaType.APPLICATION_JSON)
    public String resetConfig(@QueryParam("IKnowWhatImDoing") boolean securityQuestion) {
        if (securityQuestion) {
            ConfigurationHandler.getInstance().removeAllSettings();
        }
        return getConfig(false);
    }
}
