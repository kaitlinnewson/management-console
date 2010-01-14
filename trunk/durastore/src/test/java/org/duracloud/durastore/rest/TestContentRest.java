package org.duracloud.durastore.rest;

import org.apache.commons.httpclient.HttpStatus;
import org.duracloud.common.web.EncodeUtil;
import org.duracloud.common.web.RestHttpHelper;
import org.duracloud.common.web.RestHttpHelper.HttpResponse;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Runtime test of content REST API. The durastore web application must be
 * deployed and available at the baseUrl location in order for these tests to
 * pass.
 *
 * @author Bill Branan
 */
public class TestContentRest {

    private static RestHttpHelper restHelper = new RestHttpHelper();

    private static String baseUrl;

    private static final String CONTENT = "<content />";

    private static String spaceId;

    private static String[] contentIds = {"content1",
                                          "dir0/dir1/content2",
                                          "dir0/dir1/content3?storeID=0"};

    static {
        String random = String.valueOf(new Random().nextInt(99999));
        spaceId = "space" + random;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        baseUrl = RestTestHelper.getBaseUrl();

        // Initialize the Instance
        HttpResponse response = RestTestHelper.initialize();
        int statusCode = response.getStatusCode();
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Before
    public void setUp() throws Exception {
        // Add space
        HttpResponse response = RestTestHelper.addSpace(spaceId);
        int statusCode = response.getStatusCode();
        assertEquals(HttpStatus.SC_CREATED, statusCode);

        for (String contentId : contentIds) {
            setUpContent(contentId);
        }
    }

    private void setUpContent(String contentId) throws Exception {
        HttpResponse response;
        int statusCode;

        // Add content to space
        String url = baseUrl + "/" + spaceId + "/" + contentId;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(RestTestHelper.METADATA_NAME,
                    RestTestHelper.METADATA_VALUE);
        response = restHelper.put(url, CONTENT, headers);
        statusCode = response.getStatusCode();
        assertEquals(HttpStatus.SC_CREATED, statusCode);
    }

    @After
    public void tearDown() throws Exception {
        // Delete space
        HttpResponse response = RestTestHelper.deleteSpace(spaceId);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        String responseText = response.getResponseBody();
        assertNotNull(responseText);
    }

    private String removeParams(String contentId) {
        int paramStart = contentId.indexOf('?');
        int endIndex = paramStart > 0 ? paramStart : contentId.length();
        return contentId.substring(0, endIndex);
    }

    @Test
    public void testAddContent() throws Exception {
        // Test invalid content IDs

        // Question mark
        String id = "test?content";
        id = EncodeUtil.urlEncode(id);
        HttpResponse response = addContentItem(id);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

        // Backslash
        id = "test\\content";
        id = EncodeUtil.urlEncode(id);
        response = addContentItem(id);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

        // Too long
        id = "test-content";
        while(id.getBytes().length <= 1024) {
            id += "test-content";
        }
        response = addContentItem(id);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

        // Test valid content IDs

        // Test Special characters
        char[] specialChars = {'~','`','!','@','$','^','&','*','(',')','_','-',
                               '+','=','\'',':','.',',','<','>','"','[',']',
                               '{','}','#','%',';','|',' ','/'};
        for(char character : specialChars) {
            testCharacterInContentId(character);
        }
    }

    private HttpResponse addContentItem(String contentId) throws Exception {
        String url = baseUrl + "/" + spaceId + "/" + contentId;
        Map<String, String> headers = new HashMap<String, String>();
        return restHelper.put(url, CONTENT, headers);
    }

    private void testCharacterInContentId(char character) throws Exception {
        String contentId = "test-" + String.valueOf(character) + "-content";
        contentId = EncodeUtil.urlEncode(contentId);
        HttpResponse response = addContentItem(contentId);
        assertEquals("Testing character: " + character,
                     HttpStatus.SC_CREATED,
                     response.getStatusCode());
        response = getContentItem(contentId);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    private HttpResponse getContentItem(String contentId) throws Exception {
        String url = baseUrl + "/" + spaceId + "/" + contentId;
        return restHelper.get(url);
    }

    @Test
    public void testGetContent() throws Exception {
        for (String contentId : contentIds) {
            doTestGetContent(contentId);
        }
    }

    private void doTestGetContent(String contentId) throws Exception {
        HttpResponse response = getContentItem(contentId);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        String content = response.getResponseBody();
        assertNotNull(content);
        assertEquals(CONTENT, content);

        String contentType = response.getResponseHeader(HttpHeaders.CONTENT_TYPE)
            .getValue();
        assertNotNull(contentType);
        assertTrue(contentType.contains("text/xml"));
    }

    @Test
    public void testGetContentMetadata() throws Exception {
        for (String contentId : contentIds) {
            doTestGetContentMetadata(contentId);
        }
    }

    private void doTestGetContentMetadata(String contentId) throws Exception {
        String url = baseUrl + "/" + spaceId + "/" + contentId;
        HttpResponse response = restHelper.head(url);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        verifyMetadata(response, HttpHeaders.CONTENT_LENGTH, "11");

        String contentType = response.getResponseHeader(HttpHeaders.CONTENT_TYPE)
            .getValue();
        assertNotNull(contentType);
        assertTrue(contentType.contains("text/xml"));

        String contentChecksum = response.getResponseHeader("Content-MD5")
            .getValue();
        assertNotNull(contentChecksum);

        String contentETag = response.getResponseHeader(HttpHeaders.ETAG)
            .getValue();
        assertNotNull(contentETag);
        assertEquals(contentChecksum, contentETag);

        String contentModified = response.getResponseHeader(HttpHeaders.LAST_MODIFIED)
            .getValue();
        assertNotNull(contentModified);

        verifyMetadata(response,
                       RestTestHelper.METADATA_NAME,
                       RestTestHelper.METADATA_VALUE);
    }

    @Test
    public void testUpdateContentMetadata() throws Exception {
        for (String contentId : contentIds) {
            doTestUpdateContentMetadata(contentId);
        }
    }

    private void doTestUpdateContentMetadata(String contentId)
        throws Exception {
        String url = baseUrl + "/" + spaceId + "/" + contentId;

        // Add metadata
        Map<String, String> headers = new HashMap<String, String>();
        String newContentMime = "text/plain";
        headers.put(HttpHeaders.CONTENT_TYPE, newContentMime);
        String newMetaName = BaseRest.HEADER_PREFIX + "new-metadata";
        String newMetaValue = "New Metadata Value";
        headers.put(newMetaName, newMetaValue);

        postMetadataUpdate(url, contentId, headers);

        // Make sure the changes were saved
        HttpResponse response = restHelper.head(url);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        verifyMetadata(response, HttpHeaders.CONTENT_TYPE, newContentMime);
        verifyMetadata(response, newMetaName, newMetaValue);

        // Remove metadata
        headers = new HashMap<String, String>();
        postMetadataUpdate(url, contentId, headers);

        response = restHelper.head(url);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        // New metadata items should be gone, mimetype should be unchanged
        verifyNoMetadata(response, newMetaName);
        verifyMetadata(response, HttpHeaders.CONTENT_TYPE, newContentMime);

        // Update mimetype
        String testMime = "application/test";
        headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, testMime);
        postMetadataUpdate(url, contentId, headers);

        response = restHelper.head(url);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        // Metadata should be updated
        verifyMetadata(response, HttpHeaders.CONTENT_TYPE, testMime);
    }

    private HttpResponse postMetadataUpdate(String url,
                                            String contentId,
                                            Map<String, String> headers)
        throws Exception {
        HttpResponse response = restHelper.post(url, null, headers);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        String responseText = response.getResponseBody();
        assertNotNull(responseText);
        assertTrue(responseText.contains(removeParams(contentId)));
        assertTrue(responseText.contains("updated"));

        return response;
    }

    private void verifyMetadata(HttpResponse response,
                                String name,
                                String value) throws Exception {
        String metadata = response.getResponseHeader(name).getValue();
        assertNotNull(metadata);
        assertEquals(metadata, value);
    }

    private void verifyNoMetadata(HttpResponse response,
                                  String name) throws Exception {
        assertNull(response.getResponseHeader(name));
    }

    @Test
    public void testNotFound() throws Exception {
        String invalidSpaceId = "non-existant-space";
        String invalidContentId = "non-existant-content";
        String url = baseUrl + "/" + invalidSpaceId + "/" + invalidContentId;

        // Add Content
        HttpResponse response = restHelper.put(url, "test-content", null);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        url = baseUrl + "/" + spaceId + "/" + invalidContentId;

        // Get Content
        response = restHelper.get(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        // Get Content Metadata
        response = restHelper.head(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        // Set Content Metadata
        response = restHelper.post(url, null, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());

        // Delete Content
        response = restHelper.delete(url);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
    }

}