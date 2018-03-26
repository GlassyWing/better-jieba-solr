package org.manlier.common.schemes;

import org.apache.hadoop.hbase.util.Bytes;

public class HBaseSynonymQuery {

	public static String TABLE_NAME = "thesaurus";
	public static byte[] SYNONYMS_COLUMNFAMILY = Bytes.toBytes("synonyms");
}
