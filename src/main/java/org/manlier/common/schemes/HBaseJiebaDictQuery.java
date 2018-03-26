package org.manlier.common.schemes;

import org.apache.hadoop.hbase.util.Bytes;

public class HBaseJiebaDictQuery {
    public static String TABLE_NAME = "jieba_dict";
    public static byte[] INFO_COLUMNFAMILY = Bytes.toBytes("info");
    public static byte[] WEIGHT_QUALIFIER = Bytes.toBytes("weight");
    public static byte[] TAG_QUALIFIER = Bytes.toBytes("tag");
}
