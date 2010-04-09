package org.duracloud.sync.mgmt;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

import java.io.File;

/**
 * @author: Bill Branan
 * Date: Apr 2, 2010
 */
public class StatusManagerTest {

    @Test
    public void testStatusManager() {
        StatusManager status = new StatusManager();
        assertEquals(0, status.getInWork());
        assertEquals(0, status.getCompleted());
        assertEquals(0, status.getFailed().size());

        for(int i=0; i<100; i++) {
            status.inWork();
        }

        assertEquals(100, status.getInWork());
        assertEquals(0, status.getCompleted());
        assertEquals(0, status.getFailed().size());

        for(int i=0; i<50; i++) {
            status.completedProcessing();
        }

        assertEquals(50, status.getInWork());
        assertEquals(50, status.getCompleted());
        assertEquals(0, status.getFailed().size());

        for(int i=0; i<50; i++) {
            status.failedProcessing(new File("test"));
        }

        assertEquals(0, status.getInWork());
        assertEquals(50, status.getCompleted());
        assertEquals(50, status.getFailed().size());
    }
}
