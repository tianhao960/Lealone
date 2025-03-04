/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.storage.aose.btree;

import org.lealone.storage.CursorParameters;
import org.lealone.storage.StorageMapCursor;
import org.lealone.storage.aose.btree.page.Page;

/**
 * A cursor to iterate over elements in ascending order.
 * 
 * @param <K> the key type
 * @param <V> the value type
 * 
 * @author H2 Group
 * @author zhh
 */
public class BTreeCursor<K, V> implements StorageMapCursor<K, V> {

    protected final BTreeMap<K, ?> map;
    protected final CursorParameters<K> parameters;
    protected CursorPos pos;

    private K key;
    private V value;

    public BTreeCursor(BTreeMap<K, ?> map, CursorParameters<K> parameters) {
        this.map = map;
        this.parameters = parameters;
        // 定位到>=from的第一个leaf page
        min(map.getRootPage(), parameters.from);
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public boolean hasNext() {
        while (pos != null) {
            if (pos.index < pos.page.getKeyCount()) {
                return true;
            }
            pos = pos.parent;
            if (pos == null) {
                return false;
            }
            if (pos.index < map.getChildPageCount(pos.page)) {
                min(pos.page.getChildPage(pos.index++), null);
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public K next() {
        // 不再检测了，让调用者自己先调用hasNext()再调用next()
        // if (!hasNext()) {
        // throw new NoSuchElementException();
        // }
        int index = pos.index++;
        key = (K) pos.page.getKey(index);
        if (parameters.allColumns)
            value = (V) pos.page.getValue(index, true);
        else
            value = (V) pos.page.getValue(index, parameters.columnIndexes);
        return key;
    }

    /**
     * Fetch the next entry that is equal or larger than the given key, starting
     * from the given page. This method retains the stack.
     * 
     * @param p the page to start
     * @param from the key to search
     */
    protected void min(Page p, K from) {
        while (true) {
            if (p.isLeaf()) {
                int x = from == null ? 0 : p.binarySearch(from);
                if (x < 0) {
                    x = -x - 1;
                }
                pos = new CursorPos(p, x, pos);
                break;
            }
            int x = from == null ? 0 : p.getPageIndex(from);
            pos = new CursorPos(p, x + 1, pos);
            p = p.getChildPage(x);
        }
    }
}
