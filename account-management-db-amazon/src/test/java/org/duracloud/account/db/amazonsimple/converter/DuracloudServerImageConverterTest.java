/*
 * Copyright (c) 2009-2011 DuraSpace. All rights reserved.
 */
package org.duracloud.account.db.amazonsimple.converter;

import com.amazonaws.services.simpledb.model.Attribute;
import org.duracloud.account.common.domain.ServerImage;

import java.util.ArrayList;
import java.util.List;

import static org.duracloud.account.db.BaseRepo.COUNTER_ATT;
import static org.duracloud.account.db.amazonsimple.converter.DuracloudServerImageConverter.DC_ROOT_PASSWORD_ATT;
import static org.duracloud.account.db.amazonsimple.converter.DuracloudServerImageConverter.DESCRIPTION_ATT;
import static org.duracloud.account.db.amazonsimple.converter.DuracloudServerImageConverter.LATEST_ATT;
import static org.duracloud.account.db.amazonsimple.converter.DuracloudServerImageConverter.PROVIDER_ACCOUNT_ID_ATT;
import static org.duracloud.account.db.amazonsimple.converter.DuracloudServerImageConverter.PROVIDER_IMAGE_ID_ATT;
import static org.duracloud.account.db.amazonsimple.converter.DuracloudServerImageConverter.VERSION_ATT;
import static org.duracloud.account.db.util.FormatUtil.padded;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author: Bill Branan
 * Date: Feb 2, 2011
 */
public class DuracloudServerImageConverterTest extends DomainConverterTest<ServerImage> {

    private static final int id = 0;
    private static final int providerAccountId = 1;
    private static final String providerImageId = "provider-image-id";
    private static final String version = "version-1";
    private static final String description = "description";
    private static final String dcRootPassword = "rootpass";
    private static final boolean latest = true;
    private static final int counter = 4;

    @Override
    protected DomainConverter<ServerImage> createConverter() {
        return new DuracloudServerImageConverter();
    }

    @Override
    protected ServerImage createTestItem() {
        return new ServerImage(id,
                               providerAccountId,
                               providerImageId,
                               version,
                               description,
                               dcRootPassword,
                               latest,
                               counter);
    }

    @Override
    protected List<Attribute> createTestAttributes() {
        DuracloudServerImageConverter imgCvtr =
            new DuracloudServerImageConverter();

        List<Attribute> testAtts = new ArrayList<Attribute>();
        testAtts.add(new Attribute(PROVIDER_ACCOUNT_ID_ATT, imgCvtr.asString(providerAccountId)));
        testAtts.add(new Attribute(PROVIDER_IMAGE_ID_ATT, providerImageId));
        testAtts.add(new Attribute(VERSION_ATT, version));
        testAtts.add(new Attribute(DESCRIPTION_ATT, description));
        testAtts.add(new Attribute(DC_ROOT_PASSWORD_ATT, dcRootPassword));
        testAtts.add(new Attribute(LATEST_ATT, String.valueOf(latest)));
        testAtts.add(new Attribute(COUNTER_ATT, padded(counter)));
        return testAtts;
    }

    @Override
    protected void verifyItem(ServerImage serverImage) {
        assertNotNull(serverImage);

        assertNotNull(serverImage.getProviderAccountId());
        assertNotNull(serverImage.getProviderImageId());
        assertNotNull(serverImage.getVersion());
        assertNotNull(serverImage.getDescription());
        assertNotNull(serverImage.getDcRootPassword());
        assertNotNull(serverImage.isLatest());

        assertEquals(counter, serverImage.getCounter());
        assertEquals(providerAccountId, serverImage.getProviderAccountId());
        assertEquals(providerImageId, serverImage.getProviderImageId());
        assertEquals(version, serverImage.getVersion());
        assertEquals(description, serverImage.getDescription());
        assertEquals(dcRootPassword, serverImage.getDcRootPassword());
        assertEquals(latest, serverImage.isLatest());
    }

}