package cn.pug.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 byte[] 转换为指定的实体类对象
     *
     * @param data Byte数组数据
     * @param clazz 目标实体类 Class 对象
     * @return 反序列化后的对象
     */
    public static <T> T fromBytes(byte[] data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize byte[] to object", e);
        }
    }

    /**
     * 将实体类对象转换为 byte[]
     *
     * @param obj 实体类对象
     * @return 序列化后的字节数组
     */
    public static byte[] toBytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to byte[]", e);
        }
    }
}
