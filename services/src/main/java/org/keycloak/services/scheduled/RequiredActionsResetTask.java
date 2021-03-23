package org.keycloak.services.scheduled;

import org.apache.commons.lang.math.NumberUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.timer.ScheduledTask;

import java.util.List;

public class RequiredActionsResetTask implements ScheduledTask {

    private static String INTERVAL_KEY = "reset_every";
    private static String UNIT_KEY = "reset_every_unit";

    public enum RequiredActions {
        terms_and_conditions;
    }

    @Override
    public void run(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realmModel -> {
            realmModel.getRequiredActionProvidersStream().forEach(requiredActionProviderModel -> {
                if(!requiredActionProviderModel.isEnabled() || requiredActionProviderModel.getConfig().get(INTERVAL_KEY)==null || requiredActionProviderModel.getConfig().get(UNIT_KEY)==null)
                    return;
                if(requiredActionProviderModel.getProviderId().equals(RequiredActions.terms_and_conditions.name())){
                    session.users().getUsersStream(realmModel, false).forEach(user -> {
                        if(expiredOrFirsttime(user, requiredActionProviderModel)) {
                            session.userCache().evict(realmModel, user);
                            user.addRequiredAction(RequiredActions.terms_and_conditions.name());
                        }
                    });
                }
            });
        });
    }



    private boolean expiredOrFirsttime(UserModel user, RequiredActionProviderModel requiredActionModel){
        List<String> attrList = user.getAttributes().get(requiredActionModel.getProviderId());
        if(attrList==null || attrList.isEmpty()) //means that this user has not performed this required action in the past
            return true;
        long numOfExpired = attrList.stream().filter(attrVal -> {
            try {
                long userLastAcceptTime = Long.parseLong(attrVal);

                long every = Long.parseLong(requiredActionModel.getConfig().get(INTERVAL_KEY));
                String timeUnit = requiredActionModel.getConfig().get(UNIT_KEY).toLowerCase();

                Long expiryOffset = null;
                switch(timeUnit){
                    case "hours":
                        expiryOffset = 60 * 60 * every;
                        break;
                    case "days":
                        expiryOffset = 60 * 60 * 24 * every;
                        break;
                }
                long epochSecondsNow = System.currentTimeMillis() / 1000L;

                if(expiryOffset != null && ( epochSecondsNow > expiryOffset + userLastAcceptTime))
                    return true; //required action expired
                else
                    return false;
            }
            catch(NumberFormatException ex){
                return false;
            }
        }).count(); //this should be 0 or 1

        return numOfExpired > 0;

    }

}