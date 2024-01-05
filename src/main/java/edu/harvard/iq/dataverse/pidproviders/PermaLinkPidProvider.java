package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * PermaLink provider This is a minimalist permanent ID provider intended for
 * use with 'real' datasets/files where the use case none-the-less doesn't lend
 * itself to the use of DOIs or Handles, e.g. * due to cost * for a
 * catalog/archive where Dataverse has a dataset representing a dataset with
 * DOI/handle stored elsewhere
 * 
 * The initial implementation will mint identifiers locally and will provide the
 * existing page URLs (using the ?persistentID=<id> format). This will be
 * overridable by a configurable parameter to support use of an external
 * resolver.
 * 
 */
public class PermaLinkPidProvider extends AbstractPidProvider {

    private static final Logger logger = Logger.getLogger(PermaLinkPidProvider.class.getCanonicalName());

    public static final String PERMA_PROTOCOL = "perma";
    public static final String TYPE = "perma";
    public static final String PERMA_PROVIDER_NAME = "PERMA";

    // ToDo - remove
    @Deprecated
    public static final String PERMA_RESOLVER_URL = JvmSettings.PERMALINK_BASE_URL.lookupOptional("permalink")
            .orElse(SystemConfig.getDataverseSiteUrlStatic());

    
    private String separator = "";

    private String baseUrl;

    public PermaLinkPidProvider(String name, String providerAuthority, String providerShoulder, String identifierGenerationStyle,
            String datafilePidFormat, String managedList, String excludedList, String baseUrl, String separator) {
        super(name, PERMA_PROTOCOL, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat,
                managedList, excludedList);
        this.baseUrl = baseUrl;
        this.separator = separator;
    }

    @Override
    public String getSeparator() {
        return separator;
    }

    @Override
    public boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) {
        // Perma doesn't manage registration, so we assume all local PIDs can be treated
        // as registered
        boolean existsLocally = !pidProviderService.isGlobalIdLocallyUnique(globalId);
        return existsLocally ? existsLocally : noProviderDefault;
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of(getName(), getBaseUrl());
    }

    @Override
    public String createIdentifier(DvObject dvo) throws Throwable {
        // Call external resolver and send landing URL?
        // FWIW: Return value appears to only be used in RegisterDvObjectCommand where
        // success requires finding the dvo identifier in this string. (Also logged a
        // couple places).
        return (dvo.getGlobalId().asString());
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvo) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
        return getTargetUrl(dvo);
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        // no-op
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        // Generate if needed (i.e. datafile case where we don't create/register early
        // (even with registerWhenPublished == false))
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generateIdentifier(dvObject);
        }
        // Call external resolver and send landing URL?
        return true;
    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        // ToDo - handle local PID resolver for dataset/file
        logger.info("Parsing in Perma: " + pidString);
        if (pidString.startsWith(getUrlPrefix())) {
            pidString = pidString.replace(getUrlPrefix(), (PERMA_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {
        logger.info("Checking Perma: " + identifierString);
        if (!PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        String identifier = null;
        if (getAuthority() != null) {
            if (identifierString.startsWith(getAuthority())) {
                identifier = identifierString.substring(getAuthority().length());
            } else {
                //Doesn't match authority
                return null;
            }
            if (identifier.startsWith(separator)) {
                identifier = identifier.substring(separator.length());
            } else {
                //Doesn't match separator
                return null;
            }
        }
        identifier = PidProvider.formatIdentifierString(identifier);
        if (PidProvider.testforNullTerminator(identifier)) {
            return null;
        }
        if(!identifier.startsWith(getShoulder())) {
            //Doesn't match shoulder
            return null;
        }
        return new GlobalId(PERMA_PROTOCOL, getAuthority(), identifier, separator, getUrlPrefix(), PERMA_PROVIDER_NAME);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!PERMA_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {

        return getBaseUrl() + "/citation?persistentId=" + PERMA_PROTOCOL + ":";
    }

    @Override
    public String getProtocol() {
        return PERMA_PROTOCOL;
    }

    @Override
    public String getProviderType() {
        return PERMA_PROTOCOL;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}