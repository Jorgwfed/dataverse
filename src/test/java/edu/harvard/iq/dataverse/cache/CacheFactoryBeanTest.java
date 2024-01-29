package edu.harvard.iq.dataverse.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CacheFactoryBeanTest {

    private SystemConfig mockedSystemConfig;
    static CacheFactoryBean cache = null;
    AuthenticatedUser authUser = new AuthenticatedUser();
    GuestUser guestUser = GuestUser.get();
    String action;
    static final String settingJson = "{\n" +
            "  \"rateLimits\":[\n" +
            "    {\n" +
            "      \"tier\": 0,\n" +
            "      \"limitPerHour\": 10,\n" +
            "      \"actions\": [\n" +
            "        \"GetLatestPublishedDatasetVersionCommand\",\n" +
            "        \"GetPrivateUrlCommand\",\n" +
            "        \"GetDatasetCommand\",\n" +
            "        \"GetLatestAccessibleDatasetVersionCommand\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"tier\": 0,\n" +
            "      \"limitPerHour\": 1,\n" +
            "      \"actions\": [\n" +
            "        \"CreateGuestbookResponseCommand\",\n" +
            "        \"UpdateDatasetVersionCommand\",\n" +
            "        \"DestroyDatasetCommand\",\n" +
            "        \"DeleteDataFileCommand\",\n" +
            "        \"FinalizeDatasetPublicationCommand\",\n" +
            "        \"PublishDatasetCommand\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"tier\": 1,\n" +
            "      \"limitPerHour\": 30,\n" +
            "      \"actions\": [\n" +
            "        \"CreateGuestbookResponseCommand\",\n" +
            "        \"GetLatestPublishedDatasetVersionCommand\",\n" +
            "        \"GetPrivateUrlCommand\",\n" +
            "        \"GetDatasetCommand\",\n" +
            "        \"GetLatestAccessibleDatasetVersionCommand\",\n" +
            "        \"UpdateDatasetVersionCommand\",\n" +
            "        \"DestroyDatasetCommand\",\n" +
            "        \"DeleteDataFileCommand\",\n" +
            "        \"FinalizeDatasetPublicationCommand\",\n" +
            "        \"PublishDatasetCommand\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @BeforeEach
    public void setup() throws IOException {
        // reuse cache and config for all tests
        if (cache == null) {
            mockedSystemConfig = mock(SystemConfig.class);
            doReturn(30).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(0), eq(RateLimitUtil.NO_LIMIT));
            doReturn(60).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(1), eq(RateLimitUtil.NO_LIMIT));
            doReturn(120).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(2), eq(RateLimitUtil.NO_LIMIT));
            doReturn(RateLimitUtil.NO_LIMIT).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(3), eq(RateLimitUtil.NO_LIMIT));
            doReturn(settingJson).when(mockedSystemConfig).getRateLimitsJson();
            cache = new CacheFactoryBean();
            cache.systemConfig = mockedSystemConfig;
            cache.init(); // PostConstruct - start Hazelcast

            // clear the static data so it can be reloaded with the new mocked data
            RateLimitUtil.rateLimitMap.clear();
            RateLimitUtil.rateLimits.clear();

            // testing cache implementation and code coverage
            final String cacheKey = "CacheTestKey" + UUID.randomUUID();
            final String cacheValue = "CacheTestValue" + UUID.randomUUID();
            long cacheSize = cache.getCacheSize(cache.RATE_LIMIT_CACHE);
            cache.setCacheValue(cache.RATE_LIMIT_CACHE, cacheKey,cacheValue);
            assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > cacheSize);
            Object cacheValueObj = cache.getCacheValue(cache.RATE_LIMIT_CACHE, cacheKey);
            assertTrue(cacheValueObj != null && cacheValue.equalsIgnoreCase((String) cacheValueObj));
        }

        // reset to default auth user
        authUser.setRateLimitTier(1);
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("authUser");

        // create a unique action for each test
        action = "cmd-" + UUID.randomUUID();
    }

    @Test
    public void testGuestUserGettingRateLimited() {
        String key = RateLimitUtil.generateCacheKey(guestUser,action);
        String value = String.valueOf(cache.getCacheValue(cache.RATE_LIMIT_CACHE, key));
        String keyLastUpdate = String.format("%s:last_update",key);
        String lastUpdate = String.valueOf(cache.getCacheValue(cache.RATE_LIMIT_CACHE, keyLastUpdate));
        System.out.println(">>> key|value|lastUpdate  |" + key + "|" + value + "|" + lastUpdate);

        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(guestUser, action);
            if (rateLimited) {
                break;
            }
            if (cnt % 10 == 0) {
                value = String.valueOf(cache.getCacheValue(cache.RATE_LIMIT_CACHE, key));
                lastUpdate = String.valueOf(cache.getCacheValue(cache.RATE_LIMIT_CACHE, keyLastUpdate));
                System.out.println(cnt + " key|value|lastUpdate  |" + key + "|" + value + "|" + lastUpdate);
            }
        }

        value = String.valueOf(cache.getCacheValue(cache.RATE_LIMIT_CACHE, key));
        lastUpdate = String.valueOf(cache.getCacheValue(cache.RATE_LIMIT_CACHE, keyLastUpdate));
        System.out.println(cnt + " key|value|lastUpdate  |" + key + "|" + value + "|" + lastUpdate);
        assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > 0);
        assertTrue(rateLimited && cnt > 1 && cnt <= 30, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }

    @Test
    public void testAdminUserExemptFromGettingRateLimited() {
        authUser.setSuperuser(true);
        authUser.setUserIdentifier("admin");
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited && cnt >= 99, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }

    @Test
    public void testAuthenticatedUserGettingRateLimited() throws InterruptedException {
        authUser.setRateLimitTier(2); // 120 cals per hour - 1 added token every 30 seconds
        boolean rateLimited = false;
        int cnt;
        for (cnt = 0; cnt <200; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(rateLimited && cnt == 120, "rateLimited:"+rateLimited + " cnt:"+cnt);

        for (cnt = 0; cnt <60; cnt++) {
            Thread.sleep(1000);// wait for bucket to be replenished (check each second for 1 minute max)
            rateLimited = !cache.checkRate(authUser, action);
            if (!rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited, "rateLimited:"+rateLimited + " cnt:"+cnt);

        // Now change the user's tier, so it is no longer limited
        authUser.setRateLimitTier(3); // tier 3 = no limit
        for (cnt = 0; cnt <200; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited && cnt == 200, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }
}