package com.rolfje.anonimatron.anonymizer;

import com.rolfje.anonimatron.synonyms.HashedFromSynonym;
import com.rolfje.anonimatron.synonyms.Synonym;
import com.rolfje.anonimatron.synonyms.SynonymMapper;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.XMLException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SynonymCache {

	private Map<String, Map<Object, Synonym>> synonymCache = new HashMap<>();
	private Hasher hasher;

	public SynonymCache() {
	}

	/**
	 * Reads the synonyms from the specified file and (re-)initializes the
	 * {@link #synonymCache} with it.
	 *
	 * @param synonymXMLfile the xml file containing the synonyms, as written by
	 *                       {@link #toFile(File)}
	 *
	 * @throws MappingException When synonyms can not be read from file.
	 * @throws IOException When synonyms can not be read from file.
	 * @throws XMLException When synonyms can not be read from file.
	 *
	 * @return Synonyms as the were stored on last run.
	 */
	public static SynonymCache fromFile(File synonymXMLfile) throws MappingException, IOException, XMLException {

		SynonymCache synonymCache = new SynonymCache();
		List<Synonym> synonymsFromFile = SynonymMapper
				.readFromFile(synonymXMLfile.getAbsolutePath());

		for (Synonym synonym : synonymsFromFile) {
			synonymCache.put(synonym);
		}

		return synonymCache;
	}

	/**
	 * Writes all known {@link Synonym}s in the cache out to the specified file
	 * in XML format.
	 *
	 * @param synonymXMLfile an empty writeable xml file to write the synonyms
	 *                       to.
	 * @throws XMLException When there is a problem writing the synonyms to file.
     * @throws IOException When there is a problem writing the synonyms to file.
     * @throws MappingException When there is a problem writing the synonyms to file.
	 */
	public void toFile(File synonymXMLfile) throws XMLException, IOException, MappingException {
		List<Synonym> allSynonyms = new ArrayList<Synonym>();

		// Flatten the type -> From -> Synonym map.
		Collection<Map<Object, Synonym>> allObjectMaps = synonymCache.values();
		for (Map<Object, Synonym> typeMap : allObjectMaps) {
			allSynonyms.addAll(typeMap.values());
		}

		SynonymMapper
				.writeToFile(allSynonyms, synonymXMLfile.getAbsolutePath());
	}

	public void put(Synonym synonym) {
		Map<Object, Synonym> map = synonymCache.get(synonym.getType());
		if (map == null) {
			map = new HashMap<Object, Synonym>();
			synonymCache.put(synonym.getType(), map);
		}

		if (hasher != null) {
			// Hash sensitive data before storing
			Synonym hashed = new HashedFromSynonym(hasher, synonym);
			map.put(hashed.getFrom(), hashed);
		} else {
			// Store as-is
			map.put(synonym.getFrom(), synonym);
		}

	}

	public Synonym get(String type, Object from) {
		Map<Object, Synonym> typemap = synonymCache.get(type);
		if (typemap == null) {
			return null;
		}

		if (hasher != null) {
			return typemap.get(hasher.base64Hash(from));
		} else {
			return typemap.get(from);
		}
	}

	public void setHasher(Hasher hasher) {
		this.hasher = hasher;
	}
}
