package kvstore;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import kvstore.xml.KVCacheEntry;
import kvstore.xml.KVCacheType;
import kvstore.xml.KVSetType;
import kvstore.xml.ObjectFactory;

import java.util.*;

/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET). If a set is
 * full and another entry is added, an entry is dropped based on the eviction
 * policy.
 */
public class KVCache implements KeyValueInterface {

	/**
	 * Constructs a second-chance-replacement cache.
	 *
	 * @param numSets
	 *            the number of sets this cache will have
	 * @param maxElemsPerSet
	 *            the size of each set
	 */
	
	public int numSets;
	public int maxElemsPerSet;
	public LinkedList<String[]>[] sets;
	public Lock[] locks;
	
	
	@SuppressWarnings("unchecked")
	public KVCache(int numSets, int maxElemsPerSet) {
		// implement me
		// arrayList; linkedlist
		this.numSets = numSets;
		this.maxElemsPerSet = maxElemsPerSet;
		this.sets = new LinkedList[numSets];
		for (int i = 0; i < numSets; i++) {
			this.sets[i] = new LinkedList<String[]>();
			this.locks[i] = new ReentrantLock();
		}
	}

	/**
	 * Retrieves an entry from the cache. Assumes access to the corresponding
	 * set has already been locked by the caller of this method.
	 *
	 * @param key
	 *            the key whose associated value is to be returned.
	 * @return the value associated to this key or null if no value is
	 *         associated with this key in the cache
	 */
	@Override
	public String get(String key) {
		int setNum = key.hashCode() % this.numSets;
		LinkedList<String[]> keySet = this.sets[setNum];
		ListIterator<String[]> listIterator = keySet.listIterator();
		String[] mapTemp;
        while (listIterator.hasNext()) {
            mapTemp = listIterator.next();
            if (mapTemp[0] == key) {
            	mapTemp[2] = "true";
            	return mapTemp[1];
            }
        }
		return null;
	}

	/**
	 * Adds an entry to this cache. If an entry with the specified key already
	 * exists in the cache, it is replaced by the new entry. When an entry is
	 * replaced, its reference bit will be set to True. If the set is full, an
	 * entry is removed from the cache based on the eviction policy. If the set
	 * is not full, the entry will be inserted behind all existing entries. For
	 * this policy, we suggest using a LinkedList over an array to keep track of
	 * entries in a set since deleting an entry in an array will leave a gap in
	 * the array, likely not at the end. More details and explanations in the
	 * spec. Assumes access to the corresponding set has already been locked by
	 * the caller of this method.
	 *
	 * @param key
	 *            the key with which the specified value is to be associated
	 * @param value
	 *            a value to be associated with the specified key
	 */
	@Override
	public void put(String key, String value) {
		// implement me
		int setNum = key.hashCode() % this.numSets;
		LinkedList<String[]> keySet = this.sets[setNum];
		if (keySet.size() < this.maxElemsPerSet) {
			String[] entry = {key, value, "false"};
			keySet.add(entry);
			return;
		}
		ListIterator<String[]> listIterator = keySet.listIterator();
		String[] mapTemp;
        while (listIterator.hasNext()) {
            mapTemp = listIterator.next();
            if (mapTemp[2] == "false") {
            	mapTemp[0] = key;
            	mapTemp[1] = value;
            	mapTemp[2] = "true";
            	return;
            } else {
            	mapTemp[2] = "false";
            }
        }
        listIterator = keySet.listIterator();
        while (listIterator.hasNext()) {
            mapTemp = listIterator.next();
            if (mapTemp[2] == "false") {
            	mapTemp[0] = key;
            	mapTemp[1] = value;
            	mapTemp[2] = "true";
            	return;
            }
        }
	}

	/**
	 * Removes an entry from this cache. Assumes access to the corresponding set
	 * has already been locked by the caller of this method. Does nothing if
	 * called on a key not in the cache.
	 *
	 * @param key
	 *            key with which the specified value is to be associated
	 */
	@Override
	public void del(String key) {
		// implement me
		int setNum = key.hashCode() % this.numSets;
		LinkedList<String[]> keySet = this.sets[setNum];
		ListIterator<String[]> listIterator = keySet.listIterator();
		String[] mapTemp;
		int i = 0;
        while (listIterator.hasNext()) {
            mapTemp = listIterator.next();
            if (mapTemp[0] == key) {
            	keySet.remove(i);
            	return;
            }
            i++;
        }
		return;
	}

	/**
	 * Get a lock for the set corresponding to a given key. The lock should be
	 * used by the caller of the get/put/del methods so that different sets can
	 * be #{modified|changed} in parallel.
	 *
	 * @param key
	 *            key to determine the lock to return
	 * @return lock for the set that contains the key
	 */

	public Lock getLock(String key) {
		int setNum = key.hashCode() % this.numSets;
		return this.locks[setNum];
	}

	/**
	 * Get the size of a given set in the cache.
	 * 
	 * @param cacheSet
	 *            Which set.
	 * @return Size of the cache set.
	 */
	int getCacheSetSize(int cacheSet) {
		// implement me
		return this.sets[cacheSet].size();
	}

	private void marshalTo(OutputStream os) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(KVCacheType.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
		marshaller.marshal(getXMLRoot(), os);
	}

	private JAXBElement<KVCacheType> getXMLRoot() throws JAXBException {
		ObjectFactory factory = new ObjectFactory();
		KVCacheType xmlCache = factory.createKVCacheType();
		// implement me
		LinkedList<String[]> keySet;
		KVCacheEntry entry;
		KVSetType setType;
		for (int i = 0; i < this.maxElemsPerSet; i++) {
			setType = factory.createKVSetType();
			setType.setId(String.valueOf(i));
			keySet = this.sets[i];
			ListIterator<String[]> listIterator = keySet.listIterator();
			String[] mapTemp;
	        while (listIterator.hasNext()) {
	            mapTemp = listIterator.next();
	            entry = factory.createKVCacheEntry();
	            entry.setKey(mapTemp[0]);
	            entry.setValue(mapTemp[1]);
	            entry.setIsReferenced(mapTemp[2]);
	            setType.getCacheEntry().add(entry);
	        }
	        xmlCache.getSet().add(setType);
		}
		return factory.createKVCache(xmlCache);
	}

	/**
	 * Serialize this store to XML. See spec for details on output format.
	 */
	public String toXML() {
		// implement me
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			marshalTo(os);
		} catch (JAXBException e) {
			 e.printStackTrace();
		}
		return os.toString();
	}

	@Override
	public String toString() {
		return this.toXML();
	}

}
