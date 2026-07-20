package com.github.postyizhan.questpointer.client;

/** Small helper to turn a dimension id into a display name for chat/GUI/overlay text. */
public class DimensionNameUtil {

    private DimensionNameUtil() {}

    public static String getName(int dimension) {
        switch (dimension) {
            case 0:
                return "主世界";
            case -1:
                return "下界";
            case 1:
                return "末地";
            default:
                return "维度 " + dimension;
        }
    }
}
