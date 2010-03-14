package org.duracloud.security.vote;

import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.ConfigAttribute;

import java.util.Collection;

/**
 * @author Andrew Woods
 *         Date: Mar 14, 2010
 */
public class VoterUtil {

    /**
     * This is small debug utility available to voters in this package.
     */
    protected static String debugText(String heading,
                                      Authentication auth,
                                      ConfigAttributeDefinition config,
                                      int decision) {
        StringBuilder sb = new StringBuilder(heading);
        sb.append(": ");
        if (auth != null) {
            sb.append(auth.getName());
        }
        if (config != null) {
            Collection<ConfigAttribute> atts = config.getConfigAttributes();
            if (atts != null && atts.size() > 0) {
                sb.append(" [");
                for (ConfigAttribute att : atts) {
                    sb.append(att.getAttribute());
                    sb.append(",");
                }
                sb.replace(sb.length() - 1, sb.length(), "]");
            }
        }
        sb.append(" => decision: " + decision);

        return sb.toString();
    }
}
