package org.duracloud.rackspacestorage;

import com.rackspacecloud.client.cloudfiles.FilesCDNContainer;
import com.rackspacecloud.client.cloudfiles.FilesClient;
import junit.framework.Assert;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.duracloud.common.model.Credential;
import org.duracloud.common.web.RestHttpHelper;
import org.duracloud.common.web.RestHttpHelper.HttpResponse;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.domain.test.db.UnitTestDatabaseUtil;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.error.StorageException;
import org.duracloud.storage.provider.StorageProvider;
import org.duracloud.storage.provider.StorageProvider.AccessType;
import static org.duracloud.storage.util.StorageProviderUtil.compareChecksum;
import static org.duracloud.storage.util.StorageProviderUtil.contains;
import static org.duracloud.storage.util.StorageProviderUtil.count;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tests the Rackspace Storage Provider. This test is run via the command line
 * in order to allow passing in credentials.
 *
 * @author Bill Branan
 */
public class RackspaceStorageProviderTest {

    protected static final Logger log =
            Logger.getLogger(RackspaceStorageProviderTest.class);

    RackspaceStorageProvider rackspaceProvider;

    FilesClient filesClient;

    private static String SPACE_ID = null;
    private static final String CONTENT_ID = "duracloud-test-content";
    private static final String SPACE_META_NAME = "custom-space-metadata";
    private static final String SPACE_META_VALUE = "Testing Space";
    private static final String CONTENT_META_NAME = "custom-content-metadata";
    private static final String CONTENT_META_VALUE = "Testing Content";
    private static final String CONTENT_MIME_NAME = StorageProvider.METADATA_CONTENT_MIMETYPE;
    private static final String CONTENT_MIME_VALUE = "text/plain";
    private static final String CONTENT_DATA = "Test Content";

    @Before
    public void setUp() throws Exception {
        Credential rackspaceCredential = getCredential();
        Assert.assertNotNull(rackspaceCredential);

        String username = rackspaceCredential.getUsername();
        String password = rackspaceCredential.getPassword();
        Assert.assertNotNull(username);
        Assert.assertNotNull(password);

        rackspaceProvider = new RackspaceStorageProvider(username, password);
        filesClient = new FilesClient(username, password);
        assertTrue(filesClient.login());

        String random = String.valueOf(new Random().nextInt(99999));
        SPACE_ID = "duracloud-test-bucket." + random;
    }

    @After
    public void tearDown() {
        try {
            rackspaceProvider.deleteSpace(SPACE_ID);
        } catch(Exception e) {
            // Ignore, the space has likely already been deleted
        }
        rackspaceProvider = null;
        filesClient = null;
    }

    private Credential getCredential() throws Exception {
        UnitTestDatabaseUtil dbUtil = new UnitTestDatabaseUtil();
        return dbUtil.findCredentialForProvider(StorageProviderType.RACKSPACE);
    }

    @Test
    public void testRackspaceStorageProvider() throws Exception {
        // test createSpace()
        log.debug("Test createSpace()");
        rackspaceProvider.createSpace(SPACE_ID);

        // test setSpaceMetadata()
        log.debug("Test setSpaceMetadata()");
        Map<String, String> spaceMetadata = new HashMap<String, String>();
        spaceMetadata.put(SPACE_META_NAME, SPACE_META_VALUE);
        rackspaceProvider.setSpaceMetadata(SPACE_ID, spaceMetadata);

        // test getSpaceMetadata()
        log.debug("Test getSpaceMetadata()");
        Map<String, String> sMetadata = rackspaceProvider.getSpaceMetadata(SPACE_ID);

        assertTrue(sMetadata.containsKey(StorageProvider.METADATA_SPACE_CREATED));
        assertTrue(sMetadata.containsKey(StorageProvider.METADATA_SPACE_COUNT));
        assertTrue(sMetadata.containsKey(StorageProvider.METADATA_SPACE_ACCESS));
        assertNotNull(sMetadata.get(StorageProvider.METADATA_SPACE_CREATED));
        assertNotNull(sMetadata.get(StorageProvider.METADATA_SPACE_COUNT));
        assertNotNull(sMetadata.get(StorageProvider.METADATA_SPACE_ACCESS));

        assertTrue(sMetadata.containsKey(SPACE_META_NAME));
        assertEquals(SPACE_META_VALUE, sMetadata.get(SPACE_META_NAME));

        // test getSpaces()
        log.debug("Test getSpaces()");
        Iterator<String> spaces = rackspaceProvider.getSpaces();
        assertNotNull(spaces);
        assertTrue(contains(spaces, SPACE_ID)); // This will only work when SPACE_ID fits
                                                // the Rackspace container naming conventions

        // test invalid space
        log.debug("Test getSpaceAccess() with invalid spaceId");
        try {
            rackspaceProvider.getSpaceAccess(SPACE_ID+"-invalid");
            fail("getSpaceAccess() should throw an exception with an invalid spaceId");
        } catch(StorageException expected) {
            assertNotNull(expected);
        }

        log.debug("Test getSpaceContents() with invalid spaceId");
        try {
            rackspaceProvider.getSpaceContents(SPACE_ID+"-invalid", null);
            fail("getSpaceContents() should throw an exception with an invalid spaceId");
        } catch(StorageException expected) {
            assertNotNull(expected);
        }

        // Check space access - should be closed
        // TODO: Uncomment after Rackspace SDK bug is fixed
        //        log.debug("Check space access");
        //        FilesCDNContainer cdnContainer = filesClient.getCDNContainerInfo(SPACE_ID);
        //        assertFalse(cdnContainer.isEnabled());

        // test setSpaceAccess()
        log.debug("Test setSpaceAccess(OPEN)");
        rackspaceProvider.setSpaceAccess(SPACE_ID, AccessType.OPEN);

        // test getSpaceAccess()
        log.debug("Test getSpaceAccess()");
        AccessType access = rackspaceProvider.getSpaceAccess(SPACE_ID);
        assertEquals(access, AccessType.OPEN);

        // Check space access - should be open
        log.debug("Check space access");
        FilesCDNContainer cdnContainer =
                filesClient.getCDNContainerInfo(SPACE_ID);
        assertTrue(cdnContainer.isEnabled());

        // test addContent()
        log.debug("Test addContent()");
        addContent(SPACE_ID, CONTENT_ID, CONTENT_MIME_VALUE);

        // test getContentMetadata()
        log.debug("Test getContentMetadata()");
        Map<String, String> cMetadata =
                rackspaceProvider.getContentMetadata(SPACE_ID, CONTENT_ID);
        assertNotNull(cMetadata);
        assertEquals(CONTENT_MIME_VALUE, cMetadata.get(CONTENT_MIME_NAME));
        assertNotNull(cMetadata.get(StorageProvider.METADATA_CONTENT_MODIFIED));
        assertNotNull(cMetadata.get(StorageProvider.METADATA_CONTENT_SIZE));
        assertNotNull(cMetadata.get(StorageProvider.METADATA_CONTENT_CHECKSUM));

        // Check content access
        log.debug("Check content access");
        String spaceUrl = cdnContainer.getCdnURL();
        String contentUrl = spaceUrl + "/" + CONTENT_ID;
        RestHttpHelper restHelper = new RestHttpHelper();
        HttpResponse httpResponse = restHelper.get(contentUrl);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusCode());

        // test setSpaceAccess()
        log.debug("Test setSpaceAccess(CLOSED)");
        rackspaceProvider.setSpaceAccess(SPACE_ID, AccessType.CLOSED);
        // Note that content stays in the Rackspace CDN for 24 hours
        // after it is made available, even if the container has been
        // set to private access or the content has been deleted.

        // add additional content for getContents tests
        String testContent2 = "test-content-2";
        addContent(SPACE_ID, testContent2, CONTENT_MIME_VALUE);
        String testContent3 = "test-content-3";
        addContent(SPACE_ID, testContent3, null);

        // test getSpaceContents()
        log.debug("Test getSpaceContents()");
        Iterator<String> spaceContents =
            rackspaceProvider.getSpaceContents(SPACE_ID, null);
        assertNotNull(spaceContents);
        assertEquals(3, count(spaceContents));
        // Ensure that space metadata is not included in contents list
        spaceContents = rackspaceProvider.getSpaceContents(SPACE_ID, null);
        String containerName = rackspaceProvider.getContainerName(SPACE_ID);
        String spaceMetaSuffix = RackspaceStorageProvider.SPACE_METADATA_SUFFIX;
        assertFalse(contains(spaceContents, containerName + spaceMetaSuffix));

        // test getSpaceContentsChunked() maxLimit
        log.debug("Test getSpaceContentsChunked() maxLimit");
        List<String> spaceContentList =
            rackspaceProvider.getSpaceContentsChunked(SPACE_ID,
                                                      null,
                                                      2,
                                                      null);
        assertNotNull(spaceContentList);
        assertEquals(2, spaceContentList.size());
        String lastItem = spaceContentList.get(spaceContentList.size() - 1);
        spaceContentList = rackspaceProvider.getSpaceContentsChunked(SPACE_ID,
                                                                     null,
                                                                     2,
                                                                     lastItem);
        assertNotNull(spaceContentList);
        assertEquals(1, spaceContentList.size());

        // test getSpaceContentsChunked() prefix
        log.debug("Test getSpaceContentsChunked() prefix");
        spaceContentList = rackspaceProvider.getSpaceContentsChunked(SPACE_ID,
                                                                     "test",
                                                                     10,
                                                                     null);
        assertEquals(2, spaceContentList.size());

        // test getContent()
        log.debug("Test getContent()");
        InputStream is = rackspaceProvider.getContent(SPACE_ID, CONTENT_ID);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String contentLine = reader.readLine();
        assertTrue(contentLine.equals(CONTENT_DATA));

        // test invalid content
        log.debug("Test getContent() with invalid content ID");       
        log.debug("-- Begin expected error log -- ");
        try {
            rackspaceProvider.getContent(SPACE_ID, "non-existant-content");
            fail("Exception expected");
        } catch (Exception e) {
            assertNotNull(e);
        }
        log.debug("-- End expected error log --");

        // test setContentMetadata()
        log.debug("Test setContentMetadata()");
        Map<String, String> contentMetadata = new HashMap<String, String>();
        contentMetadata.put(CONTENT_META_NAME, CONTENT_META_VALUE);
        rackspaceProvider.setContentMetadata(SPACE_ID,
                                             CONTENT_ID,
                                             contentMetadata);

        // test getContentMetadata()
        log.debug("Test getContentMetadata()");
        cMetadata = rackspaceProvider.getContentMetadata(SPACE_ID, CONTENT_ID);
        assertNotNull(cMetadata);
        assertEquals(CONTENT_META_VALUE, cMetadata.get(CONTENT_META_NAME));
        // Mime type was not included when setting content metadata
        // so its value should have been maintained
        assertEquals(CONTENT_MIME_VALUE, cMetadata.get(CONTENT_MIME_NAME));

        // test setContentMetadata() - mimetype
        log.debug("Test setContentMetadata() - mimetype");
        String newMime = "image/bmp";
        contentMetadata = new HashMap<String, String>();
        contentMetadata.put(CONTENT_MIME_NAME, newMime);
        rackspaceProvider.setContentMetadata(SPACE_ID,
                                             CONTENT_ID,
                                             contentMetadata);
        cMetadata = rackspaceProvider.getContentMetadata(SPACE_ID, CONTENT_ID);
        assertNotNull(cMetadata);
        assertEquals(newMime, cMetadata.get(CONTENT_MIME_NAME));
        // Custom metadata was not included in update, it should be removed
        assertNull(cMetadata.get(CONTENT_META_NAME));

        log.debug("Test getContentMetadata() - mimetype default");
        cMetadata = rackspaceProvider.getContentMetadata(SPACE_ID, testContent3);
        assertNotNull(cMetadata);
        assertEquals(StorageProvider.DEFAULT_MIMETYPE,
                     cMetadata.get(CONTENT_MIME_NAME));     

        // test deleteContent()
        log.debug("Test deleteContent()");
        rackspaceProvider.deleteContent(SPACE_ID, CONTENT_ID);
        spaceContents = rackspaceProvider.getSpaceContents(SPACE_ID, null);
        assertFalse(contains(spaceContents, CONTENT_ID));

        // test deleteSpace()
        log.debug("Test deleteSpace()");
        rackspaceProvider.deleteSpace(SPACE_ID);
        spaces = rackspaceProvider.getSpaces();
        assertFalse(contains(spaces, SPACE_ID));
    }

    private void addContent(String spaceId, String contentId, String mimeType) {
        byte[] content = CONTENT_DATA.getBytes();
        int contentSize = content.length;
        ByteArrayInputStream contentStream = new ByteArrayInputStream(content);
        String checksum = rackspaceProvider.addContent(spaceId,
                                                       contentId,
                                                       mimeType,
                                                       contentSize,
                                                       contentStream);
        compareChecksum(rackspaceProvider, spaceId, contentId, checksum);
    }

    @Test
    public void testNotFound() {
        String spaceId = SPACE_ID;
        String contentId = "NonExistantContent";
        String failMsg = "Should throw NotFoundException attempting to " +
                         "access a space which does not exist";
        byte[] content = CONTENT_DATA.getBytes();

        // Space Not Found

        try {
            rackspaceProvider.getSpaceMetadata(spaceId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.setSpaceMetadata(spaceId,
                                               new HashMap<String, String>());
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.getSpaceContents(spaceId, null);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.getSpaceContentsChunked(spaceId, null, 100, null);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.getSpaceAccess(spaceId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.setSpaceAccess(spaceId, AccessType.CLOSED);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.deleteSpace(spaceId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            int contentSize = content.length;
            ByteArrayInputStream contentStream = new ByteArrayInputStream(
                content);
            rackspaceProvider.addContent(spaceId,
                                         contentId,
                                         "text/plain",
                                         contentSize,
                                         contentStream);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.getContent(spaceId, contentId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.getContentMetadata(spaceId, contentId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.setContentMetadata(spaceId,
                                                 contentId,
                                                 new HashMap<String, String>());
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.deleteContent(spaceId, contentId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        // Content Not Found

        rackspaceProvider.createSpace(spaceId);
        failMsg = "Should throw NotFoundException attempting to " +
                  "access content which does not exist";

        try {
            rackspaceProvider.getContent(spaceId, contentId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.getContentMetadata(spaceId, contentId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.setContentMetadata(spaceId,
                                                 contentId,
                                                 new HashMap<String, String>());
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }

        try {
            rackspaceProvider.deleteContent(spaceId, contentId);
            fail(failMsg);
        } catch (NotFoundException expected) {
            assertNotNull(expected);
        }
    }

}