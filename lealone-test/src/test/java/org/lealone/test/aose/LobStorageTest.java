/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.test.aose;

import java.io.InputStream;
import java.io.StringReader;

import org.junit.Test;
import org.lealone.common.util.IOUtils;
import org.lealone.db.LocalDataHandler;
import org.lealone.db.value.ValueLob;
import org.lealone.storage.aose.AOStorage;
import org.lealone.storage.aose.lob.LobStreamStorage;

public class LobStorageTest extends AoseTestBase {
    @Test
    public void run() throws Exception {
        AOStorage storage = AOStorageTest.openStorage();
        String name = LobStorageTest.class.getName();
        name = "/" + name.replace('.', '/') + ".class";
        InputStream in = LobStorageTest.class.getResourceAsStream(name);
        int length = in.available();
        LobStreamStorage lobStorage = new LobStreamStorage(new LocalDataHandler(), storage);
        lobStorage.init();
        lobStorage.getLobStreamMap().setMaxBlockSize(512); // 设置小一点可以测试LobStreamMap中的indirect块

        ValueLob lob = lobStorage.createBlob(in, -1);
        assertEquals(1, lob.getLobId());

        in = lobStorage.getInputStream(lob, null, 0); // 返回的流未实现available()
        assertNotNull(in);
        byte[] bytes = IOUtils.toByteArray(in);
        assertEquals(length, bytes.length);

        String clobStr = "clob-test";
        StringBuilder buff = new StringBuilder(10000 * clobStr.length());
        for (int i = 0; i < 10000; i++)
            buff.append(clobStr);

        clobStr = buff.toString();
        StringReader reader = new StringReader(clobStr);
        lob = lobStorage.createClob(reader, -1);
        assertEquals(2, lob.getLobId());

        in = lobStorage.getInputStream(lob, null, 0);
        assertNotNull(in);

        lob = lobStorage.copyLob(lob, 10, lob.getPrecision());
        assertEquals(10, lob.getTableId());

        lobStorage.setTable(lob, 20);
        lobStorage.removeAllForTable(20);
        lobStorage.removeLob(lob);
    }
}
